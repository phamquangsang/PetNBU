package com.petnbu.petnbu.jobs;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.petnbu.petnbu.api.StorageApi;
import com.petnbu.petnbu.model.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.work.Data;

public class UploadPhotoWorker extends PhotoWorker {

    @NonNull
    @Override
    public WorkerResult doWork() {
        WorkerResult workerResult = WorkerResult.FAILURE;

        String photoName = getInputData().getString(KEY_PHOTO, "");
        if(!TextUtils.isEmpty(photoName)) {
            String jsonPhoto = getInputData().getString(photoName, "");
            try {
                if(!TextUtils.isEmpty(jsonPhoto)) {
                    Photo photo = fromJson(jsonPhoto);
                    if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        uploadPhoto(photo, countDownLatch);
                        countDownLatch.await();
                    } else {
                        String key = Uri.parse(photo.getOriginUrl()).getLastPathSegment();
                        Data data = new Data.Builder()
                                .putString(key, toJson(photo))
                                .build();
                        setOutputData(data);
                    }
                    workerResult = WorkerResult.SUCCESS;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return workerResult;
    }

    private void uploadPhoto(Photo photo, CountDownLatch countDownLatch) {
        ArrayList<String> urls = new ArrayList<>();
        urls.add(photo.getOriginUrl());
        urls.add(photo.getLargeUrl());
        urls.add(photo.getMediumUrl());
        urls.add(photo.getSmallUrl());
        urls.add(photo.getThumbnailUrl());

        new StorageApi.OnUploadingImage(urls){

            @Override
            public void onCompleted(List<String> result) {
                String key = Uri.parse(photo.getOriginUrl()).getLastPathSegment();

                for (String remoteUrl : result) {
                    if(remoteUrl.contains("-FHD")) {
                        photo.setLargeUrl(remoteUrl);
                    } else if(remoteUrl.contains("-HD")) {
                        photo.setMediumUrl(remoteUrl);
                    } else if(remoteUrl.contains("-qHD")) {
                        photo.setSmallUrl(remoteUrl);
                    } else if(remoteUrl.contains("-thumbnail")) {
                        photo.setThumbnailUrl(remoteUrl);
                    } else {
                        photo.setOriginUrl(remoteUrl);
                    }
                }

                for (String url : urls) {
                    File file = new File(url);
                    if(file.exists()) {
                        file.delete();
                    }
                }

                Data data = new Data.Builder()
                        .putString(key, toJson(photo))
                        .build();
                setOutputData(data);
                countDownLatch.countDown();
            }

            @Override
            public void onFailed(Exception e) {
                countDownLatch.countDown();
            }
        }.start();
    }
}
