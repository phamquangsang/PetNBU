package com.petnbu.petnbu.db;

import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.util.StringUtil;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Photo;

import java.util.ArrayList;
import java.util.Collections;
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
    public static Long dateToLong(Date date){
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date longToDate(Long dateAsLong){
        return dateAsLong == null ? null : new Date(dateAsLong);
    }

    @TypeConverter
    public static String listStringToString(List<String> list){
        return TextUtils.join(",",list);
    }

    @TypeConverter
    public static List<String> stringBackToList(String lists){
        return ArrayUtils.toArrayList(TextUtils.split(lists, ","));
    }
}
