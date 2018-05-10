package com.petnbu.petnbu.util;

public class ImageUtils {

    public static final int FHD = 1; // large
    public static final int HD = 2; // medium
    public static final int qHD = 3; // small
    public static final int THUMBNAIL = 4; // thumbnail

    public static String getResolutionTitle(int type) {
        switch (type) {
            case FHD: //1920x1080
                return "FHD";
            case HD: //1280x720
                return "HD";
            case qHD: //960x540
                return "qHD";
            case THUMBNAIL: //150x150
                return "thumbnail";
            default: return "";
        }
    }

    public static int getResolutionType(int width) {
        if(width > 1920) {
            return -1;
        } else if(width > 1280) {
            return FHD;
        } else if(width > 960) {
            return HD;
        } else return qHD;
    }

    public static int[] getResolutionForImage(int type, int width, int height) {
        int resolution[] = new int[2]; // 0 width 1 height
        float ratio = (float) width / (float) height;
        switch (type) {
            case FHD: //1920x1080
                if(width > 1920) {
                    resolution[0] = 1920;
                    resolution[1] = (int) (1920 / ratio);
                } else {
                    resolution[0] = width;
                    resolution[1] = height;
                }
                break;
            case HD: //1280x720
                if(width > 1280) {
                    resolution[0] = 1280;
                    resolution[1] = (int) (1280 / ratio);
                } else {
                    resolution[0] = width;
                    resolution[1] = height;
                }
                break;
            case qHD: //960x540
                if(width > 960) {
                    resolution[0] = 960;
                    resolution[1] = (int) (960 / ratio);
                } else {
                    resolution[0] = width;
                    resolution[1] = height;
                }
                break;
            case THUMBNAIL: //150x150
                if(width > 150) {
                    resolution[0] = 150;
                    resolution[1] = (int) (150 / ratio);
                } else {
                    resolution[0] = width;
                    resolution[1] = height;
                }
                break;
        }
        return resolution;
    }
}
