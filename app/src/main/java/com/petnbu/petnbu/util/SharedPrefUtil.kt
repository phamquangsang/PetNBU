package com.petnbu.petnbu.util

import com.petnbu.petnbu.PetApplication

object SharedPrefUtil {

    private const val KEY_LOGGED_USER_ID = "key-logged-user-id"

    val userId: String
        get() = PetApplication.appComponent.sharedPref.getString(KEY_LOGGED_USER_ID, "") ?: ""

    fun saveUserId(userId: String) {
        PetApplication.appComponent.sharedPref.edit()?.putString(KEY_LOGGED_USER_ID, userId)?.apply()
    }
}
