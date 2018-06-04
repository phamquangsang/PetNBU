package com.petnbu.petnbu.repo

import android.arch.lifecycle.LiveData
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.model.Resource
import com.petnbu.petnbu.model.UserEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject
constructor(private val mPetDb: PetDb, private val mAppExecutors: AppExecutors, private val mWebService: WebService) {

    fun getUserById(id: String): LiveData<Resource<UserEntity>> {
        Timber.i("getUserById: %s", id)
        return object : NetworkBoundResource<UserEntity, UserEntity>(mAppExecutors) {
            override fun saveCallResult(item: UserEntity) = mPetDb.userDao().insert(item)

            override fun shouldFetch(data: UserEntity?): Boolean = true

            override fun loadFromDb(): LiveData<UserEntity> = mPetDb.userDao().findLiveUserById(id)

            override fun createCall(): LiveData<ApiResponse<UserEntity>> = mWebService.getUser(id)

            override fun deleteDataFromDb(body: UserEntity?) {}
        }.asLiveData()
    }
}
