package com.voidstrap.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_TAB = "tab";
    public static final String EXTRA_PROBLEM = "problem";
    private static final String TESTING_NOTICE = "androidTestingNoticeSeen";

    private boolean testingNoticeOpen;

    private static final int[] TABS = {R.id.nav_home, R.id.nav_mods, R.id.nav_flags, R.id.nav_flag_editor, R.id.nav_integrations, R.id.nav_smart, R.id.nav_settings};

    private Store store;
    private int current = R.id.nav_home;
    private NavigationBarView nav;
    private LinearLayout sidebarItems;
    private static long lastJoinImport;
    private View barSave;
    private View barLaunch;
    private long savedAt;
    private final Runnable storeListener = this::onStoreChanged;
    private boolean saving;
    private String styled;
    private final androidx.activity.result.ActivityResultLauncher<Intent> launchPage = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), r -> onLaunchPage(r.getResultCode()));
    private final Runnable presenceRefresh = AppPresence::update;

    @Override
    protected void onCreate(Bundle saved) {
        store = Store.get(this);
        styled = VoidstrapApp.styleKey(store);
        VoidstrapApp.style(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        ThemeFade.play(this);
        VoidstrapApp.bars(this);
        if (saved != null) current = saved.getInt(EXTRA_TAB, R.id.nav_home);
        nav = findViewById(R.id.nav);
        sidebarItems = findViewById(R.id.sidebar_items);
        if (nav != null) {
            floatNav();
            nav.setOnItemSelectedListener(item -> {
                show(item.getItemId());
                return true;
            });
            nav.setOnItemReselectedListener(item -> {
                if (current != item.getItemId() && parent(current) == item.getItemId()) show(item.getItemId());
            });
        }
        if (sidebarItems != null) buildSidebar();
        setupBar();
        applyInsets();
        AppBackground.apply(this);
        getOnBackPressedDispatcher().addCallback(this, editorBack);
        select(current);
        if (saved == null && (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) handle(getIntent());
        if (saved == null && LaunchMenuActivity.due(getIntent())) {
            if (OnboardingActivity.due(this)) startActivity(new Intent(this, OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            else launchPage.launch(new Intent(this, LaunchMenuActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle saved) {
        super.onRestoreInstanceState(saved);
        if (nav == null) return;
        MenuItem item = nav.getMenu().findItem(current);
        if (item == null) item = nav.getMenu().findItem(parent(current));
        if (item != null) item.setChecked(true);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        Flyout.track(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent ev) {
        Flyout.track(ev);
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    private boolean isString(int id) {
        try {
            return "string".equals(getResources().getResourceTypeName(id));
        } catch (android.content.res.Resources.NotFoundException e) {
            return false;
        }
    }

    private void handle(Intent intent) {
        if (intent == null) return;
        int tab = Crash.call("intent tab", () -> intent.getIntExtra(EXTRA_TAB, 0), 0);
        if (tab != 0 && java.util.Arrays.stream(TABS).anyMatch(t -> t == tab)) {
            select(tab);
            intent.removeExtra(EXTRA_TAB);
        }
        int problem = Crash.call("intent problem", () -> intent.getIntExtra(EXTRA_PROBLEM, 0), 0);
        if (problem != 0 && isString(problem)) {
            intent.removeExtra(EXTRA_PROBLEM);
            View content = findViewById(R.id.content);
            if (content != null) content.post(() -> Ui.say(this, problem));
        }
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.unobserve(storeListener);
        if (isFinishing()) AppPresence.hidden();
        super.onDestroy();
    }

    private void onLaunchPage(int code) {
        if (code == LaunchMenuActivity.LAUNCH) {
            Actions.launch(this, null, getString(Targets.nameRes(Targets.selected(this))));
        } else if (code == LaunchMenuActivity.SETTINGS) {
            select(R.id.nav_settings);
        } else if (code == LaunchMenuActivity.ABOUT) {
            SettingsFragment.pendingSection = SettingsFragment.SECTION_ABOUT;
            select(R.id.nav_settings);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!styled.equals(VoidstrapApp.styleKey(store))) recreate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        store.observe(presenceRefresh);
        AppPresence.shown(this, current);
    }

    @Override
    protected void onStop() {
        store.unobserve(presenceRefresh);
        super.onStop();
        if (!isChangingConfigurations() && !isFinishing()) release();
    }

    private void setupBar() {
        barSave = findViewById(R.id.bar_save);
        barLaunch = findViewById(R.id.bar_launch);
        if (barSave != null) barSave.setOnClickListener(v -> save(false));
        if (barLaunch != null) barLaunch.setOnClickListener(v -> Actions.launch(this, null, getString(Targets.nameRes(Targets.selected(this)))));
        store.observe(storeListener);
    }

    /** Edits made after a save hide the launch button again. Saving itself also fires this, so ignore it briefly. */
    private void onStoreChanged() {
        if (saving || barLaunch == null || barLaunch.getVisibility() != View.VISIBLE) return;
        if (SystemClock.uptimeMillis() - savedAt < 1500) return;
        hideLaunch();
    }

    private void showLaunch() {
        if (barLaunch == null) return;
        savedAt = SystemClock.uptimeMillis();
        barLaunch.animate().cancel();
        barLaunch.setScaleX(0f);
        barLaunch.setScaleY(0f);
        barLaunch.setVisibility(View.VISIBLE);
        barLaunch.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    private void hideLaunch() {
        if (barLaunch == null) return;
        barLaunch.animate().cancel();
        barLaunch.animate().scaleX(0f).scaleY(0f).setDuration(150)
                .withEndAction(() -> barLaunch.setVisibility(View.GONE)).start();
    }

    private void save(boolean launch) {
        if (saving) return;
        saving = true;
        store.saveAll(ok -> {
            saving = false;
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                Ui.alert(this)
                        .setTitle(R.string.save_failed_title)
                        .setMessage(R.string.save_failed_body)
                        .setPositiveButton(R.string.common_ok, null)
                        .show();
                return;
            }
            if (launch) Actions.launch(this, null, getString(Targets.nameRes(Targets.selected(this))));
            else {
                showLaunch();
                applySavedMods();
            }
        });
    }

    private void applySavedMods() {
        Context app = getApplicationContext();
        if (!ModEngine.enabled(app) || !ModEngine.hasMods(app)) {
            Notify.say(this, Notify.GENERAL, R.string.bar_saved);
            return;
        }
        if (FlagWriter.rootMode(app) == FlagWriter.Mode.NONE) {
            Ui.say(this, R.string.bar_saved_no_root);
            return;
        }
        String pkg = Targets.selected(app);
        store.work.execute(() -> {
            ModEngine.Result r = ModEngine.apply(app, pkg, false, null, null);
            store.main.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                Notify.say(this, r == ModEngine.Result.UNCHANGED ? Notify.GENERAL : ModEngine.ok(r) ? Notify.MODS : null, modsResult(r));
            });
        });
    }

    private int modsResult(ModEngine.Result r) {
        switch (r) {
            case APPLIED:
                return R.string.bar_saved_mods_applied;
            case UNCHANGED:
                return R.string.bar_saved;
            case REMOVED:
                return R.string.bar_saved_mods_removed;
            case NO_ROOT:
                return R.string.mods_result_no_root;
            case NOT_INSTALLED:
                return R.string.launch_not_installed;
            case NO_SPACE:
                return R.string.mods_error_space_full;
            case CANCELLED:
                return R.string.mods_error_cancelled_full;
            default:
                return R.string.mods_result_failed;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyRootGate();
        Updater.resumed(this);
        store.changed();
        Crash.run("update resume", () -> Updater.resume(this));
        Crash.run("update check", () -> Updater.auto(this));
        offerPendingLaunch();
        offerTestingNotice();
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastJoinImport > 15_000) {
            lastJoinImport = now;
            android.content.Context app = getApplicationContext();
            store.work.execute(() -> {
                Helper.keepRootHelper(app);
                Crash.run("shizuku helper", () -> ShizukuHelper.keepHelper(app));
                LibraryData.importJoins(app);
            });
        }
    }

    @Override
    protected void onPause() {
        Updater.paused(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(EXTRA_TAB, current);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_UI_HIDDEN) release();
    }

    private void release() {
        releaseHiddenTabs();
        Net.trim();
        store.work.execute(System::gc);
    }

    private void releaseHiddenTabs() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved() && fm.isDestroyed()) return;
        FragmentTransaction t = null;
        for (int tab : TABS) {
            Fragment f = fm.findFragmentByTag("tab" + tab);
            if (f == null || !f.isHidden()) continue;
            if (t == null) t = fm.beginTransaction().setReorderingAllowed(true);
            t.remove(f);
        }
        if (t != null) t.commitNowAllowingStateLoss();
    }

    private void offerTestingNotice() {
        if (testingNoticeOpen || isFinishing() || "1".equals(store.setting(TESTING_NOTICE, "0")) || OnboardingActivity.due(this)) return;
        testingNoticeOpen = true;
        Ui.alert(this)
                .setTitle(R.string.testing_notice_title)
                .setMessage(R.string.testing_notice_body)
                .setCancelable(false)
                .setPositiveButton(R.string.testing_notice_ok, (dialog, which) -> store.putSetting(TESTING_NOTICE, "1"))
                .setOnDismissListener(dialog -> testingNoticeOpen = false)
                .show();
    }

    private void offerPendingLaunch() {
        String pending = store.setting("pending", null);
        if (pending == null) return;
        String pkg = Targets.selected(this);
        if (!Targets.resolve(this, pkg).ready()) return;
        Deeplink d = Deeplink.parse(pending);
        String name = store.setting("pendingName", "");
        if (d == null) {
            clearPending();
            return;
        }
        clearPending();
        Actions.launch(this, d, name);
    }

    private void clearPending() {
        store.putSetting("pending", null);
        store.putSetting("pendingName", null);
    }

    public void select(int id) {
        if (nav != null && nav.getMenu().findItem(id) != null && nav.getSelectedItemId() != id) nav.setSelectedItemId(id);
        else show(id);
    }

    private final androidx.activity.OnBackPressedCallback editorBack = new androidx.activity.OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            show(parent(current));
        }
    };

    private static int parent(int id) {
        if (id == R.id.nav_flag_editor) return R.id.nav_flags;
        if (id == R.id.nav_integrations || id == R.id.nav_smart) return R.id.nav_settings;
        return id;
    }

    private void show(int id) {
        if (id == R.id.nav_mods && RootGate.hideRootFeatures(this)) id = R.id.nav_home;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction().setReorderingAllowed(true);
        for (int tab : TABS) {
            String tag = "tab" + tab;
            Fragment f = fm.findFragmentByTag(tag);
            if (tab == id) {
                if (f == null) t.add(R.id.content, create(tab), tag);
                else t.show(f);
            } else if (f != null && !f.isHidden()) {
                t.hide(f);
            }
        }
        Crash.run("tab switch", t::commitNowAllowingStateLoss);
        if (id != current) {
            Fragment shown = fm.findFragmentByTag("tab" + id);
            if (shown != null && shown.getView() != null) enter(shown.getView());
        }
        current = id;
        View content = findViewById(R.id.content);
        if (content != null) content.post(() -> Crash.run("translate tab", () -> LiveTranslator.apply(this)));
        AppPresence.page(id);
        editorBack.setEnabled(parent(id) != id && nav != null && nav.getMenu().findItem(id) == null);
        if (nav != null && nav.getMenu().findItem(id) == null) {
            MenuItem owner = nav.getMenu().findItem(parent(id));
            if (owner != null) owner.setChecked(true);
        }
        updateSidebar();
    }

    private static final android.view.animation.Interpolator ENTER_EASE = new android.view.animation.DecelerateInterpolator(1.5f);

    private void enter(View v) {
        v.animate().cancel();
        v.setAlpha(0f);
        if (sidebarItems != null) {
            float offset = Ui.dp(this, 16);
            v.setTranslationX(v.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? offset : -offset);
            v.setTranslationY(0f);
        } else {
            v.setTranslationX(0f);
            v.setTranslationY(Ui.dp(this, 18));
        }
        v.post(() -> v.animate().alpha(1f).translationX(0f).translationY(0f).setDuration(250).setInterpolator(ENTER_EASE).withLayer().start());
    }

    private static Fragment create(int id) {
        if (id == R.id.nav_mods) return new ModsFragment();
        if (id == R.id.nav_flags) return new FlagSettingsFragment();
        if (id == R.id.nav_flag_editor) return new FlagsFragment();
        if (id == R.id.nav_settings) return new SettingsFragment();
        if (id == R.id.nav_integrations) return new IntegrationsFragment();
        if (id == R.id.nav_smart) {
            Page smart = SmartJoin.page();
            if (smart != null) return smart;
        }
        return new HomeFragment();
    }

    /** Hides root-only tabs while Shizuku or USB debugging is active and no real root is available. */
    private void applyRootGate() {
        boolean hide = RootGate.hideRootFeatures(this);
        if (nav != null) {
            MenuItem mods = nav.getMenu().findItem(R.id.nav_mods);
            if (mods != null) mods.setVisible(!hide);
        }
        if (sidebarItems != null) {
            View row = sidebarItems.findViewById(R.id.nav_mods);
            if (row != null) row.setVisibility(hide ? View.GONE : View.VISIBLE);
        }
        if (hide && current == R.id.nav_mods) select(R.id.nav_home);
    }

    private void buildSidebar() {
        Menu menu = new PopupMenu(this, sidebarItems).getMenu();
        getMenuInflater().inflate(R.menu.nav_wide, menu);
        LayoutInflater inf = getLayoutInflater();
        LinearLayout footer = findViewById(R.id.sidebar_footer);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            LinearLayout parent = id == R.id.nav_settings ? footer : sidebarItems;
            View row = inf.inflate(R.layout.item_sidebar, parent, false);
            row.setId(id);
            ((TextView) row.findViewById(R.id.label)).setText(item.getTitle());
            ((android.widget.ImageView) row.findViewById(R.id.icon)).setImageDrawable(item.getIcon());
            row.setOnClickListener(v -> show(id));
            parent.addView(row);
        }
    }

    private void updateSidebar() {
        if (sidebarItems == null) return;
        for (int tab : TABS) {
            View row = findViewById(tab);
            if (row == null) continue;
            boolean on = tab == current;
            if (row.isSelected() == on) continue;
            row.setSelected(on);
        }
    }

    /** Turns the bottom bar into a floating, fully rounded Material 3 pill. */
    private void floatNav() {
        if (nav.getBackground() instanceof MaterialShapeDrawable) {
            MaterialShapeDrawable bg = (MaterialShapeDrawable) nav.getBackground();
            bg.setShapeAppearanceModel(ShapeAppearanceModel.builder().setAllCornerSizes(new RelativeCornerSize(0.5f)).build());
        }
        nav.setClipToOutline(true);
    }

    private void applyInsets() {
        View root = findViewById(R.id.root);
        View content = findViewById(R.id.column);
        View bar = findViewById(R.id.action_bar);
        View divider = findViewById(R.id.bar_divider);
        boolean wide = sidebarItems != null;
        if (wide) getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.attr(this, R.attr.vsSidebar)));
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = ime.bottom > bars.bottom;
            int visibility = imeVisible ? View.GONE : View.VISIBLE;
            bar.setVisibility(visibility);
            divider.setVisibility(visibility);
            if (wide) {
                root.setPadding(bars.left, bars.top, bars.right, imeVisible ? ime.bottom : bars.bottom);
            } else {
                nav.setVisibility(visibility);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) nav.getLayoutParams();
                lp.leftMargin = Ui.dp(this, 16) + bars.left;
                lp.rightMargin = Ui.dp(this, 16) + bars.right;
                lp.bottomMargin = Ui.dp(this, 12) + bars.bottom;
                nav.setLayoutParams(lp);
                content.setPadding(bars.left, bars.top, bars.right, imeVisible ? ime.bottom : 0);
            }
            return insets;
        });
    }
}
