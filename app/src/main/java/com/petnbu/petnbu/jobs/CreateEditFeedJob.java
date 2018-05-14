package com.petnbu.petnbu.jobs;


import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.webkit.URLUtil;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.StorageApi;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.util.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateEditFeedJob extends JobService {

    private static String EXTRA_FEED_ID = "extra-feed-id";
    private static String EXTRA_FLAG_UPDATING = "extra-updating";

    private FeedResponse mFeedResponse;

    private JobParameters mParams;

    @Inject
    WebService mWebService;

    @Inject
    FeedDao mFeedDao;

    @Inject
    UserDao mUserDao;

    @Inject
    PetDb mPetDb;

    @Inject
    AppExecutors mAppExecutors;

    public CreateEditFeedJob() {
        super();
        PetApplication.getAppComponent().inject(this);
    }

    public static Bundle extras(String localFeedId, boolean isUpdating) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_FEED_ID, localFeedId);
        bundle.putBoolean(EXTRA_FLAG_UPDATING, isUpdating);
        return bundle;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(JobParameters params) {
        mParams = params;
        Bundle bundle = params.getExtras();
        if (bundle == null) {
            return false;
        }

        final String feedId = bundle.getString(EXTRA_FEED_ID);
        final boolean isUpdating = bundle.getBoolean(EXTRA_FLAG_UPDATING);

        mAppExecutors.diskIO().execute(() -> {
            FeedEntity feedEntity = mFeedDao.findFeedEntityById(feedId);
            if (feedEntity == null) {
                jobFinished(params, false);
                return;
            }
            UserEntity userEntity = mUserDao.findUserById(feedEntity.getFromUserId());
            FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
            mFeedResponse = new FeedResponse(feedEntity.getFeedId(), feedUser, feedEntity.getPhotos(), feedEntity.getCommentCount(), null
                    , feedEntity.getLikeCount(), feedEntity.getContent(), feedEntity.getTimeCreated()
                    , feedEntity.getTimeUpdated(), feedEntity.getStatus());

            if (mFeedResponse.getStatus() != FeedEntity.STATUS_UPLOADING) {
                jobFinished(params, false);
                Timber.i("status is not STATUS_UPLOADING");
                return;
            }

            Timber.i("received mFeedResponse %s", mFeedResponse);

            mFeedResponse.setTimeUpdated(new Date());

            final String localFeedId = mFeedResponse.getFeedId();
            List<String> photoUrls = new ArrayList<>(mFeedResponse.getPhotos().size());

            if (isUpdating) {
                for (Photo photo : mFeedResponse.getPhotos()) {
                    if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                        photoUrls.add(photo.getOriginUrl());
                    }
                }
                if (photoUrls.isEmpty()) {
                    updateFeed(mFeedResponse);
                } else {
                    int []sizeTypes = {ImageUtils.FHD, ImageUtils.HD, ImageUtils.qHD, ImageUtils.THUMBNAIL};
                    new StorageApi.OnUploadingMultiSizeBitmap<Photo>(mFeedResponse.getPhotos(), sizeTypes) {

                        private ArrayMap<String, Photo> mFileNamePhotoMap = new ArrayMap<>();

                        @Override
                        public void onCompleted(ArrayMap<String, String> result) {
                            for (String key : result.keySet()) {
                                Photo photo = mFileNamePhotoMap.get(key);
                                String url = result.get(key);

                                if(key.contains("FHD")) {
                                    photo.setLargeUrl(url);
                                } else if(key.contains("HD")) {
                                    photo.setMediumUrl(url);
                                } else if(key.contains("qHD")) {
                                    photo.setSmallUrl(url);
                                } else if(key.contains("thumbnail")) {
                                    photo.setThumbnailUrl(url);
                                } else {
                                    photo.setOriginUrl(url);
                                }
                            }
                            updateFeed(mFeedResponse);
                        }

                        @Override
                        public void onFailed(Exception e) {
                            updateLocalFeedError(mFeedResponse);
                        }

                        @Override
                        public StorageApi.UploadRequest getUploadRequest(Photo photo) {
                            if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                                StorageApi.UploadRequest uploadRequest = new StorageApi.UploadRequest();
                                File file = new File(Utils.getPath(CreateEditFeedJob.this, Uri.parse(photo.getOriginUrl())));
                                uploadRequest.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                                uploadRequest.setResourceName(Uri.fromFile(file).getLastPathSegment());
                                mFileNamePhotoMap.put(uploadRequest.getResourceName(), photo);
                                return uploadRequest;
                            }
                            return null;
                        }

                        @Override
                        public StorageApi.UploadRequest getResizedBitmapRequest(Photo photo, String originSrcName, Bitmap bitmap, int sizeType) {
                            int[] resolution = ImageUtils.getResolutionForImage(sizeType, bitmap.getWidth(), bitmap.getHeight());
                            StorageApi.UploadRequest uploadRequest = new StorageApi.UploadRequest();
                            uploadRequest.setResourceName(String.format("%s-%s", originSrcName , ImageUtils.getResolutionTitle(sizeType)));
                            uploadRequest.setBitmap(Bitmap.createScaledBitmap(bitmap, resolution[0], resolution[1], false));
                            mFileNamePhotoMap.put(uploadRequest.getResourceName(), photo);
                            return uploadRequest;
                        }
                    }.start();
                }
            } else {
                for (Photo photo : mFeedResponse.getPhotos()) {
                    photoUrls.add(photo.getOriginUrl());
                }

                if (photoUrls.isEmpty()) {
                    createFeed(mFeedResponse, localFeedId);
                } else {
                    int []sizeTypes = {ImageUtils.FHD, ImageUtils.HD, ImageUtils.qHD, ImageUtils.THUMBNAIL};
                    new StorageApi.OnUploadingMultiSizeBitmap<Photo>(mFeedResponse.getPhotos(), sizeTypes) {

                        private ArrayMap<String, Photo> mFileNamePhotoMap = new ArrayMap<>();

                        @Override
                        public void onCompleted(ArrayMap<String, String> result) {
                            for (String fileName : result.keySet()) {
                                Photo photo = mFileNamePhotoMap.get(fileName);
                                String fileUrl = result.get(fileName);

                                if(fileName.endsWith("-FHD")) {
                                    photo.setLargeUrl(fileUrl);
                                } else if(fileName.endsWith("-HD")) {
                                    photo.setMediumUrl(fileUrl);
                                } else if(fileName.endsWith("-qHD")) {
                                    photo.setSmallUrl(fileUrl);
                                } else if(fileName.endsWith("-thumbnail")) {
                                    photo.setThumbnailUrl(fileUrl);
                                } else {
                                    photo.setOriginUrl(fileUrl);
                                }
                            }
                            createFeed(mFeedResponse, localFeedId);
                        }

                        @Override
                        public void onFailed(Exception e) {
                            updateLocalFeedError(mFeedResponse);
                        }

                        @Override
                        public StorageApi.UploadRequest getUploadRequest(Photo photo) {
                            StorageApi.UploadRequest uploadRequest = new StorageApi.UploadRequest();
                            File file = new File(Utils.getPath(CreateEditFeedJob.this, Uri.parse(photo.getOriginUrl())));
                            uploadRequest.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                            uploadRequest.setResourceName(Uri.fromFile(file).getLastPathSegment());
                            mFileNamePhotoMap.put(uploadRequest.getResourceName(), photo);
                            return uploadRequest;
                        }

                        @Override
                        public StorageApi.UploadRequest getResizedBitmapRequest(Photo photo, String originSrcName, Bitmap bitmap, int sizeType) {
                            int[] resolution = ImageUtils.getResolutionForImage(sizeType, bitmap.getWidth(), bitmap.getHeight());
                            StorageApi.UploadRequest uploadRequest = new StorageApi.UploadRequest();
                            uploadRequest.setResourceName(String.format("%s-%s", originSrcName , ImageUtils.getResolutionTitle(sizeType)));
                            uploadRequest.setBitmap(Bitmap.createScaledBitmap(bitmap, resolution[0], resolution[1], false));
                            mFileNamePhotoMap.put(uploadRequest.getResourceName(), photo);
                            return uploadRequest;
                        }
                    }.start();
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Timber.i("onJobStop");
        return true;
    }

    private void createFeed(FeedResponse feedResponse, String temporaryFeedId) {
        LiveData<ApiResponse<FeedResponse>> apiResponse = mWebService.createFeed(feedResponse);
        apiResponse.observeForever(new Observer<ApiResponse<FeedResponse>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<FeedResponse> feedApiResponse) {
                if (feedApiResponse != null) {
                    if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                        FeedResponse newFeedResponse = feedApiResponse.body;

                        Timber.i("upload mFeedResponse succeed %s", newFeedResponse.toString());
                        mAppExecutors.diskIO().execute(() -> {
                            Timber.i("update feedId from %s to %s", temporaryFeedId, newFeedResponse.getFeedId());

                            Paging currentPaging = mFeedDao.findFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
                            if (currentPaging != null) {
                                currentPaging.getIds().add(0, newFeedResponse.getFeedId());
                            }

                            Paging userPaging = mFeedDao.findFeedPaging(newFeedResponse.getFeedUser().getUserId());
                            if (userPaging != null) {
                                userPaging.getIds().add(0, newFeedResponse.getFeedId());
                            }

                            mPetDb.runInTransaction(() -> {
                                mFeedDao.updateFeedId(temporaryFeedId, newFeedResponse.getFeedId());
                                newFeedResponse.setStatus(FeedEntity.STATUS_DONE);

                                if (currentPaging != null) {
                                    mFeedDao.update(currentPaging);
                                }
                                if (userPaging != null) {
                                    mFeedDao.update(userPaging);
                                }
                                mFeedDao.update(newFeedResponse.toEntity());
                            });
                            jobFinished(mParams, false);
                        });
                    } else {
                        Timber.e("createFeed error %s", feedApiResponse.errorMessage);
                        updateLocalFeedError(feedResponse);
                        jobFinished(mParams, true);
                    }
                    apiResponse.removeObserver(this);
                }
            }
        });
    }

    private void updateFeed(FeedResponse feedResponse) {
        LiveData<ApiResponse<FeedResponse>> apiResponse = mWebService.updateFeed(feedResponse);
        apiResponse.observeForever(new Observer<ApiResponse<FeedResponse>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<FeedResponse> feedApiResponse) {
                if (feedApiResponse != null) {
                    if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                        Timber.i("upload mFeedResponse succeed %s", feedApiResponse.body.toString());

                        FeedResponse newFeedResponse = feedApiResponse.body;
                        newFeedResponse.setStatus(FeedEntity.STATUS_DONE);

                        mAppExecutors.diskIO().execute(() -> {
                            mFeedDao.update(newFeedResponse.toEntity());
                            jobFinished(mParams, false);
                        });
                    } else {
                        Timber.e("createFeed error %s", feedApiResponse.errorMessage);
                        updateLocalFeedError(feedResponse);
                        jobFinished(mParams, true);
                    }
                    apiResponse.removeObserver(this);
                }
            }
        });
    }

    private void updateLocalFeedError(FeedResponse feedResponse) {
        feedResponse.setStatus(FeedEntity.STATUS_ERROR);
        mAppExecutors.diskIO().execute(() -> {
            mFeedDao.update(feedResponse.toEntity());
            jobFinished(mParams, true);
        });
    }
}
