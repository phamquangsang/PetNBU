package com.petnbu.petnbu.db;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.model.Photo;

import java.util.List;


public class ListPhotoConverters {

    public static Gson gson = PetApplication.getAppComponent().getGson();

    @TypeConverter
    public static String listPhotosToJson(List<Photo> photos){
        return gson.toJson(photos);
    }

    @TypeConverter
    public static List<Photo> jsonToListPhoto(String photosJson){
        return gson.fromJson(photosJson, new TypeToken<List<Photo>>(){}.getType());
    }
}
