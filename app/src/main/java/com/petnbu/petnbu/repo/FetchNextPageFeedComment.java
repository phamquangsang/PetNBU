package com.petnbu.petnbu.repo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;

import java.util.ArrayList;
import java.util.List;

public class FetchNextPageFeedComment implements Runnable{

    //data boolean return if new feed has more item or not
    private final MutableLiveData<Resource<Boolean>> mLiveData = new MutableLiveData<>();
    private final PetDb mPetDb;
    private final WebService mWebService;
    private final AppExecutors mAppExecutors;
    private final String mPagingId;
    private String mFeedId;

    public FetchNextPageFeedComment(String feedId ,String pagingId, WebService webService, PetDb petDb, AppExecutors appExecutors) {
        mPagingId = pagingId;
        mFeedId = feedId;
        mWebService = webService;
        mPetDb = petDb;
        mAppExecutors = appExecutors;
    }

    @Override
    public void run() {
        Paging currentPaging = mPetDb.pagingDao().findFeedPaging(mPagingId);

        if (currentPaging == null || currentPaging.isEnded() || currentPaging.getOldestId() == null) {
            mLiveData.postValue(new Resource<>(Status.SUCCESS, false, null));
            return;
        }

        mLiveData.postValue(new Resource<>(Status.LOADING, null, null));
        LiveData<ApiResponse<List<Comment>>> result =
                mWebService.getCommentsPaging(mFeedId, currentPaging.getOldestId(), CommentRepository.COMMENT_PER_PAGE);
        result.observeForever(new Observer<ApiResponse<List<Comment>>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<List<Comment>> listApiResponse) {
                if (listApiResponse != null) {
                    result.removeObserver(this);
                    if (listApiResponse.isSucceed) {
                        if (listApiResponse.body != null && listApiResponse.body.size() > 0) {
                            List<String> ids = new ArrayList<>(currentPaging.getIds());
                            for (Comment item : listApiResponse.body) {
                                ids.add(item.getId());
                            }
                            Paging newPaging = new Paging(mPagingId, ids, false, ids.get(ids.size() - 1));
                            mAppExecutors.diskIO().execute(() -> {
                                mPetDb.runInTransaction(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPetDb.commentDao().insertListComment(listApiResponse.body);
                                        for (Comment item :
                                                listApiResponse.body) {
                                            mPetDb.userDao().insert(item.getFeedUser());
                                            mPetDb.commentDao().insertFromComment(item.getLatestComment());
                                        }
                                        mPetDb.pagingDao().insert(newPaging);
                                    }
                                });

                            });
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, true, null));
                        } else {
                            currentPaging.setEnded(true);
                            mAppExecutors.diskIO().execute(() -> mPetDb.pagingDao().update(currentPaging));
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, false, null));
                        }
                    } else {
                        mLiveData.postValue(new Resource<>(Status.ERROR, true, listApiResponse.errorMessage));
                    }
                }
            }
        });
    }

    public MutableLiveData<Resource<Boolean>> getLiveData() {
        return mLiveData;
    }

}
