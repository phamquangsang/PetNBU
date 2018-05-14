package com.petnbu.petnbu.util;

import android.os.Build;
import android.os.Trace;

import com.petnbu.petnbu.BuildConfig;

import timber.log.Timber;

public class TraceUtils {

    public static final String TAG = TraceUtils.class.getSimpleName();

    public static void begin(String tag){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && BuildConfig.DEBUG) {
            Trace.beginSection(tag);
        }
    }

    public static void end(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && BuildConfig.DEBUG) {
            Trace.endSection();
        }
    }

    public static void begin(String tag, DoingTask task){
        long i = System.currentTimeMillis();
        try {
            begin(tag);
            task.doing();
        } finally{
            Timber.d(tag + " in " + (System.currentTimeMillis() - i));
            end();
        }
    }

    public interface DoingTask{
        void doing();
    }

}