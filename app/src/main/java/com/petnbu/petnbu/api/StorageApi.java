package com.petnbu.petnbu.api;


import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.petnbu.petnbu.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

/**
 * Created by Quang Quang on 11/23/2016.
 */

public class StorageApi {

    public static final String TAG = StorageApi.class.getSimpleName();

    // map files to file

    public static void updateImage(Uri uri, String fileName, OnResultListener onResultListener) {
        Log.i(TAG, "updateImage: update file " + uri.toString());
        StorageReference ref = getStorageRef().child("image_" + System.currentTimeMillis() + fileName);
        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> onResultListener.onSuccess(fileName, taskSnapshot))
                .addOnFailureListener(e -> onResultListener.onFailure(fileName, e));
    }

    public static void updateBitmap(Bitmap bitmap, String fileName, OnResultListener onResultListener) {
        Timber.i("updateBitmap " + fileName);
        StorageReference ref = getStorageRef().child("image_" + System.currentTimeMillis() + fileName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
        byte[] data = outputStream.toByteArray();
        ref.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> onResultListener.onSuccess(fileName, taskSnapshot))
                .addOnFailureListener(e -> onResultListener.onFailure(fileName, e));
    }

    public static StorageReference getStorageRef() {
        StorageReference storage = FirebaseStorage.getInstance().getReferenceFromUrl(BuildConfig.FIREBASE_STORAGE_URL);
        return storage.child("pets").child("photos");
    }

    public static abstract class OnUploadingImage {

        private AtomicInteger mTotalImage;
        private List<String> mList;
        private List<String> mResult;
        private OnResultListener mOnResultListener = new OnResultListener() {
            @Override
            public void onSuccess(String fileName, UploadTask.TaskSnapshot taskSnapshot) {
                mTotalImage.decrementAndGet();
                mResult.add(taskSnapshot.getDownloadUrl().toString());
                if (mTotalImage.get() == 0) {
                    onCompleted(mResult);
                }
            }

            @Override
            public void onFailure(String fileName, Exception e) {
                mTotalImage.decrementAndGet();
                onFailed(e);
            }
        };

        public OnUploadingImage(List<String> imageList) {
            mTotalImage = new AtomicInteger(imageList.size());
            mList = imageList;
            mResult = new ArrayList<>();
        }

        public void start() {
            for (String file : mList) {
                Uri uri = Uri.parse(file);
                updateImage(uri, uri.getLastPathSegment(), mOnResultListener);
            }
        }

        public abstract void onCompleted(List<String> result);

        public abstract void onFailed(Exception e);
    }

    public interface OnResultListener {

        void onSuccess(String fileName, UploadTask.TaskSnapshot taskSnapshot);

        void onFailure(String fileName, Exception e);
    }
}
