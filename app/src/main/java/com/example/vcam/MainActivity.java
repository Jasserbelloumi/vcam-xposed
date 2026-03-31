package com.example.vcam;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private static final String BASE =
        Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/";

    private Switch swDisable, swNoToast;
    private Switch swFlipH, swFlipV;
    private Switch swRot90, swRot180, swRot270;
    private Switch swLeft, swRight, swUp, swDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new File(BASE).mkdirs();

        ScrollView sv = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(32, 32, 32, 32);

        addTitle(ll, "VCAM - Virtual Camera");

        // --- أساسي ---
        addHeader(ll, "الإعدادات الأساسية");
        swDisable = addSwitch(ll, "تعطيل VCAM",      "disable.jpg");
        swNoToast = addSwitch(ll, "إخفاء الإشعارات", "no_toast.jpg");

        // --- قلب ---
        addHeader(ll, "قلب الصورة");
        swFlipH = addSwitch(ll, "قلب أفقي  ↔  (يمين / يسار)", "flip_h.cfg");
        swFlipV = addSwitch(ll, "قلب عمودي ↕  (أعلى / أسفل)", "flip_v.cfg");

        // --- تدوير ---
        addHeader(ll, "تدوير الصورة");
        swRot90  = addSwitch(ll, "تدوير 90°",  "rotate_90.cfg");
        swRot180 = addSwitch(ll, "تدوير 180°", "rotate_180.cfg");
        swRot270 = addSwitch(ll, "تدوير 270°", "rotate_270.cfg");

        // --- تحريك ---
        addHeader(ll, "تحريك الصورة");
        swLeft  = addSwitch(ll, "تحريك ← يسار",  "move_left.cfg");
        swRight = addSwitch(ll, "تحريك → يمين",   "move_right.cfg");
        swUp    = addSwitch(ll, "تحريك ↑ أعلى",   "move_up.cfg");
        swDown  = addSwitch(ll, "تحريك ↓ أسفل",   "move_down.cfg");

        // --- زر إعادة ضبط ---
        Button btnReset = new Button(this);
        btnReset.setText("إعادة ضبط جميع التحويلات");
        btnReset.setOnClickListener(v -> resetAll());
        ll.addView(btnReset);

        // --- مسار ---
        TextView tvPath = new TextView(this);
        tvPath.setText("\nمسار الملفات:\n" + BASE);
        tvPath.setTextSize(11f);
        ll.addView(tvPath);

        sv.addView(ll);
        setContentView(sv);

        if (!hasPerm()) requestPerm();
        syncSwitches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSwitches();
    }

    // ==================== مساعدات UI ====================

    private void addTitle(LinearLayout ll, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(22f);
        ll.addView(tv);
    }

    private void addHeader(LinearLayout ll, String text) {
        TextView tv = new TextView(this);
        tv.setText("\n" + text);
        tv.setTextSize(16f);
        ll.addView(tv);
    }

    private Switch addSwitch(LinearLayout ll, String label, String fileName) {
        TextView tv = new TextView(this);
        tv.setText(label);
        ll.addView(tv);

        Switch sw = new Switch(this);
        sw.setChecked(new File(BASE + fileName).exists());
        sw.setOnCheckedChangeListener((btn, checked) -> {
            File f = new File(BASE + fileName);
            try {
                if (checked) f.createNewFile();
                else f.delete();
                Toast.makeText(this,
                    label + ": " + (checked ? "مفعّل" : "معطّل"),
                    Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "خطأ: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
        ll.addView(sw);
        return sw;
    }

    // ==================== مزامنة ومنطق ====================

    private void syncSwitches() {
        if (swDisable != null) swDisable.setChecked(new File(BASE + "disable.jpg").exists());
        if (swNoToast != null) swNoToast.setChecked(new File(BASE + "no_toast.jpg").exists());
        if (swFlipH   != null) swFlipH.setChecked(new File(BASE + "flip_h.cfg").exists());
        if (swFlipV   != null) swFlipV.setChecked(new File(BASE + "flip_v.cfg").exists());
        if (swRot90   != null) swRot90.setChecked(new File(BASE + "rotate_90.cfg").exists());
        if (swRot180  != null) swRot180.setChecked(new File(BASE + "rotate_180.cfg").exists());
        if (swRot270  != null) swRot270.setChecked(new File(BASE + "rotate_270.cfg").exists());
        if (swLeft    != null) swLeft.setChecked(new File(BASE + "move_left.cfg").exists());
        if (swRight   != null) swRight.setChecked(new File(BASE + "move_right.cfg").exists());
        if (swUp      != null) swUp.setChecked(new File(BASE + "move_up.cfg").exists());
        if (swDown    != null) swDown.setChecked(new File(BASE + "move_down.cfg").exists());
    }

    private void resetAll() {
        String[] files = {
            "flip_h.cfg","flip_v.cfg",
            "rotate_90.cfg","rotate_180.cfg","rotate_270.cfg",
            "move_left.cfg","move_right.cfg","move_up.cfg","move_down.cfg"
        };
        for (String f : files) new File(BASE + f).delete();
        syncSwitches();
        Toast.makeText(this, "تم إعادة الضبط", Toast.LENGTH_SHORT).show();
    }

    private boolean hasPerm() {
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                       == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                       == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPerm() {
        if (Build.VERSION.SDK_INT >= 23) {
            new AlertDialog.Builder(this)
                .setTitle("صلاحية التخزين")
                .setMessage("يحتاج VCAM إلى صلاحية الوصول للتخزين")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("منح", (d, w) ->
                    requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 100))
                .show();
        }
    }
}
