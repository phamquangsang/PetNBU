package com.petnbu.petnbu.repo;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.UserEntity;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class UserRepository {

    private final PetDb mPetDb;

    private final UserDao mUserDao;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    @Inject
    public UserRepository(PetDb petDb, UserDao userDao, AppExecutors appExecutors, WebService webService) {
        mPetDb = petDb;
        mUserDao = userDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
    }

    public LiveData<Resource<UserEntity>> getUserById(@NonNull String id){
        Timber.i("getUserById: %s", id);
        return new NetworkBoundResource<UserEntity, UserEntity>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull UserEntity item) {
                mUserDao.insert(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable UserEntity data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<UserEntity> loadFromDb() {
                return mUserDao.findLiveUserById(id);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<UserEntity>> createCall() {
                return mWebService.getUser(id);
            }

            @Override
            protected void deleteDataFromDb(UserEntity body) {

            }
        }.asLiveData();
    }
}
