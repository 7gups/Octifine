package com.voidstrap.android;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The account's limiteds, with search, category and sort menus, a list or grid view, and paging. */
public final class LimitedsActivity extends AppCompatActivity {
    private static final int PAGE_SIZE = 24;
    private static final String[] CATEGORY_NAMES = {"All", "Hats", "Hair", "Face", "Neck", "Shoulder", "Front", "Back", "Waist", "Heads", "Faces", "Gears", "Decals"};
    private static final String[] CATEGORY_IDS = {"null", "8", "41", "42", "43", "44", "45", "46", "47", "17", "18", "19", "13"};

    private final List<JSONObject> items = new ArrayList<>();
    private final Map<Long, String> thumbs = new HashMap<>();
    private Store store;
    private long userId;
    private String cursor = "";
    private String nextCursor;
    private boolean loading;
    private String category = "null";
    private int sortMode;
    private boolean gridMode;
    private LinearLayout list;
    private TextView status;
    private MaterialButton more;
    private android.widget.EditText search;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_limiteds);
        store = Store.get(this);
        userId = PekoraSession.userId(this);
        if (userId <= 0) {
            finish();
            return;
        }
        list = findViewById(R.id.limiteds_list);
        status = findViewById(R.id.limiteds_status);
        more = findViewById(R.id.limiteds_more);
        search = findViewById(R.id.limiteds_search);

        MaterialAutoCompleteTextView categoryView = findViewById(R.id.limiteds_category);
        categoryView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CATEGORY_NAMES));
        categoryView.setText(CATEGORY_NAMES[0], false);
        categoryView.setOnItemClickListener((parent, v, position, id) -> {
            String next = CATEGORY_IDS[position];
            if (!next.equals(category)) {
                category = next;
                reload();
            }
        });

        MaterialAutoCompleteTextView sortView = findViewById(R.id.limiteds_sort);
        sortView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.limiteds_sort_options)));
        sortView.setText(getResources().getStringArray(R.array.limiteds_sort_options)[0], false);
        sortView.setOnItemClickListener((parent, v, position, id) -> {
            if (position != sortMode) {
                sortMode = position;
                render();
            }
        });

        MaterialButtonToggleGroup viewToggle = findViewById(R.id.limiteds_view_toggle);
        viewToggle.check(R.id.limiteds_view_list_btn);
        viewToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean grid = checkedId == R.id.limiteds_view_grid_btn;
            if (grid != gridMode) {
                gridMode = grid;
                render();
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        more.setOnClickListener(x -> loadPage());
        ScrollView scroll = findViewById(R.id.limiteds_scroll);
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> maybeLoadMore());
        reload();
    }

    private void reload() {
        items.clear();
        thumbs.clear();
        cursor = "";
        nextCursor = null;
        render();
        loadPage();
    }

    private void loadPage() {
        if (loading) return;
        loading = true;
        status.setText(R.string.limiteds_loading);
        more.setVisibility(View.GONE);
        String requestCursor = cursor;
        String requestCategory = category;
        store.work.execute(() -> {
            JSONObject page = PekoraApi.collectibles(userId, requestCursor, PAGE_SIZE, requestCategory, null);
            JSONArray data = page == null ? null : page.optJSONArray("data");
            List<JSONObject> got = new ArrayList<>();
            Map<Long, String> urls = new HashMap<>();
            if (data != null) {
                StringBuilder ids = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject o = data.optJSONObject(i);
                    if (o == null) continue;
                    got.add(o);
                    if (ids.length() > 0) ids.append(',');
                    ids.append(o.optLong("assetId", 0));
                }
                if (ids.length() > 0) {
                    JSONObject t = PekoraApi.assetThumbs(ids.toString(), "150x150");
                    JSONArray td = t == null ? null : t.optJSONArray("data");
                    for (int i = 0; td != null && i < td.length(); i++) {
                        JSONObject x = td.optJSONObject(i);
                        if (x != null && "Completed".equals(x.optString("state", "")) && x.optString("imageUrl", "").startsWith("https://")) {
                            urls.put(x.optLong("targetId", 0), x.optString("imageUrl", ""));
                        }
                    }
                }
            }
            String next = page == null ? "" : page.optString("nextPageCursor", "");
            boolean failed = data == null;
            runOnUiThread(() -> {
                loading = false;
                if (failed) {
                    status.setText(R.string.limiteds_failed);
                    more.setVisibility(nextCursor != null ? View.VISIBLE : View.GONE);
                    return;
                }
                items.addAll(got);
                thumbs.putAll(urls);
                nextCursor = next.isEmpty() || next.equals("null") ? null : next;
                if (nextCursor != null) cursor = nextCursor;
                render();
            });
        });
    }

    private void render() {
        list.removeAllViews();
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<JSONObject> visible = new ArrayList<>();
        for (JSONObject o : items) {
            if (q.isEmpty() || o.optString("name", "").toLowerCase(Locale.ROOT).contains(q)) visible.add(o);
        }
        sortItems(visible);
        if (gridMode) renderGrid(visible);
        else for (JSONObject o : visible) list.addView(row(o));
        int shown = visible.size();
        if (items.isEmpty() && !loading) status.setText(R.string.limiteds_empty);
        else if (shown == 0 && !q.isEmpty()) status.setText(R.string.limiteds_no_match);
        else status.setText(getString(R.string.limiteds_count, shown));
        more.setVisibility(nextCursor != null && !loading ? View.VISIBLE : View.GONE);
        list.post(this::maybeLoadMore);
    }

    /** Loads the next page automatically once the list is close to its bottom edge. */
    private void maybeLoadMore() {
        if (nextCursor == null || loading) return;
        ScrollView scroll = findViewById(R.id.limiteds_scroll);
        View content = scroll.getChildAt(0);
        int remaining = content.getHeight() - (scroll.getHeight() + scroll.getScrollY());
        if (remaining < Math.round(600 * getResources().getDisplayMetrics().density)) loadPage();
    }

    private interface Key {
        Long of(JSONObject o);
    }

    private static final Key RAP = o -> o.isNull("recentAveragePrice") ? null : o.optLong("recentAveragePrice", 0);
    private static final Key SERIAL = o -> o.isNull("serialNumber") ? null : o.optLong("serialNumber", 0);

    /** Sorts by RAP or serial. Items without that value always go last. */
    private void sortItems(List<JSONObject> out) {
        Key key;
        boolean descending;
        switch (sortMode) {
            case 1: key = RAP; descending = true; break;
            case 2: key = RAP; descending = false; break;
            case 3: key = SERIAL; descending = false; break;
            case 4: key = SERIAL; descending = true; break;
            default: return;
        }
        Collections.sort(out, (a, b) -> {
            Long x = key.of(a), y = key.of(b);
            if (x == null && y == null) return 0;
            if (x == null) return 1;
            if (y == null) return -1;
            return descending ? Long.compare(y, x) : Long.compare(x, y);
        });
    }

    private void renderGrid(List<JSONObject> visible) {
        float d = getResources().getDisplayMetrics().density;
        int margin = Math.round(4 * d);
        LinearLayout pair = null;
        for (int i = 0; i < visible.size(); i++) {
            if (i % 2 == 0) {
                pair = new LinearLayout(this);
                pair.setOrientation(LinearLayout.HORIZONTAL);
                list.addView(pair, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(margin, margin, margin, margin);
            pair.addView(cell(visible.get(i)), lp);
        }
        if (visible.size() % 2 == 1 && pair != null) {
            pair.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));
        }
    }

    private View row(JSONObject o) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Math.round(12 * d);
        content.setPadding(pad, pad, pad, pad);

        ImageView img = new ImageView(this);
        String url = thumbs.get(o.optLong("assetId", 0));
        if (url != null) Net.image(img, url, R.color.vs_subtle_pressed, Math.round(56 * d));
        content.addView(img, new LinearLayout.LayoutParams(Math.round(56 * d), Math.round(56 * d)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(Math.round(12 * d), 0, 0, 0);
        TextView name = new TextView(this);
        name.setText(o.optString("name", ""));
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurface));
        text.addView(name);
        text.addView(details(o));
        content.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        return openable(card(content, d, 8), o);
    }

    private View cell(JSONObject o) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(10 * d);
        content.setPadding(pad, pad, pad, pad);

        ImageView img = new ImageView(this);
        String url = thumbs.get(o.optLong("assetId", 0));
        if (url != null) Net.image(img, url, R.color.vs_subtle_pressed, Math.round(120 * d));
        content.addView(img, new LinearLayout.LayoutParams(Math.round(120 * d), Math.round(120 * d)));

        TextView name = new TextView(this);
        name.setText(o.optString("name", ""));
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(2);
        name.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurface));
        content.addView(name);
        content.addView(details(o));
        return openable(card(content, d, 0), o);
    }

    /** Opens the item's page on Octane when the card is tapped. */
    private View openable(MaterialCardView card, JSONObject o) {
        long id = o.optLong("assetId", 0);
        if (id > 0) card.setOnClickListener(x -> Ui.openWeb(this, "https://octane.wtf/catalog/" + id));
        return card;
    }

    private TextView details(JSONObject o) {
        StringBuilder sb = new StringBuilder();
        String price = o.isNull("recentAveragePrice") ? "--" : String.format(Locale.getDefault(), "%,d", o.optLong("recentAveragePrice", 0));
        sb.append(getString(R.string.limiteds_rap, price));
        if (!o.isNull("serialNumber")) {
            String copies = o.isNull("copyCount") ? "--" : String.valueOf(o.optLong("copyCount", 0));
            sb.append('\n').append(getString(R.string.limiteds_serial, String.valueOf(o.optLong("serialNumber", 0)), copies));
        }
        TextView t = new TextView(this);
        t.setText(sb.toString());
        t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        t.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        return t;
    }

    private MaterialCardView card(View content, float d, int bottomDp) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardElevation(0);
        card.setRadius(16 * d);
        card.addView(content);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Math.round(bottomDp * d));
        card.setLayoutParams(lp);
        return card;
    }
}
