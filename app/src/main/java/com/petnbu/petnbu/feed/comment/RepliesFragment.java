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
import com.petnbu.petnbu.databinding.FragmentRepliesCommentsBinding;
import com.petnbu.petnbu.model.Photo;

public class RepliesFragment extends Fragment {

    private static final String EXTRA_COMMENT_ID = "extra_comment_id";

    private FragmentRepliesCommentsBinding mBinding;
    private CommentsViewModel mCommentsViewModel;

    private RepliesRecyclerViewAdapter mAdapter;
    private RequestManager mRequestManager;
    private String mCommentId;

    public static RepliesFragment newInstance(String commentId) {
        RepliesFragment repliesFragment = new RepliesFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_COMMENT_ID, commentId);
        repliesFragment.setArguments(args);
        return repliesFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mCommentId = getArguments().getString(EXTRA_COMMENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_replies_comments, container, false);
        initialize();
        return mBinding.getRoot();
    }

    private void initialize() {
        mRequestManager = Glide.with(this);
        mCommentsViewModel = ViewModelProviders.of(getActivity()).get(CommentsViewModel.class);
        mBinding.setViewModel(mCommentsViewModel);

        mCommentsViewModel.loadSubComments(mCommentId).observe(this, commentsResource -> {
            if(commentsResource.data != null) {
                mAdapter.setComments(commentsResource.data);
            }
        });

        mBinding.rvComments.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new RepliesRecyclerViewAdapter(null, mCommentId, mRequestManager, new RepliesRecyclerViewAdapter.OnItemClickListener() {
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

            }
        });
        mBinding.rvComments.setAdapter(mAdapter);
    }
}
