package com.petnbu.petnbu.db

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.model.Photo


object ListPhotoConverters {

    val gson: Gson = PetApplication.appComponent.gson

    @TypeConverter
    @JvmStatic
    fun listPhotosToJson(photos: MutableList<Photo>?): String = gson.toJson(photos)

    @TypeConverter
    @JvmStatic
    fun jsonToListPhoto(photosJson: String?): MutableList<Photo>? = gson.fromJson(photosJson, object : TypeToken<MutableList<Photo>?>() {}.type)
}