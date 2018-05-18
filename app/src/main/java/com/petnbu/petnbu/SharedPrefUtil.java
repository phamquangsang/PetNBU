package com.petnbu.petnbu;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefUtil {

    public static final String KEY_LOGGED_USER_ID = "key-logged-user-id";

    public static void saveUserId(String userId){
        SharedPreferences setting = PetApplication.getAppComponent().getSharedPref();
        setting.edit().putString(KEY_LOGGED_USER_ID, userId).apply();
    }

    public static String getUserId(Context context){
        SharedPreferences setting = PetApplication.getAppComponent().getSharedPref();
        return setting.getString(KEY_LOGGED_USER_ID, "");
    }
}
