package com.petnbu.petnbu.util;

import android.util.Log;

import java.util.UUID;

import timber.log.Timber;

public class IdUtil {

    public static String generateID(String prefix){
        String randomId = UUID.randomUUID().toString();
        Timber.i("generatedId: %s", (prefix + randomId));
        return prefix + randomId;
    }
}
