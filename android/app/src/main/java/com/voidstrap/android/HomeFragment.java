package com.voidstrap.android;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

public final class HomeFragment extends Page {
    private static final long FEED_REFRESH_MS = 10 * 60_000L;
    private static final long FEED_RETRY_MS = 2 * 60_000L;
    private static final long WELCOME_REFRESH_MS = 60 * 60_000L;
    private static final long ACCOUNT_REFRESH_MS = 10 * 60_000L;
    private static final long ALERT_REFRESH_MS = 30 * 60_000L;
    private static final long AUTO_REFRESH_MS = 15 * 60_000L;


    private LinearLayout catalogGrid;
    private TextView catalogStatus;
    private long catalogDueAt;
    private TextView accountGreeting;
    private TextView accountRobux;
    private TextView accountTickets;
    private ImageView accountAvatar;
    private View accountCard;
    private View accountSignIn;
    private TextView libraryHeader;
    private LinearLayout libraryGrid;
    private long accountDueAt;
    private View alertBanner;
    private TextView alertText;
    private MaterialButton alertLink;
    private String alertUrl;
    private long alertDueAt;
    private final android.os.Handler autoHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable autoRefreshTick = new Runnable() {
        @Override
        public void run() {
            forceRefresh();
            autoHandler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    private final android.net.ConnectivityManager.NetworkCallback online = new android.net.ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull android.net.Network network) {
            store.main.post(HomeFragment.this::reconnected);
        }

        @Override
        public void onBlockedStatusChanged(@NonNull android.net.Network network, boolean blocked) {
            if (!blocked) store.main.post(HomeFragment.this::reconnected);
        }
    };
    private android.net.ConnectivityManager watching;
    private long watchingSince;


    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onStart() {
        super.onStart();
        android.net.ConnectivityManager cm = requireContext().getSystemService(android.net.ConnectivityManager.class);
        if (cm == null) return;
        watchingSince = android.os.SystemClock.elapsedRealtime();
        try {
            cm.registerDefaultNetworkCallback(online);
            watching = cm;
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onStop() {
        if (watching != null) {
            try {
                watching.unregisterNetworkCallback(online);
            } catch (RuntimeException ignored) {
            }
            watching = null;
        }
        super.onStop();
    }

    private void reconnected() {
        if (!isAdded() || getView() == null || isHidden()) return;
        if (android.os.SystemClock.elapsedRealtime() - watchingSince < 3000) return;
        refresh();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        accountGreeting = v.findViewById(R.id.account_greeting);
        accountAvatar = v.findViewById(R.id.account_avatar);
        accountRobux = v.findViewById(R.id.account_robux);
        accountTickets = v.findViewById(R.id.account_tickets);
        ((View) accountRobux.getParent()).setVisibility(View.GONE);
        ((View) accountTickets.getParent()).setVisibility(View.GONE);
        v.findViewById(R.id.account_limiteds).setOnClickListener(x -> startActivity(new Intent(requireContext(), LimitedsActivity.class)));
        accountCard = v.findViewById(R.id.account_card);
        accountSignIn = v.findViewById(R.id.account_signin);
        libraryHeader = v.findViewById(R.id.library_header);
        libraryGrid = v.findViewById(R.id.library_grid);
        accountSignIn.setOnClickListener(x -> PekoraSession.toggle(requireActivity()));
        alertBanner = v.findViewById(R.id.alert_banner);
        alertText = v.findViewById(R.id.alert_text);
        alertLink = v.findViewById(R.id.alert_link);
        alertLink.setOnClickListener(x -> openAlertLink());
        catalogGrid = v.findViewById(R.id.catalog_grid);
        catalogStatus = v.findViewById(R.id.catalog_status);
        v.findViewById(R.id.catalog_refresh).setOnClickListener(x -> forceRefresh());
        catalogDueAt = 0;
    }

    @Override
    protected void refresh() {
        long now = System.currentTimeMillis();
        if (now >= catalogDueAt) loadCatalog();
        loadLibrary();
        bindWelcome();
        boolean in = PekoraSession.signedIn(requireContext());
        if (accountCard != null) accountCard.setVisibility(in ? View.VISIBLE : View.GONE);
        if (accountSignIn != null) accountSignIn.setVisibility(in ? View.GONE : View.VISIBLE);
        if (in && now >= accountDueAt) loadAccount();
        if (now >= alertDueAt) loadAlert();
    }

    private int contentDp() {
        Configuration cfg = getResources().getConfiguration();
        int w = cfg.screenWidthDp;
        int nav = w >= 840 ? 240 : w >= 600 ? 80 : 0;
        int inset = w >= 600 ? 24 : 16;
        return Math.max(200, w - nav - inset * 2);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void fill(LinearLayout container, List<View> cells, int cols, int gapDp) {
        container.removeAllViews();
        int gap = Ui.dp(requireContext(), gapDp);
        for (int i = 0; i < cells.size(); i += cols) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            for (int k = 0; k < cols; k++) {
                View cell = i + k < cells.size() ? cells.get(i + k) : new Space(requireContext());
                ViewGroup.LayoutParams own = cell.getLayoutParams();
                int height = own != null && own.height > 0 ? own.height : ViewGroup.LayoutParams.WRAP_CONTENT;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
                if (k < cols - 1) lp.setMarginEnd(gap);
                row.addView(cell, lp);
            }
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i + cols < cells.size()) rp.bottomMargin = gap;
            container.addView(row, rp);
        }
    }



    /** Shows games from the user's library, pinned first. Uses the local library and the place-details lookup. */
    private void loadLibrary() {
        Context app = requireContext().getApplicationContext();
        store.work.execute(() -> {
            boolean playingChanged = LibraryData.refreshPlaying(app);
            if (playingChanged) store.main.post(this::loadLibrary);
            List<LibraryData.Game> games = LibraryData.base(store);
            try {
                LibraryData.enrich(app, games);
            } catch (RuntimeException ignored) {
            }
            List<LibraryData.Game> out = new ArrayList<>(games);
            store.main.post(() -> bindLibrary(out));
        });
    }

    private void bindLibrary(List<LibraryData.Game> games) {
        if (getView() == null || libraryGrid == null) return;
        boolean has = games != null && !games.isEmpty();
        libraryHeader.setVisibility(has ? View.VISIBLE : View.GONE);
        libraryGrid.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) return;
        List<LibraryData.Game> shown = new ArrayList<>();
        for (LibraryData.Game g : games) {
            if (g.pinned && shown.size() < 6) shown.add(g);
        }
        for (LibraryData.Game g : games) {
            if (!g.pinned && shown.size() < 6) shown.add(g);
        }
        List<View> cells = new ArrayList<>();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (LibraryData.Game g : shown) {
            View card = inf.inflate(R.layout.item_catalog_card, libraryGrid, false);
            ((TextView) card.findViewById(R.id.catalog_name)).setText(g.name.isEmpty() ? String.valueOf(g.placeId) : g.name);
            ((TextView) card.findViewById(R.id.catalog_creator)).setText(g.creator);
            String tag = g.live ? getString(R.string.home_library_playing) : g.pinned ? getString(R.string.home_library_pinned) : "";
            ((TextView) card.findViewById(R.id.catalog_type)).setText(tag);
            ((TextView) card.findViewById(R.id.catalog_price)).setText("");
            card.findViewById(R.id.catalog_limited).setVisibility(View.GONE);
            Net.image((ImageView) card.findViewById(R.id.catalog_image), g.iconUrl, R.color.vs_news_surface, Ui.dp(requireContext(), 150));
            card.setOnClickListener(x -> Ui.openWeb(requireContext(), "https://octane.wtf/games/" + g.placeId));
            cells.add(card);
        }
        fill(libraryGrid, cells, contentDp() >= 560 ? 3 : 2, 12);
    }

