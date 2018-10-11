package com.petnbu.petnbu.repo

import android.arch.paging.PagedList
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.util.PagingRequestHelper
import java.util.concurrent.Executor

class FeedBoundaryCallback(private val webService: WebService,
                           private val ioExecutor: Executor) : PagedList.BoundaryCallback<FeedUI>() {

    val helper = PagingRequestHelper(ioExecutor)
//    val networkState = helper.createStatusLiveData()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
    }

    override fun onItemAtEndLoaded(itemAtEnd: FeedUI) {
        super.onItemAtEndLoaded(itemAtEnd)
    }

    override fun onItemAtFrontLoaded(itemAtFront: FeedUI) {
        super.onItemAtFrontLoaded(itemAtFront)
    }
}