package com.lynko.keyboard;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LynkoIME extends InputMethodService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences p;
    boolean shift, sym, si;

    static final String[] EN  = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    static final String[] SYM = {"1234567890", "@#$%&-+()/", "*\"':;!?~_="};
    static final String[] SIN = {"අආඇඈඉඊඋඌඍඑ", "ඒඓඔඕඖකඛගඝඞ", "චඡජඣඤටඨඩඪණ",
            "තථදධනපඵබභම", "යරලවශෂසහළෆ", "ාැෑිීුූෙේො", "ෝෞෛෘ්ංඃ"};

    @Override public void onCreate() {
        super.onCreate();
        p = getSharedPreferences("lynko", MODE_PRIVATE);
        p.registerOnSharedPreferenceChangeListener(this);
    }

    @Override public void onDestroy() {
        p.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    // settings / theme වෙනස් කළ ගමන් real-time update
    @Override public void onSharedPreferenceChanged(SharedPreferences s, String k) { setInputView(build()); }
    @Override public void onConfigurationChanged(Configuration c) { super.onConfigurationChanged(c); setInputView(build()); }
    @Override public View onCreateInputView() { return build(); }

    int dp(int x) { return (int) (x * getResources().getDisplayMetrics().density + 0.5f); }
    int a(int c, int al) { return (c & 0xFFFFFF) | (al << 24); }

    int[] theme() { // bg, key, text, accent
        int t = p.getInt("theme", 0);
        if (t == 0) {
            boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            t = night ? 1 : 2;
        }
        switch (t) {
            case 2: return new int[]{0xE8EAF0, 0xFFFFFF, 0x111111, 0x5B5BD6};
            case 3: return new int[]{0x2A1B4D, 0x4B3380, 0xFFFFFF, 0xFF6FD8};
            case 4: return new int[]{0x000000, 0x0B1F1A, 0x00FFC8, 0xFF2E93};
            default: return new int[]{0x1E1E2E, 0x313244, 0xFFFFFF, 0x7C4DFF};
        }
    }

    View build() {
        int[] t = theme();
        int al = p.getInt("alpha", 255);
        int h = dp(p.getInt("height", 46));
        boolean sin = si && !sym;
        if (sin) h = h * 7 / 10;

        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(a(t[0], al));
        r.setPadding(dp(4), dp(4), dp(4), dp(4));

        // Toolbar: cut, copy, paste, select all, cursor, clear clipboard
        LinearLayout tb = row(r);
        int th = h * 3 / 4;
        key(tb, "✂", 1, th, t, false, () -> ctx(android.R.id.cut));
        key(tb, "⧉", 1, th, t, false, () -> ctx(android.R.id.copy));
        key(tb, "📋", 1, th, t, false, () -> {
            ctx(android.R.id.paste);
            if (p.getBoolean("privacy", false)) clearClip();
        });
        key(tb, "A*", 1, th, t, false, () -> ctx(android.R.id.selectAll));
        key(tb, "◀", 1, th, t, false, () -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT));
        key(tb, "▶", 1, th, t, false, () -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT));
        key(tb, "🗑", 1, th, t, false, this::clearClip);

        if (p.getBoolean("numrow", true) && !sym && !sin) letters(r, "1234567890", h, t);

        if (sym) {
            for (String s : SYM) letters(r, s, h, t);
        } else if (sin) {
            for (int i = 0; i < SIN.length; i++) {
                LinearLayout l = row(r);
                for (char c : SIN[i].toCharArray()) key(l, String.valueOf(c), 1, h, t, false, () -> type(String.valueOf(c)));
                if (i == SIN.length - 1) key(l, "⌫", 1.5f, h, t, true, this::del);
            }
        } else {
            letters(r, EN[0], h, t);
            LinearLayout l2 = row(r); spacer(l2, 0.5f);
            fill(l2, EN[1], h, t); spacer(l2, 0.5f);
            LinearLayout l3 = row(r);
            key(l3, shift ? "⇪" : "⇧", 1.5f, h, t, true, () -> { shift = !shift; setInputView(build()); });
            fill(l3, EN[2], h, t);
            key(l3, "⌫", 1.5f, h, t, true, this::del);
        }

        LinearLayout b = row(r);
        key(b, sym ? "ABC" : "?123", 1.5f, h, t, true, () -> { sym = !sym; setInputView(build()); });
        key(b, si ? "EN" : "සිං", 1.2f, h, t, true, () -> { si = !si; sym = false; setInputView(build()); });
        key(b, ",", 1, h, t, false, () -> type(","));
        key(b, "space", 4, h, t, false, () -> type(" "));
        key(b, ".", 1, h, t, false, () -> type("."));
        key(b, "⏎", 1.5f, h, t, true, this::enter);

        int oh = p.getInt("onehand", 0); // 0 off, 1 left, 2 right
        if (oh == 0) return r;
        FrameLayout f = new FrameLayout(this);
        int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.82f);
        f.addView(r, new FrameLayout.LayoutParams(w, -2, oh == 1 ? Gravity.START : Gravity.END));
        return f;
    }

    LinearLayout row(LinearLayout parent) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(l, new LinearLayout.LayoutParams(-1, -2));
        return l;
    }

    void letters(LinearLayout r, String s, int h, int[] t) { fill(row(r), s, h, t); }

    void fill(LinearLayout l, String s, int h, int[] t) {
        for (char c : s.toCharArray()) {
            String lab = (shift && !sym) ? String.valueOf(c).toUpperCase() : String.valueOf(c);
            key(l, lab, 1, h, t, false, () -> {
                type(lab);
                if (shift) { shift = false; setInputView(build()); }
            });
        }
    }

    void spacer(LinearLayout l, float w) { l.addView(new View(this), new LinearLayout.LayoutParams(0, 1, w)); }

    void key(LinearLayout l, String label, float w, int h, int[] t, boolean accent, Runnable act) {
        int al = p.getInt("alpha", 255);
        TextView v = new TextView(this);
        v.setText(label);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(a(t[2], 255));
        v.setTextSize(p.getInt("textsize", 18));
        v.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(a(accent ? t[3] : t[1], al));
        g.setCornerRadius(dp(p.getInt("radius", 8)));
        v.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, w);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        v.setOnClickListener(x -> { feedback(x); act.run(); });
        l.addView(v, lp);
    }

    void feedback(View v) {
        if (p.getBoolean("vibrate", true)) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (p.getBoolean("sound", false))
            ((AudioManager) getSystemService(AUDIO_SERVICE)).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
    }

    void type(String s) { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.commitText(s, 1); }
    void del() { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL); }
    void ctx(int id) { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.performContextMenuAction(id); }

    void clearClip() {
        try { ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).clearPrimaryClip(); } catch (Exception ignored) {}
    }

    void enter() {
        EditorInfo ei = getCurrentInputEditorInfo();
        int act = ei == null ? 0 : ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && act != EditorInfo.IME_ACTION_NONE && act != EditorInfo.IME_ACTION_UNSPECIFIED)
            sendDefaultEditorAction(true);
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
    }
}
