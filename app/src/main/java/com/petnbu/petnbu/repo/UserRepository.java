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
import com.petnbu.petnbu.model.User;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    public LiveData<Resource<User>> getUserById(@NonNull String id){
        return new NetworkBoundResource<User, User>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull User item) {
                mUserDao.insert(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable User data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<User> loadFromDb() {
                return mUserDao.findLiveUserById(id);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<User>> createCall() {
                return mWebService.getUser(id);
            }
        }.asLiveData();
    }
}
