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
import timber.log.Timber;

public class UploadPhotoWorker extends PhotoWorker {

    private final Data.Builder outputDataBuilder = new Data.Builder();

    @NonNull
    @Override
    public WorkerResult doWork() {
        boolean isSuccess = false;
        String photoName = getInputData().getString(KEY_PHOTO, "");

        if(getInputData().getBoolean("result", false) && !TextUtils.isEmpty(photoName)) {
            String jsonPhoto = getInputData().getString(photoName, "");
            try {
                if(!TextUtils.isEmpty(jsonPhoto)) {
                    Photo photo = fromJson(jsonPhoto);
                    String key = Uri.parse(photo.getOriginUrl()).getLastPathSegment();
                    if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        uploadPhoto(photo, countDownLatch);
                        countDownLatch.await();
                    } else {
                        outputDataBuilder.putString(key, toJson(photo));
                    }
                    isSuccess = true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        outputDataBuilder.putBoolean("result", isSuccess);
        setOutputData(outputDataBuilder.build());
        return WorkerResult.SUCCESS;
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
                outputDataBuilder.putString(key, toJson(photo));
                countDownLatch.countDown();
            }

            @Override
            public void onFailed(Exception e) {
                countDownLatch.countDown();
            }
        }.start();
    }
}
