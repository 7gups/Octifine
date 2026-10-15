package com.voidstrap.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.content.pm.PackageInfoCompat;

import java.util.ArrayList;
import java.util.List;

public final class Targets {
    public static final String PEKORA = "wtf.octane.client"; // Octane 2.463.417004 (2021 Roblox client)
    public static final String DOWNLOAD = "https://octane.wtf/setup/android/Octane.apk";
    public static final String[] ALL = {PEKORA};

    public static final class State {
        public String pkg;
        public boolean installed;
        public boolean enabled;
        public boolean launchable;
        public String versionName;
        public long versionCode;
        public Drawable icon;

        public boolean ready() {
            return installed && enabled && launchable;
        }
    }

    public static boolean known(String pkg) {
        for (String p : ALL) if (p.equals(pkg)) return true;
        return false;
    }

    public static int nameRes(String pkg) {
        return R.string.target_pekora;
    }

    private static final java.util.Set<String> NOT_IN_2021 = new java.util.HashSet<>(java.util.Arrays.asList(
            "FIntDebugForceMSAASamples"));

    public static boolean is2018(Context c) {
        return false;
    }

    /** False only when the selected app's engine does not contain this flag (checked against each libroblox.so). */
    public static boolean flagSupported(Context c, String flag) {
        return !NOT_IN_2021.contains(flag);
    }

    public static String downloadUrl(String pkg) {
        return DOWNLOAD;
    }

    public static State resolve(Context c, String pkg) {
        State s = new State();
        s.pkg = pkg;
        PackageManager pm = c.getPackageManager();
        try {
            PackageInfo info = Build.VERSION.SDK_INT >= 33
                    ? pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                    : pm.getPackageInfo(pkg, 0);
            ApplicationInfo ai = info.applicationInfo;
            s.installed = true;
            s.enabled = ai == null || ai.enabled;
            s.versionName = info.versionName;
            s.versionCode = PackageInfoCompat.getLongVersionCode(info);
            s.launchable = pm.getLaunchIntentForPackage(pkg) != null;
            if (ai != null) s.icon = ai.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException ignored) {
            s.installed = false;
        }
        return s;
    }

    public static String selected(Context c) {
        String p = Store.get(c).setting("target", PEKORA);
        return known(p) ? p : PEKORA;
    }

    public static List<String> offered(Context c) {
        List<String> out = new ArrayList<>();
        for (String p : ALL) out.add(p);
        return out;
    }
}
