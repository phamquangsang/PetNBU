package com.petnbu.petnbu.feed;


import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.databinding.FragmentFeedsBinding;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.util.RateLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class FeedsFragment extends Fragment {

    private FragmentFeedsBinding mBinding;
    private FeedsViewModel mFeedsViewModel;
    private FeedsRecyclerViewAdapter mAdapter;
    private RateLimiter<String> mLikeClickLimiter = new RateLimiter<>(1, TimeUnit.SECONDS);

    public FeedsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_feeds, container, false);
        initialize();
        return mBinding.getRoot();
    }

    public void initialize() {
        Activity activity = getActivity();
        if (activity != null) {
            mFeedsViewModel = ViewModelProviders.of(getActivity()).get(FeedsViewModel.class);
            mFeedsViewModel.getFeeds().observe(this, feeds -> {
                if (feeds.data != null) {
                    mAdapter.setFeeds(feeds.data);
                }
            });

            mFeedsViewModel.getLoadMoreState().observe(this, state -> {
                Timber.i(state != null ? state.toString() : "null");
                if (state != null) {
                    if (state.isRunning()) {
                        mBinding.progressBar.setVisibility(View.VISIBLE);
                    } else {
                        mBinding.progressBar.setVisibility(View.GONE);
                    }
                    String errorMessage = state.getErrorMessageIfNotHandled();
                    if(errorMessage != null){
                        Snackbar.make(mBinding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        }

        mAdapter = new FeedsRecyclerViewAdapter(getContext(), new ArrayList<>(), new FeedsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onProfileClicked(String userId) {

            }

            @Override
            public void onPhotoClicked(Photo photo) {

            }

            @Override
            public void onLikeClicked(String feedId) {
                 if(mLikeClickLimiter.shouldFetch(feedId)){
                     Timber.i("like clicked");
                 }
            }

            @Override
            public void onCommentClicked(String feedId) {

            }
        });
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mBinding.rvFeeds.setLayoutManager(linearLayoutManager);
        mBinding.rvFeeds.post(() -> {
            int maxHeight = mBinding.rvFeeds.getHeight();
            mAdapter.setMaxPhotoHeight(maxHeight - (Utils.getToolbarHeight(getContext()) * 3 / 2));
            mBinding.rvFeeds.setAdapter(mAdapter);
        });
        mBinding.rvFeeds.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    mBinding.fabNewPost.hide();
                } else if (dy < 0) {
                    mBinding.fabNewPost.show();
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager)
                        recyclerView.getLayoutManager();
                int lastPosition = layoutManager
                        .findLastVisibleItemPosition();
                if (lastPosition == mAdapter.getItemCount() - 1) {
                    mFeedsViewModel.loadNextPage();
                }
            }
        });

        mBinding.pullToRefresh.setOnRefreshListener(() ->
        {
            LiveData<Resource<List<Feed>>> refreshing = mFeedsViewModel.refresh();
            refreshing.observe(FeedsFragment.this, new Observer<Resource<List<Feed>>>() {
                @Override
                public void onChanged(@Nullable Resource<List<Feed>> listResource) {
                    if (listResource != null && listResource.status != Status.LOADING) {
                        mBinding.pullToRefresh.setRefreshing(false);
                        refreshing.removeObserver(this);
                    }
                }
            });
        });

        mBinding.fabNewPost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateFeedActivity.class);
            startActivity(intent);
        });
    }
}
