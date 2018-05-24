package com.petnbu.petnbu.db;

import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.text.TextUtils;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Photo;

import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class PetTypeConverters {
    public static Gson gson = PetApplication.getAppComponent().getGson();

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
