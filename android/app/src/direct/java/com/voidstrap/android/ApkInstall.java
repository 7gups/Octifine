package com.voidstrap.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

/** Opens the system package installer for a downloaded APK. Direct build only; Play builds don't have install permission. */
final class ApkInstall {
    private ApkInstall() {
    }

    static boolean available() {
        return true;
    }

    /** Checks the APK is the expected package, then opens the installer. Returns an error string, or null. */
    static String open(Context c, File apk, String expectedPackage) {
        PackageInfo info = c.getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
        if (info == null || !expectedPackage.equals(info.packageName)) {
            apk.delete();
            return "wrong package";
        }
        Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".apkfiles", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
        return null;
    }
}
