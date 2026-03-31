package com.example.vcam;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.os.Build;
import java.io.File;

public class FloatingControlService extends Service {

    private WindowManager wm;
    private View floatView;
    private WindowManager.LayoutParams params;
    private int initX, initY;
    private float initTouchX, initTouchY;

    public static final String VIDEO_PATH = "/storage/emulated/0/DCIM/Camera1/";

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        return START_STICKY;
    }

    private void showFloatingWindow() {
        if (floatView != null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // إنشاء الحاوية
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.argb(200, 30, 30, 30));
        root.setPadding(12, 8, 12, 8);

        // شريط السحب
        LinearLayout dragBar = new LinearLayout(this);
        dragBar.setBackgroundColor(Color.argb(180, 60, 60, 60));
        dragBar.setPadding(8, 4, 8, 4);

        LinearLayout.LayoutParams dragParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
        root.addView(dragBar, dragParams);

        // أزرار التحكم
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);

        // زر تعطيل/تفعيل VCAM
        ImageButton btnToggle = makeButton("VCAM\nON");
        updateToggleBtn(btnToggle);
        btnToggle.setOnClickListener(v -> {
            File f = new File(VIDEO_PATH + "disable.jpg");
            if (f.exists()) {
                f.delete();
                btnToggle.setBackgroundColor(Color.argb(200, 0, 150, 0));
                setLabel(btnToggle, "VCAM\nON");
            } else {
                try { f.createNewFile(); } catch (Exception ignored) {}
                btnToggle.setBackgroundColor(Color.argb(200, 180, 0, 0));
                setLabel(btnToggle, "VCAM\nOFF");
            }
        });

        // زر صوت الفيديو
        ImageButton btnSound = makeButton("SOUND\nOFF");
        updateSoundBtn(btnSound);
        btnSound.setOnClickListener(v -> {
            File f = new File(VIDEO_PATH + "no-silent.jpg");
            if (f.exists()) {
                f.delete();
                setLabel(btnSound, "SOUND\nOFF");
                btnSound.setBackgroundColor(Color.argb(200, 80, 80, 80));
            } else {
                try { f.createNewFile(); } catch (Exception ignored) {}
                setLabel(btnSound, "SOUND\nON");
                btnSound.setBackgroundColor(Color.argb(200, 0, 120, 200));
            }
        });

        // زر Toast إشعارات
        ImageButton btnToast = makeButton("TOAST\nON");
        updateToastBtn(btnToast);
        btnToast.setOnClickListener(v -> {
            File f = new File(VIDEO_PATH + "no_toast.jpg");
            if (f.exists()) {
                f.delete();
                setLabel(btnToast, "TOAST\nON");
                btnToast.setBackgroundColor(Color.argb(200, 80, 80, 80));
            } else {
                try { f.createNewFile(); } catch (Exception ignored) {}
                setLabel(btnToast, "TOAST\nOFF");
                btnToast.setBackgroundColor(Color.argb(200, 150, 100, 0));
            }
        });

        // زر إغلاق النافذة العائمة
        ImageButton btnClose = makeButton("✕");
        btnClose.setBackgroundColor(Color.argb(200, 180, 0, 0));
        btnClose.setOnClickListener(v -> stopSelf());

        btnRow.addView(btnToggle);
        btnRow.addView(space());
        btnRow.addView(btnSound);
        btnRow.addView(space());
        btnRow.addView(btnToast);
        btnRow.addView(space());
        btnRow.addView(btnClose);

        root.addView(btnRow);
        floatView = root;

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // سحب النافذة
        dragBar.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initX = params.x;
                    initY = params.y;
                    initTouchX = event.getRawX();
                    initTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initX + (int)(event.getRawX() - initTouchX);
                    params.y = initY + (int)(event.getRawY() - initTouchY);
                    wm.updateViewLayout(floatView, params);
                    return true;
            }
            return false;
        });

        wm.addView(floatView, params);
    }

    private ImageButton makeButton(String label) {
        ImageButton btn = new ImageButton(this);
        btn.setBackgroundColor(Color.argb(200, 60, 60, 60));
        setLabel(btn, label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 80);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void setLabel(ImageButton btn, String label) {
        // نستخدم ContentDescription كـ tag للـ label
        btn.setContentDescription(label);
    }

    private View space() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(8, 8));
        return v;
    }

    private void updateToggleBtn(ImageButton btn) {
        File f = new File(VIDEO_PATH + "disable.jpg");
        if (f.exists()) {
            btn.setBackgroundColor(Color.argb(200, 180, 0, 0));
            setLabel(btn, "VCAM\nOFF");
        } else {
            btn.setBackgroundColor(Color.argb(200, 0, 150, 0));
            setLabel(btn, "VCAM\nON");
        }
    }

    private void updateSoundBtn(ImageButton btn) {
        File f = new File(VIDEO_PATH + "no-silent.jpg");
        if (f.exists()) {
            btn.setBackgroundColor(Color.argb(200, 0, 120, 200));
            setLabel(btn, "SOUND\nON");
        } else {
            btn.setBackgroundColor(Color.argb(200, 80, 80, 80));
            setLabel(btn, "SOUND\nOFF");
        }
    }

    private void updateToastBtn(ImageButton btn) {
        File f = new File(VIDEO_PATH + "no_toast.jpg");
        if (f.exists()) {
            btn.setBackgroundColor(Color.argb(200, 150, 100, 0));
            setLabel(btn, "TOAST\nOFF");
        } else {
            btn.setBackgroundColor(Color.argb(200, 80, 80, 80));
            setLabel(btn, "TOAST\nON");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatView != null && wm != null) {
            wm.removeView(floatView);
            floatView = null;
        }
    }
}
