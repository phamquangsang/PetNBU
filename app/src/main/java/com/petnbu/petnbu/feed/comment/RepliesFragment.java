package com.petnbu.petnbu.feed.comment;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentRepliesCommentsBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Status;

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
            if(commentsResource != null){
                if(commentsResource.data != null){
                    mAdapter.setComments(commentsResource.data);
                }
                if(commentsResource.status == Status.LOADING){
                    //show loading
                }else{
                    //hide loading
                }
            }
        });

        mBinding.rvComments.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new RepliesRecyclerViewAdapter(getActivity(), null, mCommentId,
                new RepliesRecyclerViewAdapter.OnItemClickListener() {

            @Override
            public void onPhotoClicked(Photo photo) {

            }

            @Override
            public void onLikeClicked(String commentId) {

            }
        }, mCommentsViewModel);
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
                    mCommentsViewModel.loadSubCommentsNextPage(mCommentId);
                }
            }
        });
    }
}
