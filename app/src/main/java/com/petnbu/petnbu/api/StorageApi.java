package com.petnbu.petnbu.api;


import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.util.ArrayMap;
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

    public static abstract class OnUploadingFile<T> {

        AtomicInteger mTotalFileCount;
        List<T> mData;
        ArrayMap<String, String> mResult;
        OnResultListener mOnResultListener = new OnResultListener() {
            @Override
            public void onSuccess(String fileName, UploadTask.TaskSnapshot taskSnapshot) {
                mTotalFileCount.decrementAndGet();
                mResult.put(fileName, taskSnapshot.getDownloadUrl().toString());
                if (mTotalFileCount.get() == 0) {
                    onCompleted(mResult);
                }
            }

            @Override
            public void onFailure(String fileName, Exception e) {
                mTotalFileCount.decrementAndGet();
                onFailed(e);
            }
        };

        public OnUploadingFile(List<T> data) {
            mTotalFileCount = new AtomicInteger(data.size());
            mData = data;
            mResult = new ArrayMap<>();
        }

        public void start() {
            for (T data : mData) {
                UploadRequest uploadRequest = getUploadRequest(data);
                if(uploadRequest.mFileUri != null) {
                    updateImage(uploadRequest.mFileUri, uploadRequest.mFileUri.getLastPathSegment(), mOnResultListener);
                } else if(uploadRequest.mBitmap != null) {
                    updateBitmap(uploadRequest.mBitmap, uploadRequest.mResourceName, mOnResultListener);
                }
            }
        }

        public abstract void onCompleted(ArrayMap<String, String> result);

        public abstract void onFailed(Exception e);

        public abstract UploadRequest getUploadRequest(T t);
    }

    public static abstract class OnUploadingMultiSizeBitmap<T> extends OnUploadingFile<T> {

        private int[] mSizeTypes;

        public OnUploadingMultiSizeBitmap(List<T> data, int[] sizeTypes) {
            super(data);
            mSizeTypes = sizeTypes;
            mTotalFileCount = new AtomicInteger(data.size() * (sizeTypes.length + 1)); // +1 for origin size
        }

        @Override
        public void start() {
            for (T t : mData) {
                UploadRequest uploadRequest = getUploadRequest(t);
                if(uploadRequest != null && uploadRequest.mBitmap != null) {
                    updateBitmap(uploadRequest.mBitmap, uploadRequest.mResourceName, mOnResultListener);
                    for (int sizeType : mSizeTypes) {
                        UploadRequest resizedBitmapRequest = getResizedBitmapRequest(t, uploadRequest.mResourceName, uploadRequest.mBitmap, sizeType);
                        updateBitmap(resizedBitmapRequest.mBitmap, resizedBitmapRequest.mResourceName, mOnResultListener);
                    }
                }
            }
        }

        public abstract UploadRequest getResizedBitmapRequest(T t, String srcName, Bitmap bitmap, int sizeType);
    }

    public static class UploadRequest {

        private Uri mFileUri;
        private Bitmap mBitmap;
        private String mResourceName;

        public Uri getFileUri() {
            return mFileUri;
        }

        public void setFileUri(Uri fileUri) {
            mFileUri = fileUri;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public String getResourceName() {
            return mResourceName;
        }

        public void setResourceName(String resourceName) {
            mResourceName = resourceName;
        }
    }

    interface OnResultListener {

        void onSuccess(String fileName, UploadTask.TaskSnapshot taskSnapshot);

        void onFailure(String fileName, Exception e);
    }
}
