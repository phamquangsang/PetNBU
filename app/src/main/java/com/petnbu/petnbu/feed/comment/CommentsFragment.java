package com.petnbu.petnbu.feed.comment;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentCommentsBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.repo.LoadMoreState;

import timber.log.Timber;

public class CommentsFragment extends Fragment {

    private static final String EXTRA_FEED_ID = "extra_feed_id";

    private FragmentCommentsBinding mBinding;
    private CommentsViewModel mCommentsViewModel;

    private CommentsRecyclerViewAdapter mAdapter;
    private RequestManager mRequestManager;
    private String mFeedId;

    public static CommentsFragment newInstance(String feedId) {
        CommentsFragment commentsFragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_FEED_ID, feedId);
        commentsFragment.setArguments(args);
        return commentsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFeedId = getArguments().getString(EXTRA_FEED_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_comments, container, false);
        initialize();
        return mBinding.getRoot();
    }

    private void initialize() {
        mRequestManager = Glide.with(this);
        mCommentsViewModel = ViewModelProviders.of(getActivity()).get(CommentsViewModel.class);
        mBinding.setViewModel(mCommentsViewModel);

        mCommentsViewModel.loadComments(mFeedId).observe(this, commentsResource -> {
            if (commentsResource != null) {
                if (commentsResource.data != null) {
                    mAdapter.setComments(commentsResource.data);
                }
                if (commentsResource.status != Status.LOADING) {
                    //todo show hide progress bar
                }
            }

        });

        mCommentsViewModel.getLoadMoreState().observe(this, new Observer<LoadMoreState>() {
            @Override
            public void onChanged(@Nullable LoadMoreState loadMoreState) {
                if (loadMoreState != null) {
                    Timber.i("loadMore: %s", loadMoreState);
                    if (loadMoreState.isRunning()) {
                        //todo show progress bar
                    } else {
                        //todo hide progress bar;
                    }
                    String errorMessage = loadMoreState.getErrorMessageIfNotHandled();
                    if (errorMessage != null) {
                        Snackbar.make(mBinding.layoutRoot, errorMessage, Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });

        mBinding.rvComments.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new CommentsRecyclerViewAdapter(null, mFeedId, mRequestManager, new CommentsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onProfileClicked(String userId) {

            }

            @Override
            public void onPhotoClicked(Photo photo) {

            }

            @Override
            public void onLikeClicked(String commentId) {

            }

            @Override
            public void onReplyClicked(String commentId) {
                mCommentsViewModel.openRepliesEvent.setValue(commentId);
            }
        });
        mBinding.rvComments.setAdapter(mAdapter);
        mBinding.rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager)
                        recyclerView.getLayoutManager();
                int lastPosition = layoutManager
                        .findLastVisibleItemPosition();
                if (lastPosition >= mAdapter.getItemCount() - 2) {
                    mCommentsViewModel.loadNextPage(mFeedId);
                }
            }
        });
    }
}
