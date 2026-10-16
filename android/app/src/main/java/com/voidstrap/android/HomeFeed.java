package com.voidstrap.android;

import android.content.Context;

import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class HomeFeed {
    public static final String NEWS_PAGE = "https://devforum.pekora.pro/c/updates/announcements/46";
    private static final int MAX_GAMES = 9;

    private HomeFeed() {
    }

    public static final class News {
        public long id;
        public String title;
        public String summary;
        public String url;
        public String tag;
        public String image;
        public long created;
    }

    public static final class Item {
        public long id;
        public String name;
        public String creator;
        public String type;
        public String image;
        public int price;
        public boolean limited;
        public boolean bundle;

        public String link() {
            return "https://octane.wtf/" + (bundle ? "bundles/" : "catalog/") + id;
        }

        public String appLink() {
            return bundle ? "roblox://navigation/item_details?itemType=Bundle&itemId=" + id : link();
        }
    }

    public static final class GameCard {
        public long placeId;
        public long universeId;
        public String name;
        public String creator;
        public String thumbnail;
        public String likes = "?";
        public String players = "?";
        public long launched;
    }

    public static List<News> allNews(Context c) throws IOException {
        return news(c, 0);
    }

    public static List<News> latestNews(Context c, int count) throws IOException {
        return news(c, count);
    }

    private static List<News> news(Context c, int latest) throws IOException {
        List<News> out = new ArrayList<>();
        for (JSONObject o : Core.objects(Core.value("feed.news", Core.feed(c, "latest", latest)))) {
            News n = new News();
            n.id = o.optLong("id");
            n.title = o.optString("title");
            n.summary = o.optString("summary");
            n.url = o.optString("url");
            n.tag = o.optString("tag");
            n.image = o.optString("image");
            n.created = o.optLong("created");
            out.add(n);
        }
        return out;
    }

    public static String thumbnail(String image, int width) {
        return text("feed.thumbnail", Core.args("image", image == null ? "" : image, "width", width), image == null ? "" : image);
    }

    public static String cardTitle(String title) {
        return text("feed.cardTitle", Core.args("title", title), title.trim());
    }

    private static String text(String op, JSONObject args, String fallback) {
        try {
            String v = Core.text(op, args);
            return v == null ? fallback : v;
        } catch (IOException e) {
            return fallback;
        }
    }

    private static final int CATALOG_LIMIT = 10;
    private static final Map<Integer, String> ASSET_TYPES = new HashMap<>();

    static {
        ASSET_TYPES.put(1, "Image");
        ASSET_TYPES.put(2, "T-Shirt");
        ASSET_TYPES.put(3, "Audio");
        ASSET_TYPES.put(4, "Mesh");
        ASSET_TYPES.put(8, "Hat");
        ASSET_TYPES.put(9, "Place");
        ASSET_TYPES.put(10, "Model");
        ASSET_TYPES.put(11, "Shirt");
        ASSET_TYPES.put(12, "Pants");
        ASSET_TYPES.put(13, "Decal");
        ASSET_TYPES.put(17, "Head");
        ASSET_TYPES.put(18, "Face");
        ASSET_TYPES.put(19, "Gear");
        ASSET_TYPES.put(21, "Badge");
        ASSET_TYPES.put(24, "Animation");
        ASSET_TYPES.put(27, "Torso");
        ASSET_TYPES.put(28, "Right Arm");
        ASSET_TYPES.put(29, "Left Arm");
        ASSET_TYPES.put(30, "Left Leg");
        ASSET_TYPES.put(31, "Right Leg");
        ASSET_TYPES.put(32, "Package");
        ASSET_TYPES.put(34, "Game Pass");
        ASSET_TYPES.put(38, "Plugin");
        ASSET_TYPES.put(40, "MeshPart");
        ASSET_TYPES.put(41, "Hair Accessory");
        ASSET_TYPES.put(42, "Face Accessory");
        ASSET_TYPES.put(43, "Neck Accessory");
        ASSET_TYPES.put(44, "Shoulder Accessory");
        ASSET_TYPES.put(45, "Front Accessory");
        ASSET_TYPES.put(46, "Back Accessory");
        ASSET_TYPES.put(47, "Waist Accessory");
        ASSET_TYPES.put(48, "Climb Animation");
        ASSET_TYPES.put(49, "Death Animation");
        ASSET_TYPES.put(50, "Fall Animation");
        ASSET_TYPES.put(51, "Idle Animation");
        ASSET_TYPES.put(52, "Jump Animation");
        ASSET_TYPES.put(53, "Run Animation");
        ASSET_TYPES.put(54, "Swim Animation");
        ASSET_TYPES.put(55, "Walk Animation");
        ASSET_TYPES.put(56, "Pose Animation");
        ASSET_TYPES.put(61, "Emote");
        ASSET_TYPES.put(62, "Video");
        ASSET_TYPES.put(64, "T-Shirt Accessory");
        ASSET_TYPES.put(65, "Shirt Accessory");
        ASSET_TYPES.put(66, "Pants Accessory");
        ASSET_TYPES.put(67, "Jacket Accessory");
        ASSET_TYPES.put(68, "Sweater Accessory");
        ASSET_TYPES.put(69, "Shorts Accessory");
        ASSET_TYPES.put(70, "Left Shoe Accessory");
        ASSET_TYPES.put(71, "Right Shoe Accessory");
        ASSET_TYPES.put(72, "Dress Skirt Accessory");
        ASSET_TYPES.put(76, "Eyebrow Accessory");
        ASSET_TYPES.put(77, "Eyelash Accessory");
        ASSET_TYPES.put(78, "Mood Animation");
        ASSET_TYPES.put(79, "Dynamic Head");
    }

    /**
     * Catalog cards. The search endpoint returns only ids, so this asks for the ids, then fetches details
     * (POST) and thumbnails for those ids. Bundles are skipped here.
     */
    public static List<Item> catalog(Context c) throws IOException {
        List<Item> out = new ArrayList<>();
        for (JSONObject o : Core.objects(Core.value("feed.catalog", Core.feed(c)))) {
            Item it = new Item();
            it.id = o.optLong("id");
            it.name = o.optString("name");
            it.creator = o.optString("creator");
            it.type = o.optString("type");
            it.image = o.optString("image");
            it.price = o.optInt("price");
            it.limited = o.optBoolean("limited");
            it.bundle = o.optBoolean("bundle");
            out.add(it);
        }
        return out;
    }

    private static boolean hasLimitedRestriction(JSONArray restrictions) {
        if (restrictions == null) return false;
        for (int i = 0; i < restrictions.length(); i++) {
            String r = restrictions.optString(i, "").toLowerCase(Locale.ROOT);
            if (r.equals("limited") || r.equals("limitedunique") || r.equals("collectible")) return true;
        }
        return false;
    }

    public static String price(Item it) {
        return it.price <= 0 ? null : "R$ " + NumberFormat.getIntegerInstance(Locale.getDefault()).format(it.price);
    }

    public static List<GameCard> base(List<Store.Launch> history) {
        LinkedHashMap<Long, GameCard> cards = new LinkedHashMap<>();
        for (Store.Launch l : history) {
            if (l.placeId <= 0 || cards.containsKey(l.placeId) || cards.size() >= MAX_GAMES) continue;
            if (!LibraryData.played(l)) continue;
            GameCard g = new GameCard();
            g.placeId = l.placeId;
            g.name = l.name == null || l.name.isEmpty() ? String.valueOf(l.placeId) : l.name;
            g.creator = "";
            g.launched = l.time;
            cards.put(l.placeId, g);
        }
        return new ArrayList<>(cards.values());
    }

    public static List<GameCard> enrich(Context c, List<GameCard> cards) {
        JSONArray in = new JSONArray();
        for (GameCard g : cards) in.put(Core.args("placeId", g.placeId, "universeId", g.universeId, "name", g.name, "creator", g.creator));
        try {
            List<JSONObject> out = Core.objects(Core.value("feed.cards", Core.feed(c, "cards", in)));
            for (int i = 0; i < cards.size() && i < out.size(); i++) {
                GameCard g = cards.get(i);
                JSONObject o = out.get(i);
                g.universeId = o.optLong("universeId");
                g.name = o.optString("name", g.name);
                g.creator = o.optString("creator", g.creator);
                g.players = o.optString("players", g.players);
                g.likes = o.optString("likes", g.likes);
                g.thumbnail = o.optString("thumbnail", g.thumbnail);
            }
        } catch (IOException ignored) {
        }
        return cards;
    }

    static String count(long n) {
        return text("feed.count", Core.args("n", n), String.valueOf(n));
    }
}
