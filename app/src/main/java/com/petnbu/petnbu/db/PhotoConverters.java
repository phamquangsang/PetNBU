package com.petnbu.petnbu.db;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.model.Photo;

public class PhotoConverters {

    public static Gson gson = PetApplication.getAppComponent().getGson();

    @TypeConverter
    public static String photoToJson(Photo photo){
        Gson gson = new Gson();
        return gson.toJson(photo);
    }

    @TypeConverter
    public static Photo jsonToPhoto(String photoJson){
        return gson.fromJson(photoJson, Photo.class);
    }
}
