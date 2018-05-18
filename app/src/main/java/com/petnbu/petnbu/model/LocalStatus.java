package com.petnbu.petnbu.model;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class LocalStatus {

    @Retention(SOURCE)
    @IntDef(value = {STATUS_NEW, STATUS_UPLOADING, STATUS_ERROR, STATUS_DONE})
    public @interface LOCAL_STATUS{}

    public static final int STATUS_NEW = 0;
    public static final int STATUS_UPLOADING = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_DONE = 3;
}
