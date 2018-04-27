package com.petnbu.petnbu;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefUtil {

    public static final String PREF_NAME = "PET_SETTING";

    public static final String KEY_LOGGED_USER_ID = "key-logged-user-id";

    public static void saveUserId(Context context, String userId){
        SharedPreferences setting = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setting.edit().putString(KEY_LOGGED_USER_ID, userId).apply();
    }

    public static String getUserId(Context context){
        SharedPreferences setting = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return setting.getString(KEY_LOGGED_USER_ID, "");
    }
}
