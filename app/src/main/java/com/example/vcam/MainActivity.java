package com.example.vcam;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class MainActivity extends Activity {

    private static final String VIDEO_PATH = "/storage/emulated/0/DCIM/Camera1/";

    private Switch sw_disable;
    private Switch sw_toast;
    private Switch sw_sound;
    private Switch sw_force_show;
    private Switch sw_private_dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إنشاء المجلد إن لم يكن موجوداً
        new File(VIDEO_PATH).mkdirs();

        buildUI();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSwitches();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        // عنوان
        TextView title = new TextView(this);
        title.setText("VCAM - Virtual Camera");
        title.setTextSize(24f);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("المجلد: " + VIDEO_PATH + "\nضع صورة أو فيديو (virtual.mp4) في هذا المجلد");
        info.setPadding(0, 0, 0, 24);
        root.addView(info);

        // --- مفاتيح التحكم ---
        sw_disable = addSwitch(root, "تعطيل VCAM",
                "عند التفعيل، تظهر الكاميرا الحقيقية", "disable.jpg");

        sw_toast = addSwitch(root, "إخفاء إشعارات Toast",
                "عند التفعيل، لا تظهر رسائل الإشعار", "no_toast.jpg");

        sw_sound = addSwitch(root, "تشغيل صوت الفيديو",
                "عند التفعيل، يُشغَّل صوت الفيديو مع الكاميرا", "no-silent.jpg");

        sw_force_show = addSwitch(root, "إظهار إجباري",
                "يجبر VCAM على العمل حتى لو لم يكتشف الكاميرا", "force_show.jpg");

        sw_private_dir = addSwitch(root, "مجلد خاص",
                "استخدام مجلد خاص بالتطبيق المستهدف", "private_dir.jpg");

        // --- أزرار إضافية ---
        addSeparator(root);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("إظهار نافذة التحكم العائمة");
        btnOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                        .setTitle("صلاحية مطلوبة")
                        .setMessage("يجب منح صلاحية 'العرض فوق التطبيقات' ليعمل زر التحكم العائم")
                        .setPositiveButton("الإعدادات", (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            } else {
                startService(new Intent(this, FloatingControlService.class));
                Toast.makeText(this, "تم تشغيل نافذة التحكم", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnOverlay);

        addSeparator(root);

        Button btnHelp = new Button(this);
        btnHelp.setText("كيفية الاستخدام");
        btnHelp.setOnClickListener(v -> showHelp());
        root.addView(btnHelp);
    }

    private Switch addSwitch(LinearLayout parent, String label, String desc, String fileName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 8, 0, 8);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(16f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(tv);

        Switch sw = new Switch(this);
        sw.setOnCheckedChangeListener((v, checked) -> {
            File f = new File(VIDEO_PATH + fileName);
            try {
                if (checked && !f.exists()) f.createNewFile();
                else if (!checked && f.exists()) f.delete();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        topRow.addView(sw);

        row.addView(topRow);

        TextView descTv = new TextView(this);
        descTv.setText(desc);
        descTv.setTextSize(12f);
        descTv.setAlpha(0.6f);
        row.addView(descTv);

        parent.addView(row);
        return sw;
    }

    private void addSeparator(LinearLayout parent) {
        android.view.View sep = new android.view.View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 16, 0, 16);
        sep.setBackgroundColor(0xFFCCCCCC);
        parent.addView(sep, lp);
    }

    private void syncSwitches() {
        if (sw_disable != null)
            sw_disable.setChecked(fileExists("disable.jpg"));
        if (sw_toast != null)
            sw_toast.setChecked(fileExists("no_toast.jpg"));
        if (sw_sound != null)
            sw_sound.setChecked(fileExists("no-silent.jpg"));
        if (sw_force_show != null)
            sw_force_show.setChecked(fileExists("force_show.jpg"));
        if (sw_private_dir != null)
            sw_private_dir.setChecked(fileExists("private_dir.jpg"));
    }

    private boolean fileExists(String name) {
        return new File(VIDEO_PATH + name).exists();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1);
            }
        }
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("كيفية الاستخدام")
                .setMessage(
                        "1. ضع صورة JPG/PNG أو فيديو (virtual.mp4) في:\n" +
                        "   /sdcard/DCIM/Camera1/\n\n" +
                        "2. فعّل الوحدة في LSPosed واختر التطبيقات المستهدفة\n\n" +
                        "3. أعد تشغيل التطبيق المستهدف\n\n" +
                        "ملفات التحكم:\n" +
                        "• disable.jpg = تعطيل VCAM\n" +
                        "• no_toast.jpg = إخفاء الإشعارات\n" +
                        "• no-silent.jpg = تشغيل الصوت\n" +
                        "• virtual.mp4 = فيديو بدلاً من الصورة\n" +
                        "• flip_h.cfg = قلب أفقي\n" +
                        "• flip_v.cfg = قلب عمودي\n" +
                        "• rotate_90.cfg = تدوير 90°\n" +
                        "• move_left.cfg = تحريك يساراً"
                )
                .setPositiveButton("حسناً", null)
                .show();
    }
}
