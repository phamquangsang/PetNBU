package com.petnbu.petnbu.repo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.model.Feed
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Resource
import com.petnbu.petnbu.model.Status
import java.util.*

class FetchNextPageUserFeed(private val mPagingId: String, private val mWebService: WebService,
                            private val mPetDb: PetDb, private val mAppExecutors: AppExecutors) : Runnable {

    //data boolean return if new feed has more item or not
    val liveData = MutableLiveData<Resource<Boolean>>()

    override fun run() {
        val currentPaging = mPetDb.pagingDao().findFeedPaging(mPagingId)

        if (currentPaging == null || currentPaging.isEnded || currentPaging.oldestId == null) {
            liveData.postValue(Resource(Status.SUCCESS, false, null))
            return
        }

        liveData.postValue(Resource(Status.LOADING, null, null))
        val result = mWebService.getUserFeed(mPagingId, currentPaging.oldestId, FeedRepository.FEEDS_PER_PAGE)
        result.observeForever(object : Observer<ApiResponse<List<Feed>>> {
            override fun onChanged(listApiResponse: ApiResponse<List<Feed>>?) {
                if (listApiResponse != null) {
                    result.removeObserver(this)
                    if (listApiResponse.isSucceed) {
                        if (listApiResponse.body != null && listApiResponse.body.isNotEmpty()) {
                            val ids = listApiResponse.body.map { it.feedId }
                            val newPaging = Paging(mPagingId, ids, false, ids[ids.size - 1])
                            mAppExecutors.diskIO().execute {
                                mPetDb.runInTransaction({
                                    mPetDb.feedDao().insertFromFeedList(listApiResponse.body)
                                    for (item in listApiResponse.body) {
                                        mPetDb.userDao().insert(item.feedUser)
                                        item.latestComment?.apply {
                                            //the latestComment return from server does not have latestSubComment
                                            mPetDb.commentDao().insertIfNotExists(item.latestComment!!.toEntity())
                                            mPetDb.userDao().insert(item.latestComment!!.feedUser)
                                        }
                                    }
                                    mPetDb.pagingDao().insert(newPaging)
                                })

                            }
                            liveData.postValue(Resource(Status.SUCCESS, true, null))
                        } else {
                            currentPaging.isEnded = true
                            mAppExecutors.diskIO().execute { mPetDb.pagingDao().update(currentPaging) }
                            liveData.postValue(Resource(Status.SUCCESS, false, null))
                        }
                    } else {
                        liveData.postValue(Resource(Status.ERROR, true, listApiResponse.errorMessage))
                    }
                }
            }
        })
    }
}
