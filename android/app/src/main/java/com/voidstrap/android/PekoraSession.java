package com.voidstrap.android;

import android.app.Activity;
import android.content.Context;

import java.security.SecureRandom;

/**
 * Pekora account link. The user enters a username or user ID and puts a generated phrase in their public
 * Pekora bio. No password, cookie, or WebView is used.
 */
final class PekoraSession {
    static final String ID_SETTING = "pekoraUserId";
    static final String NAME_SETTING = "pekoraName";
    private static final String OLD_VAULT = "pekora_session";
    private static final String PENDING_ID = "pekoraPendingId";
    private static final String PENDING_PHRASE = "pekoraPendingPhrase";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PekoraSession() {
    }

    static long userId(Context c) {
        return parseId(Store.get(c).setting(ID_SETTING, ""));
    }

    static boolean signedIn(Context c) {
        return userId(c) > 0;
    }

    static String statusText(Context c) {
        if (!signedIn(c)) return c.getString(R.string.pekora_account_signed_out);
        return c.getString(R.string.pekora_account_signed_in, Store.get(c).setting(NAME_SETTING, ""));
    }

    static String newPhrase() {
        StringBuilder sb = new StringBuilder("octifine-");
        for (int i = 0; i < 10; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    static void savePending(Context c, long id, String phrase) {
        Store s = Store.get(c);
        s.putSetting(PENDING_ID, String.valueOf(id));
        s.putSetting(PENDING_PHRASE, phrase);
    }

    static long pendingId(Context c) {
        return parseId(Store.get(c).setting(PENDING_ID, ""));
    }

    static String pendingPhrase(Context c) {
        return Store.get(c).setting(PENDING_PHRASE, "");
    }

    /** Stores the linked account and clears any pending phrase. */
    static void complete(Context c, long id, String name) {
        Store s = Store.get(c);
        s.putSetting(ID_SETTING, String.valueOf(id));
        s.putSetting(NAME_SETTING, name);
        s.putSetting(PENDING_ID, "");
        s.putSetting(PENDING_PHRASE, "");
        s.changed();
    }

    static void toggle(Activity a) {
        if (!signedIn(a)) {
            PekoraLoginDialog.show(a);
            return;
        }
        Ui.alert(a)
                .setTitle(R.string.pekora_sign_out_title)
                .setMessage(R.string.pekora_sign_out_body)
                .setPositiveButton(R.string.pekora_sign_out, (d, w) -> {
                    signOut(a);
                    Notify.say(a, Notify.GENERAL, R.string.pekora_signed_out_toast);
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    /** Unlinks the account and removes any session blob left by an earlier build. */
    static void signOut(Context c) {
        SecureStore.clear(c, OLD_VAULT);
        Store s = Store.get(c);
        s.putSetting(ID_SETTING, "");
        s.putSetting(NAME_SETTING, "");
        s.putSetting(PENDING_ID, "");
        s.putSetting(PENDING_PHRASE, "");
        s.changed();
    }

    private static long parseId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
