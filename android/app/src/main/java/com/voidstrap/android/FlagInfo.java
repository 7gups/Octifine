package com.voidstrap.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Popup for a single flag: a description built from its name, prefix and category, plus copy and edit actions.
 * The description comes from the flag name and the allowlist, since the 2021 build data has no prose.
 */
final class FlagInfo {
    private static final Pattern NAME = Pattern.compile("^(DF|SF|F)(Flag|Int|String|Log)(.+)$");

    private FlagInfo() {}

    static Dialog show(Activity a, String key, Object value, Runnable edit) {
        Dialog[] holder = new Dialog[1];
        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);

        MaterialButton copy = new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        copy.setText(R.string.flag_info_copy);
        copy.setIcon(a.getDrawable(R.drawable.ic_copy));
        copy.setOnClickListener(x -> {
            Ui.copy(a, key, key);
            Notify.say(a, Notify.COPY, a.getString(R.string.presets_copied, key));
        });
        content.addView(copy, fullWidth(a, 8));

        if (edit != null) {
            MaterialButton editButton = new MaterialButton(a);
            editButton.setText(R.string.flag_info_edit);
            editButton.setOnClickListener(x -> {
                holder[0].dismiss();
                edit.run();
            });
            content.addView(editButton, fullWidth(a, 0));
        }

        holder[0] = Ui.surface(a, key, describe(a, key, value), content, null);
        holder[0].show();
        return holder[0];
    }

    static String describe(Context c, String key, Object value) {
        String type = c.getString(R.string.flag_info_type_unknown);
        String loaded = c.getString(R.string.flag_info_load_unknown);
        String name = key;
        Matcher m = NAME.matcher(key);
        if (m.matches()) {
            name = m.group(3);
            switch (m.group(2)) {
                case "Flag": type = c.getString(R.string.flag_info_type_flag); break;
                case "Int": type = c.getString(R.string.flag_info_type_int); break;
                case "String": type = c.getString(R.string.flag_info_type_string); break;
                case "Log": type = c.getString(R.string.flag_info_type_log); break;
                default: break;
            }
            switch (m.group(1)) {
                case "F": loaded = c.getString(R.string.flag_info_load_fast); break;
                case "DF": loaded = c.getString(R.string.flag_info_load_dynamic); break;
                case "SF": loaded = c.getString(R.string.flag_info_load_sync); break;
                default: break;
            }
        }
        String category = FlagWriter.ALLOWLIST.get(key);
        return c.getString(R.string.flag_info_body,
                readable(name),
                type,
                loaded,
                category == null ? c.getString(R.string.flag_info_no_category) : category,
                value == null ? c.getString(R.string.flag_info_unset) : String.valueOf(value));
    }

    /** "GPULightCulling3" becomes "GPU Light Culling3". */
    static String readable(String name) {
        return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])", " ");
    }

    private static LinearLayout.LayoutParams fullWidth(Context c, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Ui.dp(c, bottomDp));
        return lp;
    }
}
