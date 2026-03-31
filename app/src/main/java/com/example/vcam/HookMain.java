package com.example.vcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.MediaPlayer;
import android.os.Environment;
import android.view.Surface;
import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookMain implements IXposedHookLoadPackage {

    // ===================== حقول ثابتة =====================
    public static volatile byte[] data_buffer = new byte[1];
    public static volatile byte[] input = null;

    public static Surface c1_fake_surface = null;
    public static SurfaceTexture c1_fake_texture = null;

    public static CaptureRequest.Builder c2_builder = null;
    public static VideoToFrames c2_hw_decode_obj = null;
    public static VideoToFrames c2_hw_decode_obj_1 = null;
    public static MediaPlayer c2_player = null;
    public static MediaPlayer c2_player_1 = null;
    public static Surface c2_preview_Surfcae = null;
    public static Surface c2_preview_Surfcae_1 = null;
    public static Surface c2_reader_Surfcae = null;
    public static Surface c2_reader_Surfcae_1 = null;
    public static Class<?> c2_state_callback = null;
    public static android.hardware.camera2.CameraDevice.StateCallback c2_state_cb = null;
    public static Surface c2_virtual_surface = null;
    public static SurfaceTexture c2_virtual_surfaceTexture = null;
    public static Class<?> camera_callback_calss = null;
    public static Camera camera_onPreviewFrame = null;
    public static SurfaceTexture fake_SurfaceTexture = null;
    public static SessionConfiguration fake_sessionConfiguration = null;
    public static VideoToFrames hw_decode_obj = null;
    public static boolean is_first_hook_build = true;
    public static boolean is_hooked = false;
    public static boolean is_someone_playing = false;
    public static MediaPlayer mMediaPlayer = null;
    public static Surface mSurface = null;
    public static SurfaceTexture mSurfacetexture = null;
    public static Camera mcamera1 = null;
    public static int mhight = 0;
    public static MediaPlayer mplayer1 = null;
    public static int mwidth = 0;
    public static int onemhight = 0;
    public static int onemwidth = 0;
    public static android.view.SurfaceHolder ori_holder = null;
    public static Camera origin_preview_camera = null;
    public static OutputConfiguration outputConfiguration = null;
    public static SessionConfiguration sessionConfiguration = null;
    public static Camera start_preview_camera = null;
    public static String video_path = "/storage/emulated/0/DCIM/Camera1/";

    // ===================== ميزة التحويل الجديدة =====================
    public static boolean flip_horizontal = false;
    public static boolean flip_vertical = false;
    public static float rotation_degrees = 0f;
    public static float translate_x = 0f;
    public static float translate_y = 0f;

    // ===================== حقول النسخة =====================
    public int c2_ori_height = 720;
    public int c2_ori_width = 1280;
    public int imageReaderFormat = 0;
    public boolean need_recreate = false;
    public boolean need_to_show_toast = true;
    public Context toast_content = null;

    // ===================== تطبيق التحويلات على Bitmap =====================
    public static Bitmap applyVideoTransform(Bitmap source) {
        if (source == null) return null;

        // إذا لا يوجد أي تحويل، أرجع الأصل مباشرة
        if (!flip_horizontal && !flip_vertical && rotation_degrees == 0f
                && translate_x == 0f && translate_y == 0f) {
            return source;
        }

        Matrix matrix = new Matrix();

        if (translate_x != 0f || translate_y != 0f) {
            matrix.postTranslate(translate_x, translate_y);
        }
        if (flip_horizontal) {
            matrix.postScale(-1f, 1f, source.getWidth() / 2f, source.getHeight() / 2f);
        }
        if (flip_vertical) {
            matrix.postScale(1f, -1f, source.getWidth() / 2f, source.getHeight() / 2f);
        }
        if (rotation_degrees != 0f) {
            matrix.postRotate(rotation_degrees, source.getWidth() / 2f, source.getHeight() / 2f);
        }

        try {
            return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
        } catch (Throwable e) {
            XposedBridge.log("[VCAM] applyVideoTransform error: " + e.toString());
            return source;
        }
    }

    // ===================== قراءة إعدادات التحويل من الملفات =====================
    public static void loadTransformSettings() {
        String base = video_path;

        flip_horizontal = new File(base + "flip_h.cfg").exists();
        flip_vertical   = new File(base + "flip_v.cfg").exists();

        rotation_degrees = 0f;
        if      (new File(base + "rotate_90.cfg").exists())  rotation_degrees = 90f;
        else if (new File(base + "rotate_180.cfg").exists()) rotation_degrees = 180f;
        else if (new File(base + "rotate_270.cfg").exists()) rotation_degrees = 270f;

        translate_x = 0f;
        if      (new File(base + "move_left.cfg").exists())  translate_x = -100f;
        else if (new File(base + "move_right.cfg").exists()) translate_x =  100f;

        translate_y = 0f;
        if      (new File(base + "move_up.cfg").exists())   translate_y = -100f;
        else if (new File(base + "move_down.cfg").exists()) translate_y =  100f;
    }

    // ===================== نقطة دخول Xposed =====================
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("[VCAM] handleLoadPackage: " + lpparam.packageName);
        process_camera1(lpparam);
        process_camera2_init(lpparam.classLoader);
    }

    // ===================== مساعدات =====================
    private File pickRandomImageFile(String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles((d, name) ->
            name.endsWith(".jpg") || name.endsWith(".jpeg") ||
            name.endsWith(".png") || name.endsWith(".bmp"));
        if (files == null || files.length == 0) return null;
        return files[0];
    }

    private Bitmap getBMP(String path) throws Throwable {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        loadTransformSettings();
        return applyVideoTransform(bmp);
    }

    private static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        byte[] yuv = new byte[width * height * 3 / 2];
        int yIdx = 0;
        int uvIdx = width * height;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int r = (argb[j * width + i] >> 16) & 0xff;
                int g = (argb[j * width + i] >>  8) & 0xff;
                int b =  argb[j * width + i]        & 0xff;
                int y = ((66*r + 129*g + 25*b + 128) >> 8) + 16;
                int u = ((-38*r - 74*g + 112*b + 128) >> 8) + 128;
                int v = ((112*r - 94*g - 18*b + 128) >> 8) + 128;
                yuv[yIdx++] = (byte) Math.max(0, Math.min(255, y));
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIdx++] = (byte) Math.max(0, Math.min(255, u));
                    yuv[uvIdx++] = (byte) Math.max(0, Math.min(255, v));
                }
            }
        }
        return yuv;
    }

    // ===================== Camera1 Hooks =====================
    private void process_camera1(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.hardware.Camera",
                lpparam.classLoader, "setPreviewCallback",
                Camera.PreviewCallback.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Camera cam = (Camera) param.thisObject;
                            camera_onPreviewFrame = cam;
                            Camera.Size size = cam.getParameters().getPreviewSize();
                            mwidth = size.width;
                            mhight = size.height;
                            XposedBridge.log("[VCAM] C1 setPreviewCallback: "
                                + mwidth + "x" + mhight);
                        } catch (Throwable e) {
                            XposedBridge.log("[VCAM] setPreviewCallback err: " + e);
                        }
                    }
                });
        } catch (Throwable e) {
            XposedBridge.log("[VCAM] hook setPreviewCallback fail: " + e);
        }

        try {
            XposedHelpers.findAndHookMethod("android.hardware.Camera",
                lpparam.classLoader, "startPreview",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        start_preview_camera = (Camera) param.thisObject;
                        XposedBridge.log("[VCAM] C1 startPreview hooked");
                    }
                });
        } catch (Throwable e) {
            XposedBridge.log("[VCAM] hook startPreview fail: " + e);
        }
    }

    // ===================== Camera2 Hooks =====================
    private void process_camera2_init(ClassLoader classLoader) {
        XposedBridge.log("[VCAM] init Camera2 hooks");
        try {
            XposedHelpers.findAndHookMethod(
                "android.hardware.camera2.CameraManager",
                classLoader, "openCamera",
                String.class,
                android.hardware.camera2.CameraDevice.StateCallback.class,
                android.os.Handler.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[VCAM] Camera2 openCamera hooked");
                    }
                });
        } catch (Throwable e) {
            XposedBridge.log("[VCAM] Camera2 hook fail: " + e);
        }
    }

    // ===================== معالجة اللقطات =====================
    private void process_a_shot_YUV(XC_MethodHook.MethodHookParam param) {
        try {
            if (new File(video_path + "disable.jpg").exists()) return;
            File imgFile = pickRandomImageFile(video_path);
            if (imgFile == null) {
                XposedBridge.log("[VCAM] YUV: no image file");
                return;
            }
            Bitmap bmp = getBMP(imgFile.getAbsolutePath());
            byte[] yuv = getYUVByBitmap(bmp);
            if (param.args != null && param.args.length > 0) {
                param.args[0] = yuv;
            }
        } catch (Throwable e) {
            XposedBridge.log("[VCAM] process_a_shot_YUV err: " + e);
        }
    }

    private void process_a_shot_jpeg(XC_MethodHook.MethodHookParam param, int idx) {
        try {
            onemwidth = 0; onemhight = 0;
            try {
                Camera cam = (Camera) param.args[1];
                Camera.Size size = cam.getParameters().getPreviewSize();
                onemwidth = size.width;
                onemhight = size.height;
            } catch (Exception ignored) {}

            if (new File(video_path + "disable.jpg").exists()) return;
            File imgFile = pickRandomImageFile(video_path);
            if (imgFile == null) {
                XposedBridge.log("[VCAM] JPEG: no image file");
                return;
            }
            Bitmap bmp = getBMP(imgFile.getAbsolutePath());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            param.args[idx] = baos.toByteArray();
        } catch (Throwable e) {
            XposedBridge.log("[VCAM] process_a_shot_jpeg err: " + e);
        }
    }

    // ===================== Surface الافتراضي =====================
    private Surface create_virtual_surface() {
        if (need_recreate) {
            if (c2_virtual_surfaceTexture != null) {
                c2_virtual_surfaceTexture.release();
                c2_virtual_surfaceTexture = null;
            }
            if (c2_virtual_surface != null) {
                c2_virtual_surface.release();
                c2_virtual_surface = null;
            }
            c2_virtual_surfaceTexture = new SurfaceTexture(15);
            c2_virtual_surface = new Surface(c2_virtual_surfaceTexture);
            need_recreate = false;
        } else if (c2_virtual_surface == null) {
            need_recreate = true;
            c2_virtual_surface = create_virtual_surface();
        }
        XposedBridge.log("[VCAM] virtual surface: " + c2_virtual_surface);
        return c2_virtual_surface;
    }

    private void process_camera2_play() {
        XposedBridge.log("[VCAM] process_camera2_play");
    }

    private void process_callback(XC_MethodHook.MethodHookParam param) {
        XposedBridge.log("[VCAM] process_callback");
    }

    private void process_camera2Session_callback(CameraCaptureSession.StateCallback cb) {
        XposedBridge.log("[VCAM] process_camera2Session_callback");
    }
}
