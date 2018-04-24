package com.petnbu.petnbu.db;

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.petnbu.petnbu.model.Photo;

import java.util.Date;
import java.util.List;

public class PetTypeConverters {
    @TypeConverter
    public static String listPhotosToJson(List<Photo> photos){
        Gson gson = new Gson();
        return gson.toJson(photos);
    }

    @TypeConverter
    public static List<Photo> jsonToListPhoto(String photosJson){
        Gson gson = new Gson();
        return gson.fromJson(photosJson, new TypeToken<List<Photo>>(){}.getType());
    }

    @TypeConverter
    public static String photoToJson(Photo photo){
        Gson gson = new Gson();
        return gson.toJson(photo);
    }

    @TypeConverter
    public static Photo jsonToPhoto(String photoJson){
        Gson gson = new Gson();
        return gson.fromJson(photoJson, Photo.class);
    }

    @TypeConverter
    public static long dateToLong(Date date){
        return date.getTime();
    }

    @TypeConverter
    public static Date longToDate(long dateAsLong){
        return new Date(dateAsLong);
    }
}
