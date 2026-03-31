package com.example.vcam;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;
import java.nio.ByteBuffer;
import de.robv.android.xposed.XposedBridge;

public class VideoToFrames {

    public interface Callback {
        void onDecodeFrame(int index);
        void onFinishDecode();
    }

    private static final long TIMEOUT_US = 10000;

    private MediaExtractor extractor;
    private MediaCodec decoder;
    private Surface outputSurface;
    private SurfaceTexture surfaceTexture;
    private volatile boolean isRunning = false;
    private Callback callback;
    private String videoPath;

    public VideoToFrames(String videoPath, Callback callback) {
        this.videoPath = videoPath;
        this.callback = callback;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        new Thread(this::decode).start();
    }

    public void stop() {
        isRunning = false;
    }

    private void decode() {
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(videoPath);

            int videoTrack = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    videoTrack = i;
                    break;
                }
            }

            if (videoTrack < 0 || format == null) {
                XposedBridge.log("[VCAM] No video track found");
                return;
            }

            extractor.selectTrack(videoTrack);
            String mime = format.getString(MediaFormat.KEY_MIME);

            surfaceTexture = new SurfaceTexture(10);
            outputSurface = new Surface(surfaceTexture);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface, null, 0);
            decoder.start();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            int frameIndex = 0;

            while (isRunning) {
                if (!inputDone) {
                    int inputId = decoder.dequeueInputBuffer(TIMEOUT_US);
                    if (inputId >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputId);
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputId, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            decoder.queueInputBuffer(inputId, 0, sampleSize,
                                extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                int outputId = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
                if (outputId >= 0) {
                    decoder.releaseOutputBuffer(outputId, true);
                    if (callback != null) callback.onDecodeFrame(frameIndex++);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
                }
            }

            if (callback != null) callback.onFinishDecode();

        } catch (Exception e) {
            XposedBridge.log("[VCAM] VideoToFrames error: " + e.toString());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (decoder != null) { decoder.stop(); decoder.release(); decoder = null; }
            if (extractor != null) { extractor.release(); extractor = null; }
            if (outputSurface != null) { outputSurface.release(); outputSurface = null; }
            if (surfaceTexture != null) { surfaceTexture.release(); surfaceTexture = null; }
        } catch (Exception e) {
            XposedBridge.log("[VCAM] cleanup error: " + e.toString());
        }
    }
}
