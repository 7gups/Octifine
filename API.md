# Octifine and Octane API reference

This document covers two things:

1. **Octane endpoints** that Octifine calls, with what has been tested.
2. **Octifine internals**: the native bridge operations and the link forms the launcher uses.

Octane's API is not public or documented. Everything below was found by testing from a shell and from a browser capture. Endpoints can change without notice.

**Status key**
- **Verified:** tested in this project and returned the shape shown.
- **Auth:** needs a signed-in session or a CSRF token. Tested and confirmed.
- **Not found:** tested and returned 404.
- **Untested:** the path is used in code but no working response has been recorded yet.

Never commit cookie values. The names below are the only ones this document lists.

---

## Hosts

| Host | Used for |
|---|---|
| `octane.wtf/apisite/` | Users, games, thumbnails, presence, passes |
| `games.octane.wtf` | Game lists, place details, game sorts |
| `catalog.octane.wtf` | Catalog search and item details |
| `octane.wtf/games/` | Legacy server list |
| `octane.wtf/users/` | Favorites |
| `assetgame.octane.wtf` | Browser launch ticket endpoint (not used by Octifine) |
| `tcdn.octane.wtf` | Image files returned by thumbnail endpoints |

## Cookies

| Name | Meaning |
|---|---|
| `.ROBLOSECURITY` | Login session. Needed for signed-in lists such as `games/v1/games/list`. |
| `rbxcsrf4` | CSRF cookie. Set by a 403 response on POST requests. Must be sent with the token on the retry. |

## CSRF on POST

Every POST to Octane's API can return `403` with:
- a header `x-csrf-token: <token>`
- `Set-Cookie: rbxcsrf4=<value>`
- body `{"errors":[{"code":0,"message":"Token Validation Failed"}]}`

Retry the same request with both:
- `x-csrf-token: <token>`
- `Cookie: rbxcsrf4=<value>`

Sending only the token fails with the same message. Octifine's`feed.rs` and `catalog_details` do this retry.

---

## Users

### `GET octane.wtf/apisite/users/v1/users/{id}` — Verified

Anonymous. Example for id `7395`:

```json
{"id":7395,"name":"jonfield","displayName":"boi","description":null,
 "created":"2026-10-03T23:20:59.697604Z","isBanned":false,
 "hasVerifiedBadge":false,"isPremium":false,"hasAdminBadge":false}
```

### `POST octane.wtf/apisite/users/v1/usernames/users` — Auth (CSRF)

Body: `{"usernames":["jonfield"],"excludeBannedUsers":false}`

Returns `403` with a CSRF token. The success response has not been recorded yet, so the username-to-id lookup is **untested** after the retry fix.

### `GET octane.wtf/users/favorites/list-json?userId={id}&assetTypeId=9&itemsPerPage={n}` — Verified

Anonymous. `assetTypeId=9` is places. Returns:

```json
{"IsValid":true,"Data":{"TotalItems":0,"Start":0,"End":0,"Page":1,
 "ItemsPerPage":6,"PageType":"favorites","Items":[]}}
```

Item fields are not recorded yet, because the test account had no favorites.

---

## Presence

### `POST octane.wtf/apisite/presence/v1/presence/users` — Auth (CSRF)

Body: `{"userIds":[7395]}`

After the CSRF retry, returned:

```json
{"userPresences":[{"userPresenceType":2,"lastLocation":"Playing","placeId":7,
 "rootPlaceId":7,"gameId":"<guid>","universeId":6,"userId":7395,
 "lastOnline":"2026-10-17T01:11:39.4090841Z"}]}
```

`userPresenceType` 2 means in a game. Octifine uses `placeId` and `universeId` to save the game to the library.

---

## Games

### `GET games.octane.wtf/v1/games/multiget-place-details?placeIds={id}` — Verified

Anonymous. Returns an **array**, not an object:

```json
[{"placeId":7,"name":"Natural Disaster Survival [ v1.5 UPDATE ]",
  "builderId":2,"builderType":"User","builder":"lol","universeId":6,
  "universeRootPlaceId":7,"isPlayable":true,"imageToken":"T_7_icon",
  "maxPlayerCount":100,"genre":"All","moderationStatus":"ReviewApproved"}]
```

Octifine's `universe_from_details` reads the first element. The array was previously discarded, which broke universe lookup and adding by PlaceId.

### `GET octane.wtf/apisite/games/v1/games?universeIds={id}` — Verified

Anonymous. Returns `{"data":[{...}]}` with `id`, `rootPlaceId`, `name`, `playing`, `visits`, `maxPlayers`, `creator`, `favoritedCount`.

### `GET games.octane.wtf/v1/games/list?gameSetTargetId={n}&maxRows={n}&sortToken={token}` — Auth (`.ROBLOSECURITY`)

Anonymous returns `Unauthorized`. With the cookie returns:

