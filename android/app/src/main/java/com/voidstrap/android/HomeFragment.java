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
    private static final long GITHUB_REFRESH_MS = 30 * 60_000L;
    private static final long WELCOME_REFRESH_MS = 60 * 60_000L;
    private static final long ACCOUNT_REFRESH_MS = 10 * 60_000L;
    private static final long ALERT_REFRESH_MS = 30 * 60_000L;
    private static final long AUTO_REFRESH_MS = 15 * 60_000L;
    private static final String GITHUB_API = "https://api.github.com/repos/7gups/Octifine";
    private static final String GITHUB_URL = "https://github.com/7gups/Octifine";
    private static final String[] GITHUB_HEADERS = {"User-Agent", "Voidstrap Android", "Accept", "application/vnd.github+json"};

    private View newsHeader;
    private TextView newsCount;
    private LinearLayout newsGrid;

    private long newsDueAt;
    private ContributionGraphView githubGraph;
    private TextView githubCaption;
    private long githubStars = -1;
    private long githubCommitCount = -1;
    private int githubYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
    private long githubDueAt;
    private LinearLayout catalogGrid;
    private TextView catalogStatus;
    private long catalogDueAt;
    private TextView accountGreeting;
    private TextView accountRobux;
    private TextView accountTickets;
    private ImageView accountAvatar;
    private View accountCard;
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
    private String githubSite;
    private Release githubRelease;
    private TextView githubReleaseTitle;
    private TextView githubReleaseMeta;
    private TextView githubReleaseNotes;
    private MaterialButton githubReleaseOpen;

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

    private static final class Release {
        String tag = "";
        String name = "";
        String date = "";
        String notes = "";
        String url = "";
    }

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
        if (newsGrid.getChildCount() > 0) return;
        newsDueAt = 0;
        refresh();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        newsHeader = v.findViewById(R.id.news_header);
        newsCount = v.findViewById(R.id.news_count);
        newsGrid = v.findViewById(R.id.news_grid);
        accountGreeting = v.findViewById(R.id.account_greeting);
        accountAvatar = v.findViewById(R.id.account_avatar);
        accountRobux = v.findViewById(R.id.account_robux);
        accountTickets = v.findViewById(R.id.account_tickets);
        ((View) accountRobux.getParent()).setVisibility(View.GONE);
        ((View) accountTickets.getParent()).setVisibility(View.GONE);
        v.findViewById(R.id.catalog_header).setVisibility(View.GONE);
        v.findViewById(R.id.account_limiteds).setOnClickListener(x -> startActivity(new Intent(requireContext(), LimitedsActivity.class)));
        accountCard = v.findViewById(R.id.account_card);
        alertBanner = v.findViewById(R.id.alert_banner);
        alertText = v.findViewById(R.id.alert_text);
        alertLink = v.findViewById(R.id.alert_link);
        alertLink.setOnClickListener(x -> openAlertLink());
        githubGraph = v.findViewById(R.id.github_graph);
        githubCaption = v.findViewById(R.id.github_caption);
        v.findViewById(R.id.github_open).setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))));
        v.findViewById(R.id.github_refresh).setOnClickListener(x -> loadGithub());
        githubReleaseTitle = v.findViewById(R.id.release_title);
        githubReleaseMeta = v.findViewById(R.id.release_meta);
        githubReleaseNotes = v.findViewById(R.id.release_notes);
        githubReleaseOpen = v.findViewById(R.id.release_open);
        v.findViewById(R.id.news_explore).setOnClickListener(x -> exploreNews());
        catalogGrid = v.findViewById(R.id.catalog_grid);
        catalogStatus = v.findViewById(R.id.catalog_status);
        v.findViewById(R.id.catalog_refresh).setOnClickListener(x -> forceRefresh());
        catalogDueAt = 0;
        newsDueAt = 0;
    }

    @Override
    protected void refresh() {
        long now = System.currentTimeMillis();
        if (now >= newsDueAt) loadNews();
        if (now >= githubDueAt) loadGithub();
        bindWelcome();
        boolean in = PekoraSession.signedIn(requireContext());
        if (accountCard != null) accountCard.setVisibility(in ? View.VISIBLE : View.GONE);
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

    private void loadNews() {
        newsDueAt = System.currentTimeMillis() + FEED_REFRESH_MS;
        Context app = requireContext().getApplicationContext();
        store.work.execute(() -> {
            List<HomeFeed.News> news = null;
            try {
                news = HomeFeed.latestNews(app, 2);
            } catch (IOException | RuntimeException e) {
                android.util.Log.w("Voidstrap", "News feed failed", e);
            }
            List<HomeFeed.News> n = news;
            store.main.post(() -> bindNews(n));
        });
    }

    private void bindNews(List<HomeFeed.News> news) {
        if (getView() == null) return;
        if (news == null) newsDueAt = System.currentTimeMillis() + FEED_RETRY_MS;
        if (news == null && newsGrid.getChildCount() > 0) return;
        boolean has = news != null && !news.isEmpty();
        newsHeader.setVisibility(has ? View.VISIBLE : View.GONE);
        newsGrid.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) return;
        int recent = 0;
        for (HomeFeed.News n : news) if (n.created > 0 && System.currentTimeMillis() - n.created < 7 * DateUtils.DAY_IN_MILLIS) recent++;
        android.text.SpannableStringBuilder title = new android.text.SpannableStringBuilder(getString(R.string.home_news_title));
        if (recent > 0) {
            int start = title.length();
            title.append("  ").append(getResources().getQuantityString(R.plurals.home_news_count, recent, recent));
            title.setSpan(new android.text.style.AbsoluteSizeSpan(13, true), start, title.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setSpan(new android.text.style.ForegroundColorSpan(Ui.attr(requireContext(), R.attr.vsTextTertiary)), start, title.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), start, title.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        newsCount.setText(title);
        List<View> cells = new ArrayList<>();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (HomeFeed.News n : news) {
            View card = inf.inflate(R.layout.item_news_card, newsGrid, false);
            ((TextView) card.findViewById(R.id.news_title)).setText(HomeFeed.cardTitle(n.title));
            String date = newsDate(n.created);
            ((TextView) card.findViewById(R.id.news_date)).setText(date);
            View pill = card.findViewById(R.id.news_tag_pill);
            pill.setVisibility(n.tag.isEmpty() ? View.GONE : View.VISIBLE);
            ((TextView) card.findViewById(R.id.news_tag)).setText(n.tag);
            ImageView image = card.findViewById(R.id.news_image);
            Net.image(image, HomeFeed.thumbnail(n.image, 760), R.color.vs_news_surface, Ui.dp(requireContext(), 380));
            card.setContentDescription(getString(R.string.home_news_description, n.title, date));
            card.setOnClickListener(x -> Ui.openWeb(requireContext(), n.url));
            cells.add(card);
        }
        fill(newsGrid, cells, contentDp() >= 560 ? 2 : 1, 14);
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
        newsDueAt = 0;
        githubDueAt = 0;
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
        JSONObject o = PekoraApi.headshots(String.valueOf(userId), "150x150");
        JSONArray data = o == null ? null : o.optJSONArray("data");
        JSONObject first = data == null ? null : data.optJSONObject(0);
        String url = first == null ? "" : first.optString("imageUrl", "");
        return url.startsWith("https://") ? url : null;
    }

    private void loadAlert() {
        alertDueAt = System.currentTimeMillis() + ALERT_REFRESH_MS;
        store.work.execute(() -> {
            JSONObject info = PekoraApi.alertInfo();
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
        alertUrl = PekoraApi.safeLink(url) ? url : null;
        boolean showLink = alertUrl != null && !link.isEmpty();
        alertLink.setText(link);
        alertLink.setVisibility(showLink ? View.VISIBLE : View.GONE);
    }

    private void openAlertLink() {
        if (alertUrl != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(alertUrl)));
    }

    /** A year of commit activity: counts[week][day] (day 0 = Sunday), month labels, and the total. */
    private static final class CommitActivity {
        int[][] counts;
        String[] months;
        int total;
        int year;
    }

    private void loadGithub() {
        githubDueAt = System.currentTimeMillis() + GITHUB_REFRESH_MS;
        store.work.execute(() -> {
            Integer stars = null;
            String site = null;
            CommitActivity activity = null;
            Release rel = null;
            boolean relFailed = false;
            try {
                JSONObject repo = new JSONObject(Net.text(GITHUB_API, 200_000));
                stars = repo.getInt("stargazers_count");
                site = repo.optString("homepage", "");
            } catch (Exception e) {
                android.util.Log.w("Voidstrap", "GitHub repo failed", e);
            }
            try {
                activity = githubActivity();
            } catch (Exception e) {
                android.util.Log.w("Voidstrap", "GitHub activity failed", e);
            }
            try {
                rel = githubRelease();
            } catch (Exception e) {
                relFailed = true;
                android.util.Log.w("Voidstrap", "GitHub release failed", e);
            }
            Integer s = stars;
            String h = site;
            CommitActivity a = activity;
            Release r = rel;
            boolean f = relFailed;
            store.main.post(() -> bindGithub(s, a, h, r, f));
        });
    }

    /**
     * Weekly commit counts for the last 52 weeks from GitHub's commit_activity stats. GitHub answers 202
     * while it is still computing them, which returns null here so the caller retries soon.
     */
    private static CommitActivity githubActivity() throws Exception {
        Object[] res = Http.request("GET", GITHUB_API + "/stats/commit_activity", GITHUB_HEADERS, null, 15_000, 400_000, true);
        if (res == null) throw new java.io.IOException("GitHub request failed");
        int code = ((int[]) res[0])[0];
        if (code == 202) return null;
        if (code != 200) throw new java.io.IOException("HTTP " + code);
        JSONArray weeks = new JSONArray(new String((byte[]) res[2], StandardCharsets.UTF_8));
        // Calendar year, January to December: each week goes in the column for its Wednesday's day of the year.
        int year = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
        CommitActivity out = new CommitActivity();
        out.year = year;
        out.counts = new int[ContributionGraphView.WEEKS][7];
        out.months = new String[ContributionGraphView.WEEKS];
        SimpleDateFormat fmt = new SimpleDateFormat("MMM", Locale.getDefault());
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String[] monthOfColumn = new String[ContributionGraphView.WEEKS];
        for (int i = 0; i < weeks.length(); i++) {
            JSONObject week = weeks.getJSONObject(i);
            long mid = week.optLong("week", 0) * 1000L + 3L * 86400000L;
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.setTimeInMillis(mid);
            if (c.get(Calendar.YEAR) != year) continue;
            int col = (c.get(Calendar.DAY_OF_YEAR) - 1) / 7;
            if (col < 0 || col >= ContributionGraphView.WEEKS) continue;
            JSONArray days = week.optJSONArray("days");
            for (int d = 0; days != null && d < 7 && d < days.length(); d++) {
                out.counts[col][d] = days.optInt(d, 0);
                out.total += out.counts[col][d];
            }
            monthOfColumn[col] = fmt.format(new Date(mid));
        }
        String last = "";
        for (int col = 0; col < ContributionGraphView.WEEKS; col++) {
            if (monthOfColumn[col] != null && !monthOfColumn[col].equals(last)) {
                out.months[col] = monthOfColumn[col];
                last = monthOfColumn[col];
            }
        }
        return out;
    }

    /** Latest published release, or null when the repo has none yet (GitHub answers 404). */
    private static Release githubRelease() throws Exception {
        Object[] res = Http.request("GET", GITHUB_API + "/releases/latest", GITHUB_HEADERS, null, 15_000, 400_000, true);
        if (res == null) throw new java.io.IOException("GitHub request failed");
        int code = ((int[]) res[0])[0];
        if (code == 404) return null;
        if (code != 200) throw new java.io.IOException("HTTP " + code);
        JSONObject o = new JSONObject(new String((byte[]) res[2], StandardCharsets.UTF_8));
        Release r = new Release();
        r.tag = o.optString("tag_name", "");
        r.name = o.optString("name", "");
        String published = o.optString("published_at", "");
        r.date = published.length() >= 10 ? published.substring(0, 10) : published;
        r.notes = plainNotes(o.optString("body", ""));
        String html = o.optString("html_url", "");
        r.url = html.startsWith("https://github.com/") ? html : GITHUB_URL + "/releases";
        return r;
    }

    /** Strips the most common markdown so release notes read as plain text in the card. */
    private static String plainNotes(String md) {
        String t = md.replaceAll("(?m)^\\s*#+\\s*", "").replaceAll("[*_`]", "").replaceAll("\n{3,}", "\n\n").trim();
        return t.length() > 600 ? t.substring(0, 600).trim() + "\u2026" : t;
    }

    private void bindGithub(Integer stars, CommitActivity activity, String site, Release rel, boolean relFailed) {
        if (githubGraph == null) return;
        if (stars != null) githubStars = stars;
        if (activity != null) {
            githubGraph.setData(activity.counts, activity.months);
            githubCommitCount = activity.total;
            githubYear = activity.year;
        } else {
            githubDueAt = System.currentTimeMillis() + FEED_RETRY_MS;
        }
        githubCaption.setText(getString(R.string.home_github_caption_year, count(githubCommitCount), githubYear, count(githubStars)));
        bindRelease(rel, relFailed);
    }

    private void bindRelease(Release r, boolean failed) {
        if (githubReleaseTitle == null) return;
        if (r == null && failed && githubRelease != null) return; // keep the last good release on a failed refresh
        githubRelease = r;
        if (r == null) {
            githubReleaseTitle.setText(failed ? R.string.home_release_failed : R.string.home_release_none);
            githubReleaseMeta.setVisibility(View.GONE);
            githubReleaseNotes.setVisibility(View.GONE);
            githubReleaseOpen.setVisibility(View.GONE);
            return;
        }
        githubReleaseTitle.setText(r.name.isEmpty() ? r.tag : r.name);
        githubReleaseMeta.setText(getString(R.string.home_release_meta, r.tag, r.date));
        githubReleaseMeta.setVisibility(View.VISIBLE);
        githubReleaseNotes.setText(r.notes);
        githubReleaseNotes.setVisibility(r.notes.isEmpty() ? View.GONE : View.VISIBLE);
        githubReleaseOpen.setVisibility(View.VISIBLE);
        githubReleaseOpen.setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(r.url))));
    }

    private void exploreNews() {
        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        TextView loading = new TextView(requireContext());
        loading.setText(R.string.home_news_loading);
        loading.setTextAppearance(R.style.TextAppearance_Voidstrap_Caption);
        list.addView(loading);
        MaterialButton all = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        all.setText(R.string.home_news_open_forum);
        all.setIconResource(R.drawable.ic_globe);
        all.setOnClickListener(x -> Ui.openWeb(requireContext(), HomeFeed.NEWS_PAGE));
        Ui.surface(host(), R.string.home_news_all_title, R.string.home_news_all_body, list).show();
        Context app = requireContext().getApplicationContext();
        store.work.execute(() -> {
            List<HomeFeed.News> news = null;
            try {
                news = HomeFeed.allNews(app);
            } catch (IOException | RuntimeException ignored) {
            }
            List<HomeFeed.News> n = news;
            store.main.post(() -> {
                if (!isAdded()) return;
                list.removeAllViews();
                if (n == null || n.isEmpty()) {
                    loading.setText(R.string.home_news_failed);
                    list.addView(loading);
                }
                if (n != null) for (HomeFeed.News item : n) {
                    Row row = Row.inflate(list);
                    String detail = item.tag.isEmpty() ? newsDate(item.created) : getString(R.string.pair, item.tag, newsDate(item.created));
                    row.set(R.drawable.ic_megaphone, item.title, detail);
                    if (!item.image.isEmpty()) row.showImage(HomeFeed.thumbnail(item.image, 160), R.drawable.ic_megaphone);
                    row.title.setMaxLines(2);
                    row.chevron.setVisibility(View.VISIBLE);
                    row.view.setOnClickListener(x -> Ui.openWeb(requireContext(), item.url));
                    list.addView(row.view);
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = Ui.dp(requireContext(), 12);
                list.addView(all, lp);
            });
        });
    }
}
