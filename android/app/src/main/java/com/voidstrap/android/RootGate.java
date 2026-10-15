package com.voidstrap.android;

import android.content.Context;
import android.provider.Settings;

/**
 * Decides whether root-only features should be hidden. Shizuku or USB/wireless debugging gives shell
 * access, which cannot mount mods, so those features stay hidden unless real root is available.
 */
final class RootGate {
    private RootGate() {}

    static boolean hideRootFeatures(Context c) {
        if (FlagWriter.rootMode(c) != FlagWriter.Mode.NONE) return false;
        return adbEnabled(c) || Helper.running() || ShizukuHelper.state(c) == ShizukuHelper.State.READY;
    }

    static boolean adbEnabled(Context c) {
        try {
            return Settings.Global.getInt(c.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
