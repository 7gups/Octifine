package com.voidstrap.android;

import android.content.Context;

/** Matchmaker removed in the direct build. Keeps the shared API so the common code compiles; every hook is inert. */
final class SmartJoin {
    interface Alert {
        void show(String text);
    }

    private SmartJoin() {
    }

    static boolean available() {
        return false;
    }

    static String title(Context c) {
        return "";
    }

    static String alertsTitle(Context c) {
        return null;
    }

    static String alertsBody(Context c) {
        return null;
    }

    static String openBody(Context c) {
        return "";
    }

    static Page page() {
        return null;
    }

    static String[] presence() {
        return null;
    }

    static boolean rejoining() {
        return false;
    }

    static Deeplink rewrite(Context app, Deeplink destination) {
        return destination;
    }

    static void onJoined(Context c, ActivityWatcher.Data data, Alert alert) {
    }
}
