package com.petnbu.petnbu.repo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.CommentDao;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.Notification;
import com.petnbu.petnbu.model.NotificationUI;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.util.RateLimiter;
import com.petnbu.petnbu.util.Toaster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

public class NotificationRepository {
    public static final int NOTIFICATION_PER_PAGE = 10;

    private final PetDb mPetDb;


    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    final private Toaster mToaster;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.MINUTES);

    @Inject
    public NotificationRepository(PetDb petDb, AppExecutors appExecutors, WebService webService, Application application, Toaster toaster) {
        mPetDb = petDb;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
        mToaster = toaster;
    }

    public LiveData<Resource<List<NotificationUI>>> getUserNotifications(String userId, long after){
        return new NetworkBoundResource<List<NotificationUI>, List<Notification>>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull List<Notification> items) {
                List<String> listId = new ArrayList<>(items.size());
                String pagingId = Paging.notificationsPagingId();
                for (Notification item : items) {
                    listId.add(item.getId());
                }
                Paging paging = new Paging(pagingId,
                        listId, false,
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));
                mPetDb.runInTransaction(()->{
                    mPetDb.notificationDao().insertFromModels(items);
                    for (Notification noti : items) {
                        mPetDb.userDao().insert(noti.getFromUser());
                    }
                    mPetDb.pagingDao().insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<NotificationUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.notificationsPagingId());
            }

            @Override
            protected void deleteDataFromDb(List<Notification> body) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.notificationsPagingId());
            }

            @NonNull
            @Override
            protected LiveData<List<NotificationUI>> loadFromDb() {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(Paging.notificationsPagingId()), input -> {
                    if (input == null) {
                        MutableLiveData<List<NotificationUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mPetDb.notificationDao().getNotifcations(input.getIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Notification>>> createCall() {
                return mWebService.getNotifications(userId, after, NOTIFICATION_PER_PAGE);
            }
        }.asLiveData();
    }
}
