package com.petnbu.petnbu.api;


import android.net.Uri;
import android.support.annotation.NonNull;
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
        ref.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get a URL to the uploaded content
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                onSuccessListener.onSuccess(taskSnapshot);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                onFailureListener.onFailure(e);
            }
        });

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
            for (String file :
                    mList) {
                OnSuccessListener<UploadTask.TaskSnapshot> successListener = new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        mTotalImage.decrementAndGet();
                        mResult.add(taskSnapshot.getDownloadUrl().toString());
                        isFinish();
                    }
                };
                OnFailureListener failureListener = new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mTotalImage.decrementAndGet();
                        isFinish();
                    }
                };
                updateImage(Uri.parse(file), successListener, failureListener);
            }
        }

        private boolean isFinish() {
            if (mTotalImage.get() == 0) {
                onCompleted(mResult);
                return true;
            } else
                return false;
        }

        public abstract void onCompleted(List<String> result);
    }
}