    private void loadCatalog() {
        catalogDueAt = System.currentTimeMillis() + FEED_REFRESH_MS;
        Context app = requireContext().getApplicationContext();
        store.work.execute(() -> {
            List<HomeFeed.Item> items = null;
            try {
                items = HomeFeed.catalog(app);
            } catch (IOException | RuntimeException e) {
                android.util.Log.w("Voidstrap", "Catalog feed failed", e);
            }
            List<HomeFeed.Item> out = items;
            store.main.post(() -> bindCatalog(out));
        });
    }

    private void bindCatalog(List<HomeFeed.Item> items) {
        if (getView() == null) return;
        if (items == null) catalogDueAt = System.currentTimeMillis() + FEED_RETRY_MS;
        if (items == null && catalogGrid.getChildCount() > 0) return;
        boolean has = items != null && !items.isEmpty();
        catalogGrid.setVisibility(has ? View.VISIBLE : View.GONE);
        catalogStatus.setVisibility(has ? View.GONE : View.VISIBLE);
        catalogStatus.setText(items == null ? R.string.home_catalog_failed : R.string.home_catalog_empty);
        if (!has) return;
        List<View> cells = new ArrayList<>();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (HomeFeed.Item it : items) {
            View card = inf.inflate(R.layout.item_catalog_card, catalogGrid, false);
            ((TextView) card.findViewById(R.id.catalog_name)).setText(it.name);
            ((TextView) card.findViewById(R.id.catalog_creator)).setText(it.creator);
            ((TextView) card.findViewById(R.id.catalog_type)).setText(it.type);
            String price = HomeFeed.price(it);
            ((TextView) card.findViewById(R.id.catalog_price)).setText(price == null ? getString(R.string.home_github_empty) : price);
            card.findViewById(R.id.catalog_limited).setVisibility(it.limited ? View.VISIBLE : View.GONE);
            Net.image((ImageView) card.findViewById(R.id.catalog_image), it.image, R.color.vs_news_surface, Ui.dp(requireContext(), 150));
            card.setOnClickListener(x -> Ui.openWeb(requireContext(), it.link()));
            cells.add(card);
        }
        fill(catalogGrid, cells, contentDp() >= 560 ? 3 : 2, 12);
    }

