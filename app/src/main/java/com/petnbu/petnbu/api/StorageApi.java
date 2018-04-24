package com.petnbu.petnbu.api;


import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Quang Quang on 11/23/2016.
 */

public class StorageApi {

    public static final String TAG = StorageApi.class.getSimpleName();

    public static void updateImage(Uri file,
                                   final OnSuccessListener onSuccessListener,
                                   final OnFailureListener onFailureListener) {
        Log.i(TAG, "updateImage: update file " + file.toString());
        StorageReference ref = getStorageRef().child("image_" + System.currentTimeMillis() + file.getLastPathSegment());
        ref.putFile(file).addOnSuccessListener(taskSnapshot -> {
            // Get a URL to the uploaded content
            Uri downloadUrl = taskSnapshot.getDownloadUrl();
            onSuccessListener.onSuccess(taskSnapshot);

        }).addOnFailureListener(e -> onFailureListener.onFailure(e));

    }

    public static StorageReference getStorageRef() {
        StorageReference storage = FirebaseStorage.getInstance().getReferenceFromUrl("gs://petnbu.appspot.com");
        return storage.child("pets").child("photos");
    }

    public static abstract class OnUploadingImage {

        private AtomicInteger mTotalImage;
        private List<String> mList;
        private List<String> mResult;

        public OnUploadingImage(List<String> imageList) {
            mTotalImage = new AtomicInteger(imageList.size());
            mList = imageList;
            mResult = new ArrayList<>();
        }

        public void start() {
            for (String file : mList) {
                OnSuccessListener<UploadTask.TaskSnapshot> successListener = taskSnapshot -> {
                    mTotalImage.decrementAndGet();
                    mResult.add(taskSnapshot.getDownloadUrl().toString());
                    if (mTotalImage.get() == 0) {
                        onCompleted(mResult);
                    }
                };
                OnFailureListener failureListener = e -> {
                    mTotalImage.decrementAndGet();
                    onFailed(e);
                };
                updateImage(Uri.parse(file), successListener, failureListener);
            }
        }

        public abstract void onCompleted(List<String> result);

        public abstract void onFailed(Exception e);
    }
}