```json
{"games":[{"universeId":6,"name":"...","placeId":7,"gameDescription":"...",
  "playerCount":27,"visitCount":66955,"creatorId":2,"creatorType":0,
  "creatorName":"lol","genre":"All","totalUpVotes":460,"totalDownVotes":116,
  "price":null,"isSponsored":false,"imageToken":"T_7_icon"}]}
```

The `gameSetTargetId=96` / `sortToken=recent` list returned 7 games even with `maxRows=100`. There is no cursor or total in the response. It is not the full catalog.

### `GET games.octane.wtf/v1/games/sorts?gameSortsContext=HomeSorts` — Auth (`.ROBLOSECURITY`)

Returns:

```json
{"sorts":[{"token":"recent","name":"Recent","displayName":"Recent",
  "gameSetTypeId":7,"gameSetTargetId":96,"numberOfGames":0,"isDefaultSort":true,
  "tokenExpiryInSeconds":86400},
  {"token":"popular","name":"popular","displayName":"Popular",
  "gameSetTypeId":1,"gameSetTargetId":90,"numberOfGames":0,"isDefaultSort":true,
  "tokenExpiryInSeconds":86400}]}
```

Only two sorts exist.

### `GET octane.wtf/apisite/games/v1/games/{universeId}/game-passes` — Verified

Anonymous. Returns:

```json
{"nextPageCursor":null,"previousPageCursor":null,"data":[
  {"id":508,"name":"Green Balloon","displayName":"Green Balloon","productId":508,
   "price":80,"isForSale":true,"isOwned":false,"sales":0,
   "created":"...","updated":"..."}]}
```

The pass `id` is also its icon asset id, so the thumbnail endpoint below works with it. No `displayIconImageAssetId` is returned.

### `GET games.octane.wtf/v1/games/{placeId}/servers/Public` — Not found

Returned `404` for every variant tried, including `Friend` and nonsense names.

### `GET octane.wtf/games/getgameinstancesjson?placeId={id}&startIndex={n}` — Verified

Legacy Roblox-style server list. Anonymous. Returns:

```json
{"PlaceId":7,"Collection":[{"jobId":"<guid>","Capacity":100,"Ping":100,
  "Fps":59.95,"PlayersCapacity":"18 of 100","CurrentPlayers":[{"Id":17222,
  "Username":"zqa"}],"JoinScript":null,"RobloxAppJoinScript":"","UserCanJoin":true}]}
```

Octifine's `smart.rs` `servers()` reads `Collection`, uses `jobId` as the id, and counts players from `PlayersCapacity`. The `startIndex` paging has not been tested beyond the first page.

### `GET octane.wtf/apisite/game-passes/v1/universes/{id}/game-passes` — Not found

Returned `404 Not found`.

---

## Catalog

### `GET catalog.octane.wtf/v1/search/items?category=Featured&limit={n}` — Verified

Anonymous. Returns IDs only:

```json
{"_total":866,"nextPageCursor":"5","previousPageCursor":null,
 "data":[{"itemType":"Asset","id":196627}]}
```

### `POST catalog.octane.wtf/v1/catalog/items/details` — Auth (CSRF)

Body: `{"items":[{"itemType":"Asset","id":196627}]}`

Returns `403` with a CSRF token. The retry with token and `rbxcsrf4` is implemented in `feed.rs` `catalog_details`. The success response has not been recorded yet, so the catalog section is **untested** after the fix.

---

## Thumbnails

### `GET octane.wtf/apisite/thumbnails/v1/assets?assetIds={ids}&size={WxH}&format=Png&isCircular=false` — Verified

Anonymous. Works for assets and game-pass ids. Returns:

```json
{"data":[{"targetId":508,"state":"Completed",
  "imageUrl":"https://tcdn.octane.wtf/<hash>.png","version":""}]}
```

### `GET octane.wtf/apisite/thumbnails/v1/game-passes?passIds={ids}` — Not found

Returned nothing. `thumbnails.octane.wtf/v1/game-passes` returned `404`.

---

## Other hosts seen in the browser

| Host | Result |
|---|---|
| `metrics.octane.wtf/v1/performance/send-measurement` | `403` |
| `contacts.octane.wtf/v1/user/get-tags` | `403` |
| `presence.octane.wtf/v1/presence/users` | `403` (browser call, not tested from Octifine) |

---

## Launching a game

### Android: `wtf.octane.client`

Octane's Android manifest registers the `roblox` and `robloxmobile` schemes. Tested on device with `am start`:

| Link | Result |
|---|---|
| `roblox://placeId=<id>` | Opens the game |
| `robloxmobile://placeID=<id>` | Opens the game |
| `roblox://experiences/start?placeId=<id>` | Opens Octane's home screen only |

Octifine tries the first two in that order. Adding `&gameInstanceId=<id>` or `&linkCode=<code>` keeps a specific server.

