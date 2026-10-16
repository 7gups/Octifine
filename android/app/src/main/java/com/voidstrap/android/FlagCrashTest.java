package com.voidstrap.android;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Finds flags that crash the client. Each step writes one batch of flags, launches the client and asks
 * whether it crashed. A crash keeps the first half of the batch, a clean run keeps the second half, until
 * one flag is left. That flag is retested alone, then the rest are checked again for more culprits.
 */
final class FlagCrashTest {
    private static final String FILE = "flag-crash-test.json";
    private static final Pattern BOOL_FLAG = Pattern.compile("^D?FFlag[A-Za-z0-9_]+$");
    private static final String CHECK = "check", SEARCH = "search", VERIFY = "verify", DONE = "done";

    private String phase = DONE;
    private List<String> pending = new ArrayList<>();
    private List<String> lo = new ArrayList<>();
    private List<String> test = new ArrayList<>();
    private List<String> found = new ArrayList<>();
    private List<String> inconclusive = new ArrayList<>();
    private boolean awaiting;

    private FlagCrashTest() {}

    /** Boolean flags from the allowlist, which are the ones this test can switch on and off. */
    static List<String> candidates() {
        List<String> out = new ArrayList<>();
        for (String name : FlagWriter.ALLOWLIST.keySet()) {
            if (BOOL_FLAG.matcher(name).matches()) out.add(name);
        }
        return out;
    }

    static void confirmStart(AppCompatActivity a, List<String> names) {
        if (!BuildConfig.DEBUG || names.isEmpty()) return;
        Ui.alert(a)
                .setTitle(R.string.crashtest_title)
                .setMessage(R.string.crashtest_body)
                .setPositiveButton(R.string.crashtest_start, (d, w) -> {
                    FlagCrashTest t = new FlagCrashTest();
                    t.pending = new ArrayList<>(names);
                    t.phase = CHECK;
                    t.advance();
                    t.save(a);
                    showNext(a, t);
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    /** Called by the launcher. Returns the test batch to send to the client, or null when no test is running. */
    static String beginStep(Context c) {
        if (!BuildConfig.DEBUG) return null;
        FlagCrashTest t = load(c);
        if (t == null || DONE.equals(t.phase)) return null;
        t.awaiting = true;
        t.save(c);
        try {
            JSONObject o = new JSONObject();
            for (String name : t.test) o.put(name, true);
            return o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Called when Octifine comes back to the foreground. Asks about the last step if one is waiting. */
    static void resumed(AppCompatActivity a) {
        if (!BuildConfig.DEBUG) return;
        FlagCrashTest t = load(a);
        if (t == null || !t.awaiting || a.isFinishing()) return;
        Ui.alert(a)
                .setTitle(R.string.crashtest_ask_title)
                .setMessage(a.getString(R.string.crashtest_ask_body, t.test.size()))
                .setPositiveButton(R.string.crashtest_crashed, (d, w) -> answer(a, true))
                .setNegativeButton(R.string.crashtest_not_crashed, (d, w) -> answer(a, false))
                .setNeutralButton(R.string.crashtest_stop, (d, w) -> delete(a))
                .setCancelable(false)
                .show();
    }

    private static void answer(AppCompatActivity a, boolean crashed) {
        FlagCrashTest t = load(a);
        if (t == null) return;
        t.awaiting = false;
        t.apply(crashed);
        t.advance();
        t.save(a);
        showNext(a, t);
    }

    private static void showNext(AppCompatActivity a, FlagCrashTest t) {
        if (a.isFinishing()) return;
        if (DONE.equals(t.phase)) {
            delete(a);
            Ui.alert(a)
                    .setTitle(R.string.crashtest_done_title)
                    .setMessage(a.getString(R.string.crashtest_done_body,
                            t.found.isEmpty() ? a.getString(R.string.crashtest_none) : joined(t.found),
                            t.inconclusive.isEmpty() ? a.getString(R.string.crashtest_none) : joined(t.inconclusive)))
                    .setPositiveButton(R.string.common_ok, null)
                    .show();
            return;
        }
        Ui.alert(a)
                .setTitle(R.string.crashtest_next_title)
                .setMessage(a.getString(R.string.crashtest_next_body, t.test.size()))
                .setPositiveButton(R.string.crashtest_launch, (d, w) ->
                        Actions.launch(a, null, a.getString(Targets.nameRes(Targets.selected(a)))))
                .setNeutralButton(R.string.crashtest_stop, (d, w) -> delete(a))
                .setCancelable(false)
                .show();
    }

    /** Moves the search after the answer. A crash on the single-flag check confirms a culprit. */
    private void apply(boolean crashed) {
        if (CHECK.equals(phase)) {
            if (!crashed) {
                phase = DONE;
            } else {
                lo = new ArrayList<>(pending);
                phase = SEARCH;
            }
        } else if (SEARCH.equals(phase)) {
            int mid = lo.size() / 2;
            lo = crashed ? new ArrayList<>(lo.subList(0, mid)) : new ArrayList<>(lo.subList(mid, lo.size()));
        } else if (VERIFY.equals(phase)) {
            String culprit = lo.get(0);
            pending.remove(culprit);
            if (crashed) found.add(culprit);
            else inconclusive.add(culprit);
            phase = CHECK;
        }
    }

    /** Works out the next batch from the current phase. */
    private void advance() {
        if (CHECK.equals(phase)) {
            if (pending.isEmpty()) phase = DONE;
            else test = new ArrayList<>(pending);
        }
        if (SEARCH.equals(phase)) {
            if (lo.size() == 1) {
                phase = VERIFY;
                test = new ArrayList<>(lo);
            } else {
                test = new ArrayList<>(lo.subList(0, lo.size() / 2));
            }
        }
        if (VERIFY.equals(phase)) test = new ArrayList<>(lo);
        if (DONE.equals(phase)) test = new ArrayList<>();
    }

    private static File file(Context c) {
        return new File(c.getFilesDir(), FILE);
    }

    private static FlagCrashTest load(Context c) {
        File f = file(c);
        if (!f.exists()) return null;
        try (InputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
            JSONObject o = new JSONObject(new String(buf.toByteArray(), StandardCharsets.UTF_8));
            FlagCrashTest t = new FlagCrashTest();
            t.phase = o.getString("phase");
            t.pending = list(o.getJSONArray("pending"));
            t.lo = list(o.getJSONArray("lo"));
            t.test = list(o.getJSONArray("test"));
            t.found = list(o.getJSONArray("found"));
            t.inconclusive = list(o.getJSONArray("inconclusive"));
            t.awaiting = o.getBoolean("awaiting");
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    private void save(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("phase", phase);
            o.put("pending", new JSONArray(pending));
            o.put("lo", new JSONArray(lo));
            o.put("test", new JSONArray(test));
            o.put("found", new JSONArray(found));
            o.put("inconclusive", new JSONArray(inconclusive));
            o.put("awaiting", awaiting);
            try (OutputStream out = new FileOutputStream(file(c))) {
                out.write(o.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    private static void delete(Context c) {
        File f = file(c);
        if (f.exists()) f.delete();
    }

    private static String joined(List<String> names) {
        StringBuilder b = new StringBuilder();
        for (String n : names) {
            if (b.length() > 0) b.append(", ");
            b.append(n);
        }
        return b.toString();
    }

    private static List<String> list(JSONArray a) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        return out;
    }
}
