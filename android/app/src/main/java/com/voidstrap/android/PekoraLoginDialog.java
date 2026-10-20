package com.voidstrap.android;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

/** Links a Pekora account by user ID. The generated phrase must appear in the public profile description. */
final class PekoraLoginDialog {
    private PekoraLoginDialog() {
    }

    static void show(Activity a) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_pekora_login, null);
        EditText idField = v.findViewById(R.id.login_user_id);
        TextView phraseView = v.findViewById(R.id.login_phrase);
        TextView status = v.findViewById(R.id.login_status);
        MaterialButton generate = v.findViewById(R.id.login_generate);
        MaterialButton verify = v.findViewById(R.id.login_verify);
        Store store = Store.get(a);

        AlertDialog dialog = Ui.alert(a)
                .setTitle(R.string.pekora_login_title)
                .setView(v)
                .setNegativeButton(R.string.common_cancel, null)
                .show();

        String pending = PekoraSession.pendingPhrase(a);
        if (!pending.isEmpty()) phraseView.setText(pending);

        generate.setOnClickListener(x -> {
            String input = idField.getText().toString().trim();
            generate.setEnabled(false);
            status.setText(R.string.pekora_login_checking);
            store.work.execute(() -> {
                long id = input.matches("\\d+") ? parseId(input) : OctaneApi.userIdForUsername(input);
                store.main.post(() -> {
                    generate.setEnabled(true);
                    if (id <= 0) {
                        status.setText(R.string.pekora_login_need_id);
                        return;
                    }
                    String phrase = PekoraSession.newPhrase();
                    PekoraSession.savePending(a, id, phrase);
                    phraseView.setText(phrase);
                    status.setText(R.string.pekora_login_hint);
                });
            });
        });

        verify.setOnClickListener(x -> {
            long id = PekoraSession.pendingId(a);
            String phrase = PekoraSession.pendingPhrase(a);
            if (id <= 0 || phrase.isEmpty()) {
                status.setText(R.string.pekora_login_need_phrase);
                return;
            }
            verify.setEnabled(false);
            status.setText(R.string.pekora_login_checking);
            store.work.execute(() -> {
                JSONObject info = OctaneApi.userInfo(id);
                store.main.post(() -> {
                    verify.setEnabled(true);
                    if (info == null) {
                        status.setText(R.string.pekora_login_unreachable);
                        return;
                    }
                    if (!info.optString("description", "").contains(phrase)) {
                        status.setText(R.string.pekora_login_not_found);
                        return;
                    }
                    PekoraSession.complete(a, id, info.optString("name", ""));
                    Notify.say(a, Notify.GENERAL, R.string.pekora_signed_in_toast);
                    dialog.dismiss();
                });
            });
        });
    }

    private static long parseId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
