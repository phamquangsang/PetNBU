package com.petnbu.petnbu.db

import android.arch.persistence.room.TypeConverter

import com.google.gson.Gson
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.model.Photo

public object PhotoConverters {

    public var gson = PetApplication.getAppComponent().gson

    @TypeConverter
    @JvmStatic
    public fun photoToJson(photo: Photo?): String? {
        return gson.toJson(photo)
    }

    @TypeConverter
    @JvmStatic
    public fun jsonToPhoto(photoJson: String?): Photo? {
        return gson.fromJson(photoJson, Photo::class.java)
    }
}
