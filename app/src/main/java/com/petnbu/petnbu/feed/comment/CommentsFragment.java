package com.petnbu.petnbu.feed.comment;

import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentCommentsBinding;
import com.petnbu.petnbu.model.Photo;

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
        if(getArguments() != null) {
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
            if(commentsResource.data != null) {
                mAdapter.setComments(commentsResource.data);
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
    }
}