    private String newsDate(long created) {
        if (created <= 0) return getString(R.string.home_news_announcement);
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(created);
        Calendar now = Calendar.getInstance();
        long days = (startOfDay(now) - startOfDay(then)) / DateUtils.DAY_IN_MILLIS;
        if (days <= 0) return getString(R.string.home_news_today);
        if (days == 1) return getString(R.string.home_news_yesterday);
        if (days < 7) return getResources().getQuantityString(R.plurals.home_news_days_ago, (int) days, (int) days);
        return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date(created)).toLowerCase(Locale.getDefault());
    }

    private static long startOfDay(Calendar c) {
        Calendar d = (Calendar) c.clone();
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        return d.getTimeInMillis();
    }

    private void bindWelcome() {
        if (accountGreeting == null) return;
        String name = PekoraSession.signedIn(requireContext()) ? store.setting(PekoraSession.NAME_SETTING, "") : "";
        accountGreeting.setText(name.isEmpty() ? getString(R.string.home_hello_fallback) : getString(R.string.home_hello, name));
    }

    @Override
    public void onResume() {
        super.onResume();
        autoHandler.removeCallbacks(autoRefreshTick);
        autoHandler.postDelayed(autoRefreshTick, AUTO_REFRESH_MS);
    }

    @Override
    public void onPause() {
        autoHandler.removeCallbacks(autoRefreshTick);
        super.onPause();
    }

    /** Marks every feed as due and reloads them now. Used by the 15-minute timer and the refresh buttons. */
    private void forceRefresh() {
        if (!isAdded() || getView() == null) return;
        catalogDueAt = 0;
        accountDueAt = 0;
        alertDueAt = 0;
        refresh();
    }




    private String count(long v) {
        return v < 0 ? getString(R.string.home_github_empty) : String.format(Locale.getDefault(), "%,d", v);
    }

    private void loadAccount() {
        accountDueAt = System.currentTimeMillis() + ACCOUNT_REFRESH_MS;
        Context app = requireContext().getApplicationContext();
        store.work.execute(() -> {
            long id = PekoraSession.userId(app);
            if (id <= 0) return;
            String avatarUrl = headshotUrl(id);
            store.main.post(() -> {
                if (avatarUrl != null) Net.image(accountAvatar, avatarUrl, R.color.vs_subtle_pressed, Ui.dp(requireContext(), 56));
                bindWelcome();
            });
        });
    }

    /** Headshot URL for the account avatar, or null when unavailable. */
    private static String headshotUrl(long userId) {
        JSONObject o = OctaneApi.headshots(String.valueOf(userId), "150x150");
        JSONArray data = o == null ? null : o.optJSONArray("data");
        JSONObject first = data == null ? null : data.optJSONObject(0);
        String url = first == null ? "" : first.optString("imageUrl", "");
        return url.startsWith("https://") ? url : null;
    }

    private void loadAlert() {
        alertDueAt = System.currentTimeMillis() + ALERT_REFRESH_MS;
        store.work.execute(() -> {
            JSONObject info = OctaneApi.alertInfo();
            store.main.post(() -> bindAlert(info));
        });
    }

    private void bindAlert(JSONObject info) {
        if (alertBanner == null) return;
        String text = info == null ? "" : info.optString("Text", "");
        boolean show = info != null && info.optBoolean("IsVisible", false) && !text.isEmpty();
        alertBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        alertText.setText(text);
        String link = info.optString("LinkText", "");
        String url = info.optString("LinkUrl", "");
        alertUrl = OctaneApi.safeLink(url) ? url : null;
        boolean showLink = alertUrl != null && !link.isEmpty();
        alertLink.setText(link);
        alertLink.setVisibility(showLink ? View.VISIBLE : View.GONE);
    }

    private void openAlertLink() {
        if (alertUrl != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(alertUrl)));
    }



    /**
     * Weekly commit counts for the last 52 weeks from GitHub's commit_activity stats. GitHub answers 202
     * while it is still computing them, which returns null here so the caller retries soon.

    /**
     * The same calendar-year graph built from the recent commit list, used while GitHub is still
     * computing commit_activity. Covers the latest 300 commits.






}
