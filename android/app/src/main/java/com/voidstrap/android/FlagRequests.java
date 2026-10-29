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
    private FlagRequests() {
    }

    /** The flag-request issue form on GitHub, with the title filled in. The user pastes the details into the form. */
    static String issueUrl(String flag) {
        return "https://github.com/7gups/Octifine/issues/new?template=flag_request.yml&labels=flag-request&title="
                + android.net.Uri.encode("Flag request: " + flag);
    }

    /** The request details for the clipboard, in the same order as the issue form. */
    static String details(Context c, String flag, String value, String reason, String group) {
        return "Flag name: " + flag + "\n"
                + "Value: " + (value.isEmpty() ? "(none)" : value) + "\n"
                + "Group: " + group + "\n\n"
                + "Why it helps:\n" + reason + "\n\n"
                + "Setup:\n" + summary(c);
    }

    /** The setup details shown to the user and sent with the request. */
    static String summary(Context c) {
        return c.getString(R.string.flag_env_pekostrap, BuildConfig.VERSION_NAME) + "\n"
                + c.getString(R.string.flag_env_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT) + "\n"
                + c.getString(R.string.flag_env_device, Build.MANUFACTURER + " " + Build.MODEL) + "\n"
                + c.getString(R.string.flag_env_pekora, versionOf(c, Targets.PEKORA)) + "\n"
                + c.getString(R.string.flag_env_patch, c.getString(patchLabel(c)));
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
