package com.voidstrap.android;

import android.content.Context;

import java.io.File;

/** Play builds don't install APKs; the caller opens the download page instead. */
final class ApkInstall {
    private ApkInstall() {
    }

    static boolean available() {
        return false;
    }

    static String open(Context c, File apk, String expectedPackage) {
        return "unsupported";
    }
}
