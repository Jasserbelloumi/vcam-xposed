package com.example.vcam;

public class OutputImageFormat {
    public static final int UNKNOWN = 0;
    public static final int NV21 = 17;
    public static final int YUV_420_888 = 35;
    public static final int JPEG = 256;
    public static final int RAW_SENSOR = 32;
    public static final int RGB_565 = 4;

    private int format;

    public OutputImageFormat(int format) {
        this.format = format;
    }

    public int getFormat() {
        return format;
    }

    public String formatName() {
        switch (format) {
            case NV21: return "NV21";
            case YUV_420_888: return "YUV_420_888";
            case JPEG: return "JPEG";
            case RAW_SENSOR: return "RAW_SENSOR";
            case RGB_565: return "RGB_565";
            default: return "UNKNOWN(" + format + ")";
        }
    }
}
