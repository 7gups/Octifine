package com.voidstrap.android;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls to the Octane API (octane.wtf apisite and related hosts). Account calls take the session value, which
 * is sent only as a Cookie header on that request and is never logged. Authenticated calls do not follow
 * redirects, so the cookie cannot be sent to another host. String arguments are checked against a strict
 * character set before they are placed in a URL.
 */
final class OctaneApi {
    static final String BASE = "https://octane.wtf/apisite/";
    static final String COOKIE = ".ROBLOSECURITY";
    private static final int TIMEOUT_MS = 15_000;
    private static final int LIMIT = 512_000;
    private static final String SAFE = "[A-Za-z0-9_,.:/=&-]*";

    private OctaneApi() {
    }

    /**
     * GET a path under the apisite. Like the site's own client, a 403 that carries an x-csrf-token header is
     * retried once with that token. Returns the body, or null on failure.
     */
    static String get(String path, String session) {
        String token = "";
        String csrf = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            Object[] res = Http.request("GET", BASE + path, headers(session, csrf, token, false), null, TIMEOUT_MS, LIMIT, session == null);
            if (res == null) return null;
            int code = ((int[]) res[0])[0];
            if (code == 200) return new String((byte[]) res[2], StandardCharsets.UTF_8);
            if (code != 403) return null;
            String next = responseHeader(res, "x-csrf-token");
            if (next == null || next.isEmpty() || next.equals(token)) return null;
            token = next;
            csrf = csrfCookie(res);
        }
        return null;
    }

    /**
     * POST a JSON body. The site uses a CSRF token on POST requests: a 403 carries the token in a header and
     * the matching rbxcsrf cookie in Set-Cookie, and the retry must send both. Returns the body, or null.
     */
    static String post(String path, String json, String session) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String token = "";
        String csrf = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            Object[] res = Http.request("POST", BASE + path, headers(session, csrf, token, true), body, TIMEOUT_MS, LIMIT, session == null);
            if (res == null) return null;
            int code = ((int[]) res[0])[0];
            if (code == 200) return new String((byte[]) res[2], StandardCharsets.UTF_8);
            if (code != 403) return null;
            String next = responseHeader(res, "x-csrf-token");
            if (next == null || next.isEmpty() || next.equals(token)) return null;
            token = next;
            csrf = csrfCookie(res);
        }
        return null;
    }

    /** Request headers. Session and CSRF cookies share one Cookie header, since a repeated header would replace the first. */
    private static String[] headers(String session, String csrf, String token, boolean json) {
        List<String> h = new ArrayList<>();
        if (json) {
            h.add("Content-Type");
            h.add("application/json");
        }
        h.add("Accept");
        h.add("application/json");
        h.add("User-Agent");
        h.add("Voidstrap Android");
        String cookie = session == null ? null : COOKIE + "=" + session;
        if (csrf != null) cookie = cookie == null ? csrf : cookie + "; " + csrf;
        if (cookie != null) {
            h.add("Cookie");
            h.add(cookie);
        }
        h.add("x-csrf-token");
        h.add(token);
        return h.toArray(new String[0]);
    }

    /** The rbxcsrf cookie from a 403's Set-Cookie header, as name=value, or null. */
    private static String csrfCookie(Object[] res) {
        String[] pairs = (String[]) res[1];
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if ("set-cookie".equalsIgnoreCase(pairs[i]) && pairs[i + 1].trim().startsWith("rbxcsrf")) {
                return pairs[i + 1].trim().split(";", 2)[0];
            }
        }
        return null;
    }

static JSONObject postObject(String path, String json, String session) {
        String body = post(path, json, session);
        if (body == null) return null;
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            return null;
        }
    }

private static String responseHeader(Object[] res, String name) {
        String[] pairs = (String[]) res[1];
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (name.equalsIgnoreCase(pairs[i])) return pairs[i + 1];
        }
        return null;
    }

static JSONObject object(String path, String session) {
        String body = get(path, session);
        if (body == null) return null;
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            return null;
        }
    }

static JSONArray array(String path, String session) {
        String body = get(path, session);
        if (body == null) return null;
        try {
            return new JSONArray(body);
        } catch (JSONException e) {
            return null;
        }
    }

private static boolean safe(String v) {
        return v != null && v.matches(SAFE);
    }

static JSONObject alertInfo() {
        return object("api/alerts/alert-info", null);
    }

static JSONObject userInfo(long userId) {
        return object("users/v1/users/" + userId, null);
    }

static JSONObject headshots(String userIds, String size) {
        return safe(userIds) && safe(size) ? object("thumbnails/v1/users/avatar-headshot?userIds=" + userIds + "&size=" + size + "&format=png", null) : null;
    }

static JSONArray gamesByUniverse(String universeIds) {
        JSONObject o = safe(universeIds) ? object("games/v1/games?universeIds=" + universeIds, null) : null;
        return o == null ? null : o.optJSONArray("data");
    }

static JSONArray placeDetails(String placeIds) {
        return safe(placeIds) ? array("games/v1/games/multiget-place-details?placeIds=" + placeIds, null) : null;
    }

static JSONObject assetThumbs(String assetIds, String size) {
        return safe(assetIds) && safe(size) ? object("thumbnails/v1/assets?assetIds=" + assetIds + "&format=png&size=" + size, null) : null;
    }

    /**
     * A user's collectibles (limiteds). Response: data[] with assetId, userAssetId, name, recentAveragePrice,
     * serialNumber, copyCount, plus nextPageCursor and previousPageCursor. assetType "null" means all categories.
     */
    static JSONObject collectibles(long userId, String cursor, int limit, String assetType, String session) {
        String enc;
        try {
            enc = java.net.URLEncoder.encode(cursor == null ? "" : cursor, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        String type = assetType == null ? "null" : assetType;
        if (!safe(type)) return null;
        return object("inventory/v1/users/" + userId + "/assets/collectibles?cursor=" + enc + "&limit=" + limit + "&assetType=" + type, session);
    }

    /** Username to user ID, or 0 when the username is not found. */
    static long userIdForUsername(String username) {
        if (username == null || username.trim().isEmpty()) return 0;
        try {
            JSONObject req = new JSONObject().put("usernames", new JSONArray().put(username.trim()));
            JSONObject o = postObject("users/v1/usernames/users", req.toString(), null);
            JSONArray data = o == null ? null : o.optJSONArray("data");
            JSONObject first = data == null ? null : data.optJSONObject(0);
            return first == null ? 0 : first.optLong("id", 0);
        } catch (JSONException e) {
            return 0;
        }
    }

    /** Presence for a list of user ids. Response: userPresences[] with userPresenceType, placeId, userId, lastOnline. */
    static JSONObject presence(long[] userIds) {
        if (userIds == null || userIds.length == 0 || userIds.length > 100) return null;
        StringBuilder sb = new StringBuilder("{\"userIds\":[");
        for (int i = 0; i < userIds.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(userIds[i]);
        }
        sb.append("]}");
        return postObject("presence/v1/presence/users", sb.toString(), null);
    }

    /** Only octane.wtf links may be opened from API content. */
    static boolean safeLink(String url) {
        Uri u = Uri.parse(url == null ? "" : url);
        String host = u.getHost();
        return "https".equals(u.getScheme()) && host != null && (host.equals("octane.wtf") || host.endsWith(".octane.wtf"));
    }
}
