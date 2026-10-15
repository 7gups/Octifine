package com.voidstrap.android;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FlagPresets {
    private static final String[] LOD = {
            "DFIntCSGLevelOfDetailSwitchingDistance",
            "DFIntCSGLevelOfDetailSwitchingDistanceL12",
            "DFIntCSGLevelOfDetailSwitchingDistanceL23",
            "DFIntCSGLevelOfDetailSwitchingDistanceL34"};
    private static final String MSAA = "FIntDebugForceMSAASamples";
    private static final String MSAA_OPTIONAL = "FIntDebugFRMOptionalMSAALevelOverride";
    private static final String DPI = "DFFlagDisableDPIScale";
    private static final String GRASS_MIN = "FIntFRMMinGrassDistance";
    private static final String GRASS_MAX = "FIntFRMMaxGrassDistance";
    private static final String VULKAN = "FFlagDebugGraphicsPreferVulkan";
    private static final String OPENGL = "FFlagDebugGraphicsPreferOpenGL";
    private static final String D3D11 = "FFlagDebugGraphicsPreferD3D11";
    private static final String NO_D3D11 = "FFlagDebugGraphicsDisableDirect3D11";
    private static final String TARGET_FPS = "DFIntTaskSchedulerTargetFps";
    private static final String VSYNC = "FFlagEnableAndroidVsync";
    private static final String LIMIT_240 = "FFlagTaskSchedulerLimitTargetFpsTo2402";
    private static final String SHOW_FPS = "FFlagDebugDisplayFPS";
    private static final long[] MSAA_VALUES = {0, 1, 2, 4, 8};
    private static final String[] SHADOWS = {"DFFlagDebugPauseVoxelizer", "FIntRenderShadowmapBias", "FIntRenderShadowIntensity"};
    private static final String BLUR = "FIntRobloxGuiBlurIntensity";
    private static final String FONT_PADDING = "FIntFontSizePadding";
    static final String[] UNDECLARED = {"FFlagDisablePostFx", "FIntRenderGrassDetailStrands",
            "FFlagDebugForceFutureIsBrightPhase2", "FFlagDebugForceFutureIsBrightPhase3",
            "FFlagRenderUnifiedLighting12", "DFIntAssetPreloading", "FFlagEnablePreferredTextSizeScale",
            "FFlagEnablePreferredTextSizeSettingInMenus2", "DFIntCSGLevelOfDetailSwitchingDistanceStatic"};
    private static final String FLAG_STATE = "FStringDebugShowFlagState";
    private static final String ASYNC_TEX = "FFlagAsyncTasksEnabledForTextures";
    private static final String MESH_OPT = "DFFlagMeshOptimizerV015";
    private static final String ALPHA_PREPASS = "FFlagRenderPrepassOpaqueWithAlpha2";
    private static final String AO = "FFlagRenderEnableHbao";
    private static final String INDOOR_SPEC = "FFlagRenderFixIndoorSpecIBL";
    private static final String MTU = "DFIntConnectionMTUSize";
    private static final String HIDE_GUI_GROUP = "DFIntCanHideGuiGroupId";
    private static final String CLOUDS = "FFlagRenderClouds";
    private static final String DROP_HAT = "DFFlagDropItLikeItsHat2";
    private static final String GOTHAM = "FFlagAvatarEditorGothamFont";
    private static final String DEATH_SOUNDS = "FFlagUserRotatingDeathSounds";
    private static final String BONES = "FFlagShowBonesAsConstraints";
    private static final String BODY_OUTLINE = "DFFlagDebugDrawBvhNodes";
    private static final String HUMANOID_DEBUG = "FFlagDebugHumanoidRendering";
    private static final long[][] PROFILES = {{2, 0, 0}, {1, 1, 1}, {4, 0, 0}};

    static final java.util.Set<String> KEYS = new java.util.HashSet<>();

    static {
        java.util.Collections.addAll(KEYS, MSAA, MSAA_OPTIONAL, DPI, GRASS_MIN, GRASS_MAX, VULKAN, OPENGL, D3D11, NO_D3D11,
                SHOW_FPS, BLUR, FONT_PADDING, FLAG_STATE);
        java.util.Collections.addAll(KEYS, LOD);
        java.util.Collections.addAll(KEYS, ASYNC_TEX, MESH_OPT, ALPHA_PREPASS, AO, INDOOR_SPEC);
        java.util.Collections.addAll(KEYS, MTU, HIDE_GUI_GROUP);
        java.util.Collections.addAll(KEYS, CLOUDS, DROP_HAT, GOTHAM, DEATH_SOUNDS, BONES, BODY_OUTLINE, HUMANOID_DEBUG);
        java.util.Collections.addAll(KEYS, SHADOWS);
    }

    private static final int MESH_MIN = 1;
    private static final int MESH_MAX = 3;

    private final AppCompatActivity a;
    private final Store store;
    private final Runnable commit;
    private final LinearLayout root;
    private int profile;

    public FlagPresets(AppCompatActivity a, Store store, Runnable commit) {
        this.a = a;
        this.store = store;
        this.commit = commit;
        root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
    }

    public View view() {
        return root;
    }

    private Map<String, Object> values() {
        return store.flags.active().values;
    }

    private long num(String key, long fallback) {
        Object v = values().get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean on(String key) {
        Object v = values().get(key);
        return Boolean.TRUE.equals(v) || (v instanceof String && "true".equalsIgnoreCase((String) v));
    }

    private void put(String key, Object value) {
        if (value == null) values().remove(key);
        else values().put(key, value);
    }

    static void setFps(Map<String, Object> v, long fps) {
        if (fps <= 0) v.remove(TARGET_FPS);
        else v.put(TARGET_FPS, fps);
        if (fps > 60) v.put(VSYNC, Boolean.FALSE);
        else v.remove(VSYNC);
        if (fps > 240) v.put(LIMIT_240, Boolean.FALSE);
        else v.remove(LIMIT_240);
    }

    static long targetFps(Map<String, Object> v) {
        Object t = v.get(TARGET_FPS);
        return t instanceof Number ? ((Number) t).longValue() : 0;
    }

    static void fixFps(Map<String, Object> v) {
        long fps = targetFps(v);
        if (fps > 60) setFps(v, fps);
    }

    private int pick(long[] options, String key) {
        if (!values().containsKey(key)) return 0;
        long value = num(key, Long.MIN_VALUE);
        for (int i = 1; i < options.length; i++) if (options[i] == value) return i;
        return -1;
    }

    private String[] labels(int array) {
        return a.getResources().getStringArray(array);
    }

    private void shadows(boolean off) {
        put(SHADOWS[0], off ? Boolean.TRUE : null);
        put(SHADOWS[1], off ? (Object) (-1L) : null);
        put(SHADOWS[2], off ? (Object) 0L : null);
    }

    private void mesh(int level) {
        for (int i = 0; i < LOD.length; i++) put(LOD[i], level <= 0 ? null : (Object) (long) Math.max(0, Math.min(3, level - i)));
    }

    public void build() {
        root.removeAllViews();

        LinearLayout graphics = section(R.string.presets_section_graphics);
        if (ok(MSAA)) choice(graphics, R.string.presets_msaa, R.string.presets_msaa_short, labels(R.array.presets_msaa), pick(MSAA_VALUES, MSAA), MSAA, i -> {
            put(MSAA, i == 0 ? null : (Object) MSAA_VALUES[i]);
            put(MSAA_OPTIONAL, null);
            commit.run();
        });
        if (ok(VULKAN)) choice(graphics, R.string.presets_render_mode, R.string.presets_render_mode_short, labels(R.array.presets_render_mode), on(VULKAN) ? 1 : on(OPENGL) ? 2 : 0, null, i -> {
            put(D3D11, null);
            put(VULKAN, i == 1 ? Boolean.TRUE : null);
            put(OPENGL, i == 2 ? Boolean.TRUE : null);
            put(NO_D3D11, null);
            commit.run();
        });
        if (ok(DPI)) toggle(graphics, R.string.presets_dpi_title, R.string.presets_dpi_short, on(DPI), checked -> {
            put(DPI, checked ? Boolean.TRUE : null);
            commit.run();
        });
        if (ok(SHADOWS[0])) toggle(graphics, R.string.presets_shadows, R.string.presets_shadows_short, on(SHADOWS[0]) && values().containsKey(SHADOWS[1]), checked -> {
            shadows(checked);
            commit.run();
        });

        LinearLayout performance = section(R.string.presets_performance);
        toggle(performance, R.string.presets_show_fps, R.string.presets_show_fps_short, on(SHOW_FPS), checked -> {
            put(SHOW_FPS, checked ? Boolean.TRUE : null);
            commit.run();
        });
        defaultOff(performance, R.string.presets_async_textures, R.string.presets_async_textures_short, ASYNC_TEX);
        defaultOff(performance, R.string.presets_mesh_optimizer, R.string.presets_mesh_optimizer_short, MESH_OPT);
        defaultOff(performance, R.string.presets_alpha_prepass, R.string.presets_alpha_prepass_short, ALPHA_PREPASS);

        LinearLayout quality = section(R.string.presets_section_quality);
        defaultOff(quality, R.string.presets_ambient_occlusion, R.string.presets_ambient_occlusion_short, AO);
        defaultOff(quality, R.string.presets_indoor_specular, R.string.presets_indoor_specular_short, INDOOR_SPEC);

        if (ok(CLOUDS) || ok(DROP_HAT) || ok(GOTHAM) || ok(DEATH_SOUNDS) || ok(BONES) || ok(BODY_OUTLINE) || ok(HUMANOID_DEBUG)) {
            LinearLayout fun = section(R.string.presets_section_fun);
            defaultOff(fun, R.string.presets_clouds, R.string.presets_clouds_short, CLOUDS);
            defaultOff(fun, R.string.presets_drop_hat, R.string.presets_drop_hat_short, DROP_HAT);
            defaultOff(fun, R.string.presets_gotham_font, R.string.presets_gotham_font_short, GOTHAM);
            defaultOn(fun, R.string.presets_death_sounds, R.string.presets_death_sounds_short, DEATH_SOUNDS);
            defaultOn(fun, R.string.presets_bones, R.string.presets_bones_short, BONES);
            defaultOff(fun, R.string.presets_body_outline, R.string.presets_body_outline_short, BODY_OUTLINE);
            defaultOff(fun, R.string.presets_humanoid_debug, R.string.presets_humanoid_debug_short, HUMANOID_DEBUG);
        }

        boolean detailAny = ok(LOD[0]) || ok(GRASS_MIN);
        LinearLayout detail = detailAny ? section(R.string.presets_section_detail) : null;
        boolean meshOn = values().containsKey(LOD[0]);
        View meshSlider = slider(MESH_MIN, MESH_MAX, (int) Math.max(MESH_MIN, Math.min(MESH_MAX, num(LOD[0], MESH_MAX))), v -> {
            mesh(v);
            commit.run();
        });
        if (ok(LOD[0])) toggle(detail, R.string.presets_mesh, R.string.presets_mesh_short, meshOn, checked -> {
            mesh(checked ? MESH_MAX : 0);
            commit.run();
            build();
        });
        if (meshOn && ok(LOD[0])) detail.addView(meshSlider);

        if (ok(GRASS_MIN)) toggle(detail, R.string.presets_remove_grass, R.string.presets_remove_grass_short, num(GRASS_MIN, -1) == 0 && num(GRASS_MAX, -1) == 0, checked -> {
            put(GRASS_MIN, checked ? (Object) 0L : null);
            put(GRASS_MAX, checked ? (Object) 0L : null);
            commit.run();
            build();
        });
        if (ok(GRASS_MIN)) grassDistance(detail);

        LinearLayout ui = section(R.string.presets_section_interface);
        toggle(ui, R.string.presets_blur, R.string.presets_blur_short, values().containsKey(BLUR) && num(BLUR, -1) == 0, checked -> {
            put(BLUR, checked ? (Object) 0L : null);
            commit.run();
        });
        input(ui, R.string.presets_font_size, R.string.presets_font_size_short, FONT_PADDING, true, v -> put(FONT_PADDING, v));
        if (ok(HIDE_GUI_GROUP)) input(ui, R.string.presets_hide_gui, R.string.presets_hide_gui_short, HIDE_GUI_GROUP, true, v -> put(HIDE_GUI_GROUP, v));

        if (ok(MTU)) {
            LinearLayout network = section(R.string.presets_section_network);
            input(network, R.string.presets_mtu, R.string.presets_mtu_short, MTU, true, v -> put(MTU, v));
        }

        LinearLayout debug = section(R.string.presets_section_debug);
        input(debug, R.string.presets_flag_state, R.string.presets_flag_state_short, FLAG_STATE, false, v -> put(FLAG_STATE, v));

        LinearLayout quick = section(R.string.presets_section_quick);
        recommended(quick);
        allowlistRow(quick);
    }

    private boolean ok(String flag) {
        return Targets.flagSupported(a, flag);
    }

    private LinearLayout section(int title) {
        return SettingRows.section(root, a.getString(title));
    }

    private LinearLayout row(LinearLayout parent, int title, int summary, View control) {
        return SettingRows.row(parent, a.getString(title), summary == 0 ? null : a.getString(summary), control);
    }

    private interface IntChoice {
        void on(int index);
    }

    private interface BoolChoice {
        void on(boolean checked);
    }

    private void toggle(LinearLayout parent, int title, int summary, boolean checked, BoolChoice change) {
        MaterialSwitch s = new MaterialSwitch(a);
        s.setChecked(checked);
        s.setContentDescription(a.getString(title));
        s.setOnCheckedChangeListener((b, c) -> change.on(c));
        row(parent, title, summary, s).setOnClickListener(v -> s.toggle());
    }

    /** A switch for a flag that ships off. Turning it on writes true; turning it off removes the override. */
    private void defaultOff(LinearLayout parent, int title, int summary, String key) {
        if (!ok(key)) return;
        toggle(parent, title, summary, on(key), checked -> {
            put(key, checked ? Boolean.TRUE : null);
            commit.run();
        });
    }

    /** A switch for a flag that ships on. Turning it off writes false; turning it back on removes the override. */
    private void defaultOn(LinearLayout parent, int title, int summary, String key) {
        if (!ok(key)) return;
        Object v = values().get(key);
        boolean checked = !(Boolean.FALSE.equals(v) || "false".equalsIgnoreCase(String.valueOf(v)));
        toggle(parent, title, summary, checked, c -> {
            put(key, c ? null : Boolean.FALSE);
            commit.run();
        });
    }

    private void choice(LinearLayout parent, int title, int summary, String[] labels, int selected, String key, IntChoice change) {
        int custom = selected < 0 ? labels.length : -1;
        String[] shown = labels;
        if (custom >= 0) {
            shown = java.util.Arrays.copyOf(labels, labels.length + 1);
            shown[custom] = a.getString(R.string.presets_custom, String.valueOf(values().get(key)));
        }
        SettingRows.choice(parent, a.getString(title), summary == 0 ? null : a.getString(summary), shown, custom >= 0 ? custom : selected, i -> {
            if (i == custom) return;
            change.on(i);
            if (custom >= 0) build();
        });
    }

    private View slider(int min, int max, int value, IntChoice change) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), Ui.dp(a, 8));
        TextView low = new TextView(a);
        low.setTextAppearance(R.style.TextAppearance_Voidstrap_Tertiary);
        low.setText(R.string.presets_low);
        TextView high = new TextView(a);
        high.setTextAppearance(R.style.TextAppearance_Voidstrap_Tertiary);
        high.setText(R.string.presets_high);
        Slider s = new Slider(a);
        s.setValueFrom(min);
        s.setValueTo(max);
        s.setStepSize(1);
        s.setTickVisibilityMode(com.google.android.material.slider.TickVisibilityMode.TICK_VISIBILITY_HIDDEN);
        s.setValue(value);
        s.addOnChangeListener((slider, v, fromUser) -> {
            if (fromUser) change.on((int) v);
        });
        box.addView(low);
        box.addView(s, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(high);
        return box;
    }

    private interface ValueChange {
        void on(Object value);
    }

    private void input(LinearLayout parent, int title, int summary, String key, boolean numeric, ValueChange change) {
        EditText field = new EditText(a);
        field.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setSingleLine(true);
        Object current = values().get(key);
        field.setText(current == null ? "" : String.valueOf(current));
        field.setHint(R.string.presets_off);
        field.setGravity(Gravity.CENTER);
        field.setTextAppearance(R.style.TextAppearance_Voidstrap_Body);
        field.setBackgroundResource(R.drawable.vs_icon_tile);
        field.setMinWidth(Ui.dp(a, numeric ? 88 : 140));
        field.setMaxWidth(Ui.dp(a, 220));
        field.setMinHeight(Ui.dp(a, 40));
        field.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        field.setContentDescription(a.getString(title));
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (text.isEmpty()) {
                    change.on(null);
                } else if (numeric) {
                    try {
                        change.on((long) Integer.parseInt(text));
                    } catch (NumberFormatException e) {
                        field.setError(a.getString(R.string.presets_whole_number));
                        return;
                    }
                } else {
                    change.on(text);
                }
                field.setError(null);
                commit.run();
            }
        });
        row(parent, title, summary, field);
    }

    private void grassDistance(LinearLayout parent) {
        LinearLayout fields = new LinearLayout(a);
        fields.setOrientation(LinearLayout.HORIZONTAL);
        fields.setGravity(Gravity.CENTER_VERTICAL);
        fields.addView(number(R.string.presets_grass_min, 100, GRASS_MIN));
        TextView dash = new TextView(a);
        dash.setTextAppearance(R.style.TextAppearance_Voidstrap_Tertiary);
        dash.setText(R.string.presets_grass_to);
        dash.setPadding(Ui.dp(a, 6), 0, Ui.dp(a, 6), 0);
        fields.addView(dash);
        fields.addView(number(R.string.presets_grass_max, 290, GRASS_MAX));
        row(parent, R.string.presets_grass_distance, R.string.presets_grass_distance_short, fields);
    }

    private View number(int label, long fallback, String key) {
        EditText field = new EditText(a);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setSingleLine(true);
        field.setText(values().containsKey(key) ? String.valueOf(num(key, fallback)) : "");
        field.setHint(String.valueOf(fallback));
        field.setGravity(Gravity.CENTER);
        field.setTextAppearance(R.style.TextAppearance_Voidstrap_Body);
        field.setBackgroundResource(R.drawable.vs_icon_tile);
        field.setMinWidth(Ui.dp(a, 64));
        field.setMinHeight(Ui.dp(a, 40));
        field.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        field.setContentDescription(a.getString(label));
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                try {
                    put(key, text.isEmpty() ? null : (Object) (long) Integer.parseInt(text));
                    field.setError(null);
                    commit.run();
                } catch (NumberFormatException e) {
                    field.setError(a.getString(R.string.presets_whole_number));
                }
            }
        });
        return field;
    }

    private void recommended(LinearLayout parent) {
        String[] names = labels(R.array.presets_profiles);
        Dropdown pick = new Dropdown(a, names, profile, i -> profile = i);
        MaterialButton apply = new MaterialButton(a);
        apply.setText(R.string.presets_apply_profile);
        apply.setOnClickListener(v -> {
            if (ok(MSAA)) {
                put(MSAA, PROFILES[profile][0] == 0 ? null : (Object) PROFILES[profile][0]);
                put(MSAA_OPTIONAL, null);
            }
            if (ok(SHADOWS[0])) shadows(PROFILES[profile][1] == 1);
            if (ok(LOD[0])) mesh((int) PROFILES[profile][2]);
            commit.run();
            Notify.say(a, Notify.FLAGS, a.getString(R.string.presets_profile_applied, names[profile]));
            build();
        });
        LinearLayout controls = new LinearLayout(a);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(pick.view);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.setMarginStart(Ui.dp(a, 4));
        controls.addView(apply, ap);
        row(parent, R.string.presets_recommended_title, R.string.presets_recommended_short, controls);
    }

    private void allowlistRow(LinearLayout parent) {
        android.widget.ImageView chevron = new android.widget.ImageView(a);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.attr(a, R.attr.vsTextTertiary)));
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout row = row(parent, R.string.presets_allowlist, R.string.presets_allowlist_short, chevron);
        row.setOnClickListener(v -> {
            LinearLayout list = new LinearLayout(a);
            list.setOrientation(LinearLayout.VERTICAL);
            com.google.android.material.button.MaterialButton request = new com.google.android.material.button.MaterialButton(a);
            request.setText(R.string.presets_request_flag);
            request.setOnClickListener(x -> requestFlag());
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, Math.round(12 * a.getResources().getDisplayMetrics().density));
            list.addView(request, rp);

            com.google.android.material.textfield.TextInputLayout search =
                    new com.google.android.material.textfield.TextInputLayout(new android.view.ContextThemeWrapper(a,
                            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox));
            search.setHint(a.getString(R.string.flag_search_hint));
            search.setStartIconDrawable(R.drawable.ic_search);
            com.google.android.material.textfield.TextInputEditText field = new com.google.android.material.textfield.TextInputEditText(search.getContext());
            field.setSingleLine(true);
            search.addView(field);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sp.setMargins(0, 0, 0, Math.round(8 * a.getResources().getDisplayMetrics().density));
            list.addView(search, sp);
            // Categories come from the data so new ones show up without touching this code.
            java.util.Set<String> found = new java.util.TreeSet<>();
            List<Map.Entry<String, String>> all = new ArrayList<>();
            for (Map.Entry<String, String> e : FlagWriter.ALLOWLIST.entrySet()) {
                if (!ok(e.getKey())) continue;
                all.add(e);
                found.add(e.getValue());
            }
            List<String> order = new ArrayList<>();
            for (String c : CATEGORY_ORDER) if (found.remove(c)) order.add(c);
            order.addAll(found);

            // The list can hold thousands of flags, so it is recycled in a RecyclerView with a fixed height
            // instead of inflating every row up front inside the scrolling sheet.
            RecyclerView rv = new RecyclerView(a);
            rv.setLayoutManager(new LinearLayoutManager(a));
            AllowAdapter adapter = new AllowAdapter(all, order);
            rv.setAdapter(adapter);
            field.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence t, int st, int c, int af) {}
                @Override public void onTextChanged(CharSequence t, int st, int b, int c) { adapter.setQuery(t.toString()); }
                @Override public void afterTextChanged(android.text.Editable e) {}
            });
            // The list is the only scroller here. The sheet's NestedScrollView would otherwise take drags
            // before the list sees them, so nested scrolling is off and the parent is told to stay out.
            rv.setNestedScrollingEnabled(false);
            rv.setOnTouchListener((touched, event) -> {
                if (touched.getParent() != null) touched.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });

            com.google.android.material.floatingactionbutton.FloatingActionButton top =
                    new com.google.android.material.floatingactionbutton.FloatingActionButton(a);
            top.setSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            top.setImageResource(R.drawable.ic_chevron_up);
            top.setContentDescription(a.getString(R.string.presets_scroll_top));
            top.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.attr(a, com.google.android.material.R.attr.colorSurfaceContainerHigh)));
            top.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.attr(a, R.attr.vsTextPrimary)));
            top.hide();
            top.setOnClickListener(x -> rv.smoothScrollToPosition(0));
            rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView r, int dx, int dy) {
                    LinearLayoutManager lm = (LinearLayoutManager) r.getLayoutManager();
                    if (lm == null) return;
                    if (lm.findFirstVisibleItemPosition() > 0) top.show();
                    else top.hide();
                }
            });

            android.widget.FrameLayout box = new android.widget.FrameLayout(a);
            box.addView(rv, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (a.getResources().getDisplayMetrics().heightPixels * 0.55f)));
            android.widget.FrameLayout.LayoutParams tp = new android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.BOTTOM | android.view.Gravity.END);
            int m = Ui.dp(a, 12);
            tp.setMargins(m, m, m, m);
            box.addView(top, tp);
            list.addView(box);
            Ui.surface(a, R.string.presets_allowlist, R.string.presets_allowlist_body, list).show();
        });
    }

    private static final String[] CATEGORY_ORDER = {
            "Rendering", "Geometry", "Physics", "Performance", "Network", "Audio", "User Interface", "Scripting", "Debug", "Other"};

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FLAG = 1;

    /**
     * Flags grouped under collapsible category headers. Headers start closed; tapping one inserts or
     * removes its flag rows with range notifications so the default item animator slides them in and out.
     */
    private final class AllowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Map<String, List<Map.Entry<String, String>>> groups = new java.util.LinkedHashMap<>();
        private final java.util.Set<String> open = new java.util.HashSet<>();
        /** Each element is either a category name (header) or a flag entry. */
        private final List<Object> visible = new ArrayList<>();
        private String query = "";

        AllowAdapter(List<Map.Entry<String, String>> all, List<String> order) {
            for (String c : order) groups.put(c, new ArrayList<>());
            for (Map.Entry<String, String> e : all) {
                List<Map.Entry<String, String>> g = groups.get(e.getValue());
                if (g != null) g.add(e);
            }
            rebuild();
        }

        /** Searching opens every category that has matches and locks the headers until the box is cleared. */
        void setQuery(String text) {
            query = text.trim().toLowerCase(java.util.Locale.ROOT);
            rebuild();
            notifyDataSetChanged();
        }

        private List<Map.Entry<String, String>> matches(String category) {
            List<Map.Entry<String, String>> out = new ArrayList<>();
            for (Map.Entry<String, String> e : groups.get(category)) {
                if (query.isEmpty() || e.getKey().toLowerCase(java.util.Locale.ROOT).contains(query)) out.add(e);
            }
            return out;
        }

        private void rebuild() {
            visible.clear();
            for (String c : groups.keySet()) {
                List<Map.Entry<String, String>> m = matches(c);
                if (!query.isEmpty() && m.isEmpty()) continue;
                visible.add(c);
                if (!query.isEmpty() || open.contains(c)) visible.addAll(m);
            }
        }

        private void toggle(String category) {
            if (!query.isEmpty()) return;
            int p = visible.indexOf(category);
            if (p < 0) return;
            int n = groups.get(category).size();
            if (open.remove(category)) {
                visible.subList(p + 1, p + 1 + n).clear();
                notifyItemRangeRemoved(p + 1, n);
            } else {
                open.add(category);
                visible.addAll(p + 1, groups.get(category));
                notifyItemRangeInserted(p + 1, n);
            }
            notifyItemChanged(p);
        }

        final class HeaderHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView count;
            final ImageView chevron;

            HeaderHolder(View v) {
                super(v);
                name = v.findViewById(R.id.category_name);
                count = v.findViewById(R.id.category_count);
                chevron = v.findViewById(R.id.category_chevron);
            }
        }

        final class FlagHolder extends RecyclerView.ViewHolder {
            final Row row;

            FlagHolder(Row row) {
                super(row.view);
                this.row = row;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return visible.get(position) instanceof String ? TYPE_HEADER : TYPE_FLAG;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                return new HeaderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_header, parent, false));
            }
            return new FlagHolder(Row.inflate(parent));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                HeaderHolder h = (HeaderHolder) holder;
                String category = (String) visible.get(position);
                h.name.setText(category);
                h.count.setText(String.valueOf(matches(category).size()));
                boolean isOpen = open.contains(category);
                h.chevron.setImageResource(isOpen ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right);
                h.itemView.setOnClickListener(x -> toggle(category));
                return;
            }
            Map.Entry<String, String> e = (Map.Entry<String, String>) visible.get(position);
            FlagHolder f = (FlagHolder) holder;
            f.row.set(R.drawable.ic_flag, e.getKey(), e.getValue());
            f.row.view.setOnClickListener(x -> FlagInfo.show(a, e.getKey(), values().get(e.getKey()), null));
        }

        @Override
        public int getItemCount() {
            return visible.size();
        }
    }

    /** Opens a form for a missing flag. Submitting opens a prefilled GitHub issue for review; nothing is sent by the app. */
    private void requestFlag() {
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * d);
        form.setPadding(pad, Math.round(8 * d), pad, 0);
        android.widget.EditText name = new android.widget.EditText(a);
        name.setHint(R.string.presets_request_name);
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        android.widget.EditText value = new android.widget.EditText(a);
        value.setHint(R.string.presets_request_value);
        value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        android.widget.EditText reason = new android.widget.EditText(a);
        reason.setHint(R.string.presets_request_reason);
        reason.setMinLines(3);
        form.addView(name);
        form.addView(value);
        form.addView(reason);
        android.widget.TextView groupLabel = new android.widget.TextView(a);
        groupLabel.setText(R.string.flag_group_label);
        groupLabel.setPadding(0, Math.round(12 * d), 0, 0);
        android.widget.Spinner groupSpinner = new android.widget.Spinner(a);
        groupSpinner.setAdapter(new android.widget.ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item, a.getResources().getStringArray(R.array.flag_groups)));
        android.widget.TextView setup = new android.widget.TextView(a);
        setup.setText(R.string.flag_env_heading);
        setup.setPadding(0, Math.round(12 * d), 0, 0);
        android.widget.TextView env = new android.widget.TextView(a);
        env.setText(FlagRequests.summary(a));
        form.addView(groupLabel);
        form.addView(groupSpinner);
        form.addView(setup);
        form.addView(env);
        androidx.appcompat.app.AlertDialog dialog = Ui.alert(a)
                .setTitle(R.string.presets_request_title)
                .setMessage(R.string.presets_request_body)
                .setView(form)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.presets_request_open, null)
                .show();
        android.widget.Button send = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        send.setOnClickListener(x -> {
            String flag = name.getText().toString().trim();
            if (!flag.matches("[A-Za-z0-9_]{3,100}")) {
                Notify.say(a, Notify.GENERAL, R.string.presets_request_invalid);
                return;
            }
            if (!FlagRequests.configured()) {
                Notify.say(a, Notify.GENERAL, R.string.flag_request_unavailable);
                return;
            }
            String valueText = value.getText().toString().trim();
            String reasonText = reason.getText().toString().trim();
            String groupText = String.valueOf(groupSpinner.getSelectedItem());
            send.setEnabled(false);
            Notify.say(a, Notify.GENERAL, R.string.flag_request_sending);
            store.work.execute(() -> {
                int message;
                try {
                    FlagRequests.send(a, flag, valueText, reasonText, groupText);
                    message = R.string.flag_request_sent;
                } catch (java.io.IOException | RuntimeException e) {
                    message = R.string.flag_request_failed;
                }
                int shown = message;
                store.main.post(() -> {
                    send.setEnabled(true);
                    Notify.say(a, Notify.GENERAL, shown);
                    if (shown == R.string.flag_request_sent) dialog.dismiss();
                });
            });
        });
    }
}
