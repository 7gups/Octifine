package com.voidstrap.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends a flag request to the Pekostrap flag bot, which opens the GitHub issue. The app holds no GitHub token;
 * the bot keeps it on the server. The request carries the flag, its value and reason, a group, and the setup
 * details that help reproduce it.
 */
final class FlagRequests {
    /** URL of the deployed flag bot, for example https://flag-bot.yourname.workers.dev. Empty until deployed. */
    static final String ENDPOINT = "";

    private FlagRequests() {
    }

    static boolean configured() {
        return ENDPOINT.startsWith("https://");
    }

    /** The setup details shown to the user and sent with the request. */
    static String summary(Context c) {
        return c.getString(R.string.flag_env_pekostrap, BuildConfig.VERSION_NAME) + "\n"
                + c.getString(R.string.flag_env_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT) + "\n"
                + c.getString(R.string.flag_env_device, Build.MANUFACTURER + " " + Build.MODEL) + "\n"
                + c.getString(R.string.flag_env_pekora, versionOf(c, Targets.PEKORA)) + "\n"
                + c.getString(R.string.flag_env_patch, c.getString(patchLabel(c)));
    }

    /** Returns the created issue URL. Throws IOException when the bot is unreachable or rejects the request. */
    static String send(Context c, String flag, String value, String reason, String group) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("flag", flag)
                    .put("value", value)
                    .put("reason", reason)
                    .put("group", group)
                    .put("pekostrapVersion", BuildConfig.VERSION_NAME)
                    .put("androidVersion", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
                    .put("device", Build.MANUFACTURER + " " + Build.MODEL)
                    .put("pekoraVersion", versionOf(c, Targets.PEKORA))
                    .put("patchMethod", c.getString(patchLabel(c)));
        } catch (JSONException e) {
            throw new IOException("bad request");
        }
        Object[] res = Http.request("POST", ENDPOINT,
                new String[]{"Content-Type", "application/json", "Accept", "application/json", "User-Agent", "Voidstrap Android"},
                body.toString().getBytes(StandardCharsets.UTF_8), 15_000, 16_000, false);
        if (res == null) throw new IOException("network");
        int code = ((int[]) res[0])[0];
        if (code != 201) throw new IOException("status " + code);
        try {
            return new JSONObject(new String((byte[]) res[2], StandardCharsets.UTF_8)).optString("url", "");
        } catch (JSONException e) {
            throw new IOException("bad response");
        }
    }

    private static String versionOf(Context c, String pkg) {
        try {
            String v = c.getPackageManager().getPackageInfo(pkg, 0).versionName;
            return v == null ? "unknown" : v;
        } catch (PackageManager.NameNotFoundException e) {
            return "not installed";
        }
    }

    private static int patchLabel(Context c) {
        switch (Store.get(c).setting("patchMethod", "")) {
            case "root": return R.string.patch_root;
            case "shizuku": return R.string.patch_shizuku;
            case "usb": return R.string.patch_usb;
            default: return R.string.patch_method_none;
        }
    }
}
