package com.petnbu.petnbu;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefUtil {

    public static final String PREF_NAME = "PET_SETTING";

    public static final String KEY_LOGGED_USER_ID = "key-logged-user-id";

    public static void saveUserId(Context c, String userId){
        SharedPreferences setting = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setting.edit().putString(KEY_LOGGED_USER_ID, userId).apply();
    }
}
