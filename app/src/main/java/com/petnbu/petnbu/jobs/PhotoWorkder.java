package com.petnbu.petnbu.jobs;

import com.google.gson.Gson;
import com.petnbu.petnbu.model.Photo;

import androidx.work.Data;
import androidx.work.Worker;

public abstract class PhotoWorkder extends Worker {

    public static final String KEY_PHOTO = "key-photo";

    public static Data data(Photo photo) {
        Data data = new Data.Builder()
                .putString(KEY_PHOTO, toJson(photo))
                .build();
        return data;
    }

    protected static final String toJson(Photo photo) {
        return new Gson().toJson(photo);
    }

    protected static final Photo fromJson(String photoJson) {
        return new Gson().fromJson(photoJson, Photo.class);
    }
}