The Octane APK does **not** register `octane-player`. That scheme is used by the web Play button on desktop.

### Web / desktop: `octane-player:1`

Seen in a browser capture. The browser opens:

```
octane-player:1+launchmode:play+gameinfo:<JWT>+placelauncherurl:https://octane.wtf/Game/PlaceLauncher.ashx?request=RequestGame&placeId=<id>
```

The `gameinfo` JWT is a join ticket (type `GameJoinTicketV1.1`, with `userId`, `placeId`, `iat`). It is issued by the `RequestGame` call. Octifine does not use this path.

---

## Octifine internals

### Native bridge operations

Octifine calls the Rust core through `Core.value(op, args)` and `Core.feed(c, key, value, …)`. Operations as of this version:

| Operation | Purpose |
|---|---|
| `feed.news` | News entries (not shown on Octifine's home) |
| `feed.catalog` | Catalog cards (IDs from search, details from POST) |
| `feed.cards` | Home cards |
| `feed.meta` (`place`) | Game name, icon and universe for a place |
| `feed.passes` (`universe`) | Game passes with icon ids |
| `feed.events` (`games`) | Event data for a game list |
| `feed.joins` | Join history |
| `flags.validKey`, `flags.cleanKey`, `flags.intFlag`, `flags.parseTyped` | Flag name handling |
| `shell.ping`, `shell.run`, `shell.su`, `shell.rootProbe`, `shell.hasSu` | Privileged shell access |
| `shell.writeScript`, `shell.removeScript`, `shell.launchScript` | Write the client's flags file |
| `shell.safeForHeredoc` | Checks the JSON before it goes into a heredoc |
| `smart.pick`, `smart.onJoined`, `smart.seen` | Server picking |
| `presence.rpc`, `presence.account`, `presence.shown` | Discord presence |

These are internal. They can change between releases.

### Flags file

The client reads flags from:

```
/data/data/wtf.octane.client/files/exe/ClientSettings/ClientAppSettings.json
```

Octifine writes it through a helper or root shell. The Mods tab copies workspace files into the client's OBB, and the crash test writes batches of flags to this path.

### Launch decision

`Launcher.launch` writes the active flags, then calls `start()`. For a place, it tries each link form in order and returns `HANDED_OFF` on the first one the system accepts. It returns `DESTINATION_UNSUPPORTED` if none are accepted. Events are written to the Mod activity log.

---

## GitHub

These are GitHub's public API routes. They have a rate limit for unauthenticated requests, so the app caches them.

### `GET api.github.com/repos/7gups/Octifine` — Verified

Returns the repository, including `stargazers_count`. Octifine's home screen reads the star count from here for the snake card and the GitHub card. Returns `200` anonymously.

### `GET api.github.com/repos/7gups/Octifine/stats/commit_activity` — Verified (202)

Returns `202` with `[]`-sized body while GitHub computes the stats. It is not ready on a new or busy repo. Octifine falls back to the commit list when it gets `202`.

### `GET api.github.com/repos/7gups/Octifine/commits?per_page=100&page={n}` — Untested

Used by the commit-graph fallback, up to three pages. Each commit's date is read from `commit.author.date`.

### `GET api.github.com/repos/7gups/Octifine/releases/latest` — Untested

Used for the release card. The code treats `404` as "no release yet".

### `GET api.github.com/repos/7gups/Octifine/releases?per_page=20` — Untested

Used by the in-app updater. It picks the asset whose name contains `android` and the build flavour.

### Other repositories the app reads

| Repository | Used for | Status |
|---|---|---|
| `KloBraticc/SkyboxPackV2` (contents, commits/main) | Sky presets in Mods | Untested |
| `klikos-modloader/marketplace` (git trees) | Catalog of mod files | Untested |

### Star chart

Octifine generates its own star-history chart. The `star-chart` workflow runs daily and on changes to `tools/star-chart/`:

1. `tools/star-chart/star_chart.py` lists every stargazer with `Accept: application/vnd.github.star+json` (paged, 100 per page) and counts stars per day.
2. It writes `star-chart-light.svg` and `star-chart-dark.svg`.
3. The workflow publishes them to the `output` branch.

The app loads them from:

- `https://raw.githubusercontent.com/7gups/Octifine/output/star-chart-light.svg`
- `https://raw.githubusercontent.com/7gups/Octifine/output/star-chart-dark.svg`

Voidstrap's chart at `KloBraticc/Voidstrap` is a separate project and is not used.

---

## Not yet verified

- Username to id lookup (`usernames/users`) after the CSRF retry.
- Catalog details (`catalog.octane.wtf/v1/catalog/items/details`) after the CSRF retry.
- Items inside `games/v1/games/list` and `favorites/list-json`.
- Paging for the legacy server list.
- Any endpoint that lists all of Octane's games. None has been found.
- The GitHub routes marked Untested above, and the two third-party repositories.
