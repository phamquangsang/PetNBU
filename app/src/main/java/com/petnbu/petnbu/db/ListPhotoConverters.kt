package com.petnbu.petnbu.db

import android.arch.persistence.room.TypeConverter

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.model.Photo


public object ListPhotoConverters {

    var gson = PetApplication.getAppComponent().gson

    @TypeConverter
    @JvmStatic
    fun listPhotosToJson(photos: List<Photo>?): String {
        return gson.toJson(photos)
    }

    @TypeConverter
    @JvmStatic
    fun jsonToListPhoto(photosJson: String?): List<Photo>? {
        return gson.fromJson(photosJson, object : TypeToken<List<Photo>?>() {

        }.type)
    }
}
