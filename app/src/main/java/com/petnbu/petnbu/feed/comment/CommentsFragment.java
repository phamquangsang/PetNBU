package com.petnbu.petnbu.feed.comment;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentCommentsBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.NavigationUtils;
import com.petnbu.petnbu.util.PermissionUtils;
import com.petnbu.petnbu.util.SnackbarUtils;
import com.petnbu.petnbu.util.Utils;

public class CommentsFragment extends Fragment {

    private static final String EXTRA_FEED_ID = "extra_feed_id";
    private final int REQUEST_READ_EXTERNAL_PERMISSIONS = 1;
    private final int OPEN_GALLERY_REQUEST_CODE = 1;

    private FragmentCommentsBinding mBinding;
    private CommentsViewModel mCommentsViewModel;

    private CommentsRecyclerViewAdapter mAdapter;
    private String mFeedId;
    private boolean mCameraClicked = false;
    private Photo mSelectedPhoto;

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
        checkToRequestReadExternalPermission();
        return mBinding.getRoot();
    }

    private void initialize() {
        mCommentsViewModel = ViewModelProviders.of(getActivity()).get(CommentsViewModel.class);
        mBinding.setViewModel(mCommentsViewModel);

        mCommentsViewModel.loadComments(mFeedId).observe(this, comments -> mAdapter.setComments(comments));
        mCommentsViewModel.getCommentLoadMoreState().observe(this, loadMoreState -> {
            if (loadMoreState != null) {
                mBinding.rvComments.post(() -> mAdapter.setAddLoadMore(loadMoreState.isRunning()));

                String errorMessage = loadMoreState.getErrorMessageIfNotHandled();
                if (errorMessage != null) {
                    SnackbarUtils.showSnackbar(mBinding.layoutRoot, errorMessage);
                }
            }
        });

        mBinding.rvComments.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new CommentsRecyclerViewAdapter(getActivity(), null, mFeedId, mCommentsViewModel);
        mBinding.rvComments.setAdapter(mAdapter);
        mBinding.rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager)
                        recyclerView.getLayoutManager();
                int lastPosition = layoutManager
                        .findLastVisibleItemPosition();
                if (lastPosition >= mAdapter.getItemCount() - 2 && mAdapter.getItemCount() > 0) {
                    mCommentsViewModel.loadNextPage(mFeedId);
                }
            }
        });

        mBinding.layoutInputComment.imgCamera.setOnClickListener(v -> {
            mCameraClicked = true;
            checkToRequestReadExternalPermission();
        });
        mBinding.layoutInputComment.edText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkToEnablePostMenu();
            }
        });
        mBinding.layoutInputComment.tvPost.setOnClickListener(v -> doPost());
    }

    private void checkToRequestReadExternalPermission() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!PermissionUtils.requestPermissions(this, REQUEST_READ_EXTERNAL_PERMISSIONS, permissions)) {
            if (mCameraClicked) {
                NavigationUtils.openPhotoGallery(this, false, OPEN_GALLERY_REQUEST_CODE);
                mCameraClicked = false;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPEN_GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    PermissionUtils.requestPersistablePermission(getActivity(), data, uri);

                    BitmapFactory.Options options = Utils.getBitmapSize(getActivity(), uri);
                    mSelectedPhoto = new Photo();
                    mSelectedPhoto.setOriginUrl(uri.toString());
                    mSelectedPhoto.setWidth(options.outWidth);
                    mSelectedPhoto.setHeight(options.outHeight);

                    showSelectedPhoto();
                    checkToEnablePostMenu();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_PERMISSIONS:
                if (mCameraClicked) {
                    NavigationUtils.openPhotoGallery(this, false, OPEN_GALLERY_REQUEST_CODE);
                    mCameraClicked = false;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void checkToEnablePostMenu() {
        mBinding.layoutInputComment.tvPost.setEnabled(!TextUtils.isEmpty(mBinding.layoutInputComment.edText.getText().toString().trim())
                || mSelectedPhoto != null);
    }

    private void showSelectedPhoto() {
        mBinding.layoutInputComment.layoutSelectedPhoto.setVisibility(View.VISIBLE);
        mBinding.layoutInputComment.imgRemoveSelectedPhoto.setOnClickListener(v -> {
            mSelectedPhoto = null;
            mBinding.layoutInputComment.layoutSelectedPhoto.setVisibility(View.GONE);
            mBinding.layoutInputComment.imgSelectedPhoto.setImageDrawable(null);
            checkToEnablePostMenu();
        });
        GlideApp.with(getActivity())
                .load(mSelectedPhoto.getOriginUrl())
                .apply(RequestOptions.centerInsideTransform())
                .into(mBinding.layoutInputComment.imgSelectedPhoto);
    }

    private void doPost() {
        String content = mBinding.layoutInputComment.edText.getText().toString().trim();
        mCommentsViewModel.sendComment(mFeedId, content, mSelectedPhoto);

        mBinding.layoutInputComment.edText.getText().clear();
        mBinding.layoutInputComment.layoutSelectedPhoto.setVisibility(View.GONE);
        mBinding.layoutInputComment.imgSelectedPhoto.setImageDrawable(null);
        mSelectedPhoto = null;
        mBinding.rvComments.scrollToPosition(0);
        checkToEnablePostMenu();
    }
}
