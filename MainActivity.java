package com.lynko.keyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class MainActivity extends Activity {
    SharedPreferences p;
    FrameLayout host;
    int cur = 0;
    static final String[] THEMES = {"Auto", "Dark", "Light", "Purple", "Neon"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = getSharedPreferences("lynko", MODE_PRIVATE);
        host = new FrameLayout(this);
        host.setBackgroundColor(0xFF12121C);
        setContentView(host);
        show(p.getBoolean("seen", false) ? 2 : 0);
    }

    @Override public void onBackPressed() { if (cur != 2) show(2); else super.onBackPressed(); }

    int dp(int x) { return (int) (x * getResources().getDisplayMetrics().density + 0.5f); }

    TextView tv(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    Button btn(String s, Runnable r) {
        Button x = new Button(this);
        x.setText(s); x.setAllCaps(false);
        x.setOnClickListener(v -> r.run());
        return x;
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    void show(int s) {
        cur = s;
        host.removeAllViews();
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(36), dp(20), dp(20));
        ScrollView sv = new ScrollView(this);
        sv.addView(c);
        host.addView(sv);
        if (s >= 2) nav(c);
        switch (s) {
            case 0: welcome(c); break;
            case 1: setup(c); break;
            case 2: home(c); break;
            case 3: preview(c); break;
            case 4: settings(c); break;
            default: about(c);
        }
    }

    void nav(LinearLayout c) {
        LinearLayout n = new LinearLayout(this);
        n.addView(btn("Home", () -> show(2)), new LinearLayout.LayoutParams(0, -2, 1));
        n.addView(btn("Preview", () -> show(3)), new LinearLayout.LayoutParams(0, -2, 1));
        n.addView(btn("Settings", () -> show(4)), new LinearLayout.LayoutParams(0, -2, 1));
        n.addView(btn("About", () -> show(5)), new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(n);
    }

    // 1. Welcome
    void welcome(LinearLayout c) {
        c.setGravity(Gravity.CENTER);
        TextView logo = tv("LYNKO", 52, 0xFF9C7CFF, true);
        logo.setGravity(Gravity.CENTER);
        logo.setAlpha(0f); logo.setScaleX(0.4f); logo.setScaleY(0.4f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1000).start();
        c.addView(logo);
        TextView sub = tv("Type your style.", 16, 0xFFCCCCCC, false);
        sub.setGravity(Gravity.CENTER);
        c.addView(sub);
        c.addView(btn("Get started", () -> { p.edit().putBoolean("seen", true).apply(); show(1); }));
    }

    // 2. Setup
    void setup(LinearLayout c) {
        c.addView(tv("Setup", 28, 0xFFFFFFFF, true));
        boolean on = enabled();
        c.addView(tv(on ? "✅ LYNKO enabled" : "❌ LYNKO not enabled yet", 16, 0xFFCCCCCC, false));
        c.addView(tv("1. Enable LYNKO Keyboard in the list.", 15, 0xFFFFFFFF, false));
        c.addView(btn("Open keyboard settings", () -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        c.addView(tv("2. Choose LYNKO as the default keyboard.", 15, 0xFFFFFFFF, false));
        c.addView(btn("Select keyboard", () ->
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker()));
        c.addView(btn("Done", () -> show(2)));
    }

    boolean enabled() {
        String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS);
        return s != null && s.contains(getPackageName());
    }

    // 3. Home dashboard
    void home(LinearLayout c) {
        c.addView(tv("LYNKO", 32, 0xFF9C7CFF, true));
        c.addView(tv("Themes", 18, 0xFFFFFFFF, true));
        themeButtons(c);
        c.addView(btn("Save current look", () -> {
            String e = p.getInt("theme", 0) + "," + p.getInt("alpha", 255) + "," + p.getInt("radius", 8) + "," + p.getInt("height", 46);
            String old = p.getString("saved", "");
            p.edit().putString("saved", old.isEmpty() ? e : old + "|" + e).apply();
            show(2);
        }));
        String saved = p.getString("saved", "");
        if (!saved.isEmpty()) {
            c.addView(tv("Saved themes", 16, 0xFFFFFFFF, true));
            for (String e : saved.split("\\|")) {
                String[] f = e.split(",");
                c.addView(btn("Apply: " + THEMES[Integer.parseInt(f[0])] + " · " + f[1] + " α", () ->
                        p.edit().putInt("theme", Integer.parseInt(f[0])).putInt("alpha", Integer.parseInt(f[1]))
                                .putInt("radius", Integer.parseInt(f[2])).putInt("height", Integer.parseInt(f[3])).apply()));
            }
        }
        c.addView(tv("Stylish text", 18, 0xFFFFFFFF, true));
        EditText in = new EditText(this);
        in.setHint("Type here"); in.setTextColor(0xFFFFFFFF); in.setHintTextColor(0xFF888888);
        TextView out = tv("", 20, 0xFF9C7CFF, false);
        c.addView(in);
        c.addView(btn("Convert & copy", () -> {
            StringBuilder sb = new StringBuilder();
            for (char ch : in.getText().toString().toCharArray()) {
                if (ch >= 'A' && ch <= 'Z') sb.appendCodePoint(0x1D4D0 + ch - 'A');
                else if (ch >= 'a' && ch <= 'z') sb.appendCodePoint(0x1D4EA + ch - 'a');
                else sb.append(ch);
            }
            out.setText(sb);
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("lynko", sb));
            toast("Copied");
        }));
        c.addView(out);
        c.addView(btn("Clear clipboard", () -> {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).clearPrimaryClip();
            toast("Clipboard cleared");
        }));
        c.addView(btn("Setup guide", () -> show(1)));
    }

    void themeButtons(LinearLayout c) {
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < THEMES.length; i++) {
            int k = i;
            Button x = btn(THEMES[i], () -> p.edit().putInt("theme", k).apply());
            x.setTextSize(11);
            row.addView(x, new LinearLayout.LayoutParams(0, -2, 1));
        }
        c.addView(row);
    }

    // 4. Live preview / keyboard test
    void preview(LinearLayout c) {
        c.addView(tv("Live preview", 24, 0xFFFFFFFF, true));
        c.addView(tv("Tap the box to open the keyboard, then change the theme.", 14, 0xFFCCCCCC, false));
        EditText e = new EditText(this);
        e.setHint("Test typing here (EN / සිං)"); e.setTextColor(0xFFFFFFFF); e.setHintTextColor(0xFF888888);
        e.setMinLines(4); e.setGravity(Gravity.TOP);
        c.addView(e);
        themeButtons(c);
        seek(c, "Transparency", "alpha", 60, 255, 255);
        seek(c, "Key roundness", "radius", 0, 24, 8);
    }

    // 5. Settings
    void settings(LinearLayout c) {
        c.addView(tv("Keyboard settings", 24, 0xFFFFFFFF, true));
        seek(c, "Keyboard height", "height", 36, 64, 46);
        seek(c, "Key text size", "textsize", 14, 26, 18);
        seek(c, "Key transparency", "alpha", 60, 255, 255);
        seek(c, "Key roundness", "radius", 0, 24, 8);
        sw(c, "Vibration", "vibrate", true);
        sw(c, "Key sound", "sound", false);
        sw(c, "Number row", "numrow", true);
        sw(c, "Privacy mode (clear clipboard after paste)", "privacy", false);
        String[] oh = {"Off", "Left", "Right"};
        Button o = btn("One-handed mode: " + oh[p.getInt("onehand", 0)], () -> {
            p.edit().putInt("onehand", (p.getInt("onehand", 0) + 1) % 3).apply();
            show(4);
        });
        c.addView(o);
        c.addView(btn("Backup settings (copy)", () -> {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, ?> en : p.getAll().entrySet())
                sb.append(en.getKey()).append('=').append(en.getValue()).append('\n');
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("lynko", sb));
            toast("Backup copied");
        }));
        c.addView(btn("Restore settings (from clipboard)", () -> {
            try {
                ClipData d = ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).getPrimaryClip();
                String s = d.getItemAt(0).getText().toString();
                SharedPreferences.Editor ed = p.edit();
                for (String line : s.split("\n")) {
                    String[] kv = line.split("=", 2);
                    if (kv.length < 2) continue;
                    if (kv[1].equals("true") || kv[1].equals("false")) ed.putBoolean(kv[0], Boolean.parseBoolean(kv[1]));
                    else if (kv[1].matches("-?\\d+")) ed.putInt(kv[0], Integer.parseInt(kv[1]));
                    else ed.putString(kv[0], kv[1]);
                }
                ed.apply();
                show(4);
                toast("Restored");
            } catch (Exception ex) { toast("Nothing to restore"); }
        }));
    }

    // 6. About & privacy
    void about(LinearLayout c) {
        c.addView(tv("About LYNKO", 24, 0xFFFFFFFF, true));
        c.addView(tv("Version 1.0", 16, 0xFFCCCCCC, false));
        c.addView(tv("Privacy: LYNKO works fully offline. It has no internet permission and never sends "
                + "anything you type anywhere. Settings stay on your phone.", 15, 0xFFFFFFFF, false));
        c.addView(tv("Support: open an issue on the project's GitHub page.", 15, 0xFFFFFFFF, false));
    }

    void seek(LinearLayout c, String label, String key, int min, int max, int def) {
        TextView t = tv(label + ": " + p.getInt(key, def), 14, 0xFFCCCCCC, false);
        c.addView(t);
        SeekBar s = new SeekBar(this);
        s.setMax(max - min);
        s.setProgress(p.getInt(key, def) - min);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int v, boolean user) {
                if (user) { p.edit().putInt(key, v + min).apply(); t.setText(label + ": " + (v + min)); }
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        c.addView(s);
    }

    void sw(LinearLayout c, String label, String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(label); s.setTextColor(0xFFFFFFFF);
        s.setChecked(p.getBoolean(key, def));
        s.setOnCheckedChangeListener((b, on) -> p.edit().putBoolean(key, on).apply());
        s.setPadding(0, dp(8), 0, dp(8));
        c.addView(s);
    }
}
