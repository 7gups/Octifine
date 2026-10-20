package com.voidstrap.android;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class LibraryData {
    private static final long MINUTE = 60_000L;
    static final String JOINS = "/data/local/tmp/voidstrap_joins.txt";

    static final class Game {
        boolean live;
        long placeId;
        long universeId;
        String name = "";
        String iconUrl;
        String thumbUrl;
        String creator = "";
        String description = "";
        String genre = "";
        boolean pinned;
        long lastPlayed;
        int plays;
        long playing;
        long visits;
        int maxPlayers;
        long created;
        long updated;
        int likePercent = -1;

        String title() {
            return name == null || name.isEmpty() ? "Place " + placeId : name;
        }

        JSONObject json() {
            return Core.args("placeId", placeId, "universeId", universeId, "name", name, "iconUrl", iconUrl, "thumbUrl", thumbUrl, "creator", creator,
                    "description", description, "genre", genre, "playing", playing, "visits", visits, "maxPlayers", maxPlayers, "created", created,
                    "updated", updated, "likePercent", likePercent);
        }

        void apply(JSONObject o) {
            universeId = o.optLong("universeId");
            name = o.optString("name");
            iconUrl = Core.opt(o, "iconUrl");
            thumbUrl = Core.opt(o, "thumbUrl");
            creator = o.optString("creator");
            description = o.optString("description");
            genre = o.optString("genre");
            playing = o.optLong("playing");
            visits = o.optLong("visits");
            maxPlayers = o.optInt("maxPlayers");
            created = o.optLong("created");
            updated = o.optLong("updated");
            likePercent = o.optInt("likePercent", -1);
        }
    }

    static final class Event {
        String id;
        String title;
        String subtitle;
        long start;
        long mediaId;
        String thumbUrl;
    }

    static final class Pass {
        long id;
        String name;
        String price;
        long iconAsset;
        String iconUrl;
    }

    private LibraryData() {
    }

    /** The game the signed-in account is in right now, or null. Set by refreshPlaying on a background thread. */
    private static volatile Game nowPlaying;

    /**
     * Asks presence for the signed-in account and stores the current game. Network call: run it off the main thread.
     * Returns true when the current game changed, so the caller can reload its list.
     */
    static boolean refreshPlaying(Context c) {
        Game found = null;
        long user = PekoraSession.userId(c);
        if (user > 0) {
            org.json.JSONObject o = OctaneApi.presence(new long[] {user});
            org.json.JSONArray arr = o == null ? null : o.optJSONArray("userPresences");
            org.json.JSONObject p = arr == null || arr.length() == 0 ? null : arr.optJSONObject(0);
            long place = p == null ? 0 : p.optLong("placeId", 0);
            if (p != null && p.optInt("userPresenceType", 0) == 2 && place > 0) {
                found = new Game();
                found.placeId = place;
                found.universeId = p.optLong("universeId", 0);
                found.name = "";
                found.live = true;
                saveCurrent(c, found);
            }
        }
        long before = nowPlaying == null ? 0 : nowPlaying.placeId;
        long after = found == null ? 0 : found.placeId;
        nowPlaying = found;
        return before != after;
    }

    private static volatile long watchToken;

    /**
     * After a launch hands off to the client, check presence every 30 seconds for 10 minutes, so the game you
     * join gets saved while Octifine is still running. Android may pause the check if Octifine is backgrounded.
     */
    static void watchPresence(Context c) {
        Context app = c.getApplicationContext();
        long token = System.nanoTime();
        watchToken = token;
        new Thread(() -> {
            long until = android.os.SystemClock.elapsedRealtime() + 10 * 60_000L;
            while (android.os.SystemClock.elapsedRealtime() < until && watchToken == token) {
                try {
                    refreshPlaying(app);
                } catch (RuntimeException ignored) {
                }
                try {
                    Thread.sleep(30_000L);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "presence-watch").start();
    }


    /** Pins the current game so it stays in the library after you leave it. Name and icon come from the meta lookup. */
    private static void saveCurrent(Context c, Game g) {
        Store store = Store.get(c);
        if (store.game(g.placeId) != null) return;
        JSONObject m = null;
        try {
            Object v = Core.value("feed.meta", Core.feed(c, "place", g.placeId));
            if (v instanceof JSONObject) m = (JSONObject) v;
        } catch (IOException ignored) {
        }
        String name = m == null ? "" : Core.opt(m, "name");
        String icon = m == null ? "" : Core.opt(m, "iconUrl");
        long universe = m == null ? g.universeId : m.optLong("universeId", g.universeId);
        store.pin(new Store.Game(g.placeId, universe, name == null ? "" : Store.clip(name, 200), icon == null ? "" : icon, System.currentTimeMillis()));
    }

    /** One open server for a place. jobId is what a join uses. */
    static final class Server {
        String jobId = "";
        int playing;
        int max;
        int ping;
        double fps;
    }

    /**
     * Open servers for a place from the legacy server list. Returns null when the request fails, and an empty list
     * when there are no open servers. Network call: run it off the main thread.
     */
    static List<Server> servers(long placeId) {
        Object[] res = Http.request("GET", "https://octane.wtf/games/getgameinstancesjson?placeId=" + placeId + "&startIndex=0",
                new String[] {"Accept", "application/json", "User-Agent", "Voidstrap Android"}, null, 15_000, 400_000, true);
        if (res == null || ((int[]) res[0])[0] != 200) return null;
        List<Server> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONObject(new String((byte[]) res[2], StandardCharsets.UTF_8)).optJSONArray("Collection");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Server s = new Server();
                s.jobId = o.optString("jobId", "");
                s.max = o.optInt("Capacity", 0);
                s.ping = o.optInt("Ping", 0);
                s.fps = o.optDouble("Fps", 0);
                // "PlayersCapacity" reads like "18 of 100"
                String pc = o.optString("PlayersCapacity", "").trim();
                String first = pc.isEmpty() ? "0" : pc.split("\\s+")[0];
                try {
                    s.playing = Integer.parseInt(first);
                } catch (NumberFormatException e) {
                    s.playing = 0;
                }
                if (!s.jobId.isEmpty() && (s.max <= 0 || s.playing < s.max)) out.add(s);
            }
        } catch (Exception e) {
            return null;
        }
        return out;
    }

    static List<Game> base(Store store) {
        LinkedHashMap<Long, Game> games = new LinkedHashMap<>();
        if (nowPlaying != null) games.put(nowPlaying.placeId, nowPlaying);
        for (Store.Game p : store.library) {
            Game g = new Game();
            g.placeId = p.placeId;
            g.universeId = p.universeId;
            g.name = p.name == null ? "" : p.name;
            g.iconUrl = p.iconUrl == null || p.iconUrl.isEmpty() ? null : p.iconUrl;
            g.pinned = true;
            if (nowPlaying != null && nowPlaying.placeId == g.placeId) g.live = true;
            games.put(g.placeId, g);
        }
        for (Store.Launch l : store.history) {
            if (l.placeId <= 0 || !played(l)) continue;
            Game g = games.get(l.placeId);
            if (g == null) {
                g = new Game();
                g.placeId = l.placeId;
                g.name = l.name == null ? "" : l.name;
                games.put(g.placeId, g);
            }
            if (g.name.isEmpty() && l.name != null) g.name = l.name;
            g.lastPlayed = Math.max(g.lastPlayed, l.time);
            g.plays++;
        }
        return new ArrayList<>(games.values());
    }

    static boolean played(Store.Launch l) {
        return Launcher.Result.HANDED_OFF.name().equals(l.result) || "PLAYED".equals(l.result);
    }

    private static JSONArray json(List<Game> games) {
        JSONArray a = new JSONArray();
        for (Game g : games) a.put(g.json());
        return a;
    }

    static void enrich(Context c, List<Game> games) {
        try {
            List<JSONObject> out = Core.objects(Core.value("feed.enrich", Core.feed(c, "games", json(games))));
            for (int i = 0; i < games.size() && i < out.size(); i++) games.get(i).apply(out.get(i));
        } catch (IOException ignored) {
        }
    }

    static List<Event> events(Context c, List<Game> games) {
        List<Event> found = new ArrayList<>();
        try {
            for (JSONObject o : Core.objects(Core.value("feed.events", Core.feed(c, "games", json(games))))) {
                Event e = new Event();
                e.id = o.optString("id");
                e.title = o.optString("title");
                e.subtitle = o.optString("subtitle");
                e.start = o.optLong("start");
                e.mediaId = o.optLong("mediaId");
                e.thumbUrl = Core.opt(o, "thumbUrl");
                found.add(e);
            }
        } catch (IOException ignored) {
        }
        return found;
    }

    static List<Pass> passes(Context c, long universeId) {
        List<Pass> out = new ArrayList<>();
        try {
            for (JSONObject o : Core.objects(Core.value("feed.passes", Core.feed(c, "universe", universeId)))) {
                Pass p = new Pass();
                p.id = o.optLong("id");
                p.name = o.optString("name");
                long price = o.optLong("price");
                p.price = !o.optBoolean("forSale") ? c.getString(R.string.library_off_sale) : price > 0 ? "R$ " + String.format(java.util.Locale.getDefault(), "%,d", price) : c.getString(R.string.library_for_sale);
                p.iconAsset = o.optLong("iconAsset");
                p.iconUrl = Core.opt(o, "iconUrl");
                out.add(p);
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    static void importJoins(Context c) {
        FlagWriter.Mode mode = FlagWriter.mode(c);
        if (mode != FlagWriter.Mode.HELPER && mode != FlagWriter.Mode.ROOT) return;
        String script = "logcat -d -s Roblox:I | grep 'Joining game' > " + JOINS + ".new; mv -f " + JOINS + ".new " + JOINS + "; chmod 0644 " + JOINS + "; exit 0\n";
        if (FlagWriter.shell(c, script, 15) != FlagWriter.OK) return;
        Object v;
        try {
            v = Core.value("feed.joins", Core.args("path", JOINS));
        } catch (IOException e) {
            return;
        }
        Store store = Store.get(c);
        String pkg = Targets.selected(c);
        List<Store.Launch> parsed = new ArrayList<>();
        JSONArray a = v instanceof JSONArray ? (JSONArray) v : new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONArray j = a.optJSONArray(i);
            if (j != null) parsed.add(new Store.Launch(j.optLong(0), j.optLong(1), "", pkg, "PLAYED"));
        }
        if (parsed.isEmpty()) return;
        store.main.post(() -> {
            List<Store.Launch> fresh = new ArrayList<>();
            for (Store.Launch l : parsed) if (!known(store.history, l) && !known(fresh, l)) fresh.add(l);
            store.importLaunches(fresh);
        });
    }

    private static boolean known(List<Store.Launch> list, Store.Launch join) {
        for (Store.Launch l : list) if (l.placeId == join.placeId && Math.abs(l.time - join.time) < 2 * MINUTE) return true;
        return false;
    }
}
