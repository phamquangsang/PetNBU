package com.petnbu.petnbu.feed.comment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.petnbu.petnbu.BaseActivity;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ActivityCommentsBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.userprofile.UserProfileActivity;

public class CommentsActivity extends BaseActivity {

    private static final String EXTRA_FEED_ID = "extra_feed_id";

    private ActivityCommentsBinding mBinding;
    private CommentsViewModel mCommentsViewModel;
    private CommentsFragment mCommentsFragment;
    private String mFeedId;
    private boolean mCameraClicked = false;
    private Photo mSelectedPhoto;

    public static Intent newIntent(Context context, String feedId) {
        Intent intent = new Intent(context, CommentsActivity.class);
        intent.putExtra(EXTRA_FEED_ID, feedId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_comments);
        initialize();
    }

    private void initialize() {
        setSupportActionBar(mBinding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mCommentsViewModel = ViewModelProviders.of(this).get(CommentsViewModel.class);
        mCommentsViewModel.loadUserInfo().observe(this, userEntity -> {
            if(userEntity == null)
                finish();
        });
        mCommentsViewModel.getOpenRepliesEvent().observe(this, this::showRepliesForComment);
        mCommentsViewModel.getOpenUserProfileEvent().observe(this, this::showUserProfile);

        mFeedId = getIntent() != null ? getIntent().getStringExtra(EXTRA_FEED_ID) : "";
        if(!TextUtils.isEmpty(mFeedId)) {
            mCommentsFragment = CommentsFragment.newInstance(mFeedId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, mCommentsFragment, CommentsFragment.class.getSimpleName())
                    .commit();
        } else {
            finish();
        }

        mBinding.imgCamera.setOnClickListener(v -> {
            mCameraClicked = true;
            checkToRequestReadExternalPermission();
        });
        mBinding.edText.addTextChangedListener(new TextWatcher() {
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
        mBinding.tvPost.setOnClickListener(v -> doPost());
    }

    private void checkToRequestReadExternalPermission() {
        if (!requestReadExternalPermission()) {
            if (mCameraClicked) {
                openPhotoGallery(false);
                mCameraClicked = false;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_PERMISSIONS:
                if (mCameraClicked) {
                    openPhotoGallery(false);
                    mCameraClicked = false;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_INTENT_CALLED || requestCode == GALLERY_KITKAT_INTENT_CALLED) {
            if (resultCode == RESULT_OK) {
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    requestPersistablePermission(data, uri);

                    mSelectedPhoto = new Photo();
                    mSelectedPhoto.setOriginUrl(uri.toString());
                    showSelectedPhoto();
                    checkToEnablePostMenu();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkToEnablePostMenu() {
        mBinding.tvPost.setEnabled(!TextUtils.isEmpty(mBinding.edText.getText().toString().trim()) || mSelectedPhoto != null);
    }

    private void showSelectedPhoto() {
        mBinding.layoutSelectedPhoto.setVisibility(View.VISIBLE);
        mBinding.imgRemoveSelectedPhoto.setOnClickListener(v -> {
            mSelectedPhoto = null;
            mBinding.layoutSelectedPhoto.setVisibility(View.GONE);
            mBinding.imgSelectedPhoto.setImageDrawable(null);
            checkToEnablePostMenu();
        });
        Glide.with(this)
                .load(mSelectedPhoto.getOriginUrl())
                .apply(RequestOptions.centerInsideTransform())
                .into(mBinding.imgSelectedPhoto);
    }

    private void doPost() {
        String content = mBinding.edText.getText().toString().trim();
        mCommentsViewModel.sendComment(mFeedId, content, mSelectedPhoto);

        mBinding.edText.getText().clear();
        mBinding.layoutSelectedPhoto.setVisibility(View.GONE);
        mBinding.imgSelectedPhoto.setImageDrawable(null);
        mSelectedPhoto = null;
    }

    private void showRepliesForComment(String commentId) {
        getSupportFragmentManager()
                .beginTransaction()
                .hide(mCommentsFragment)
                .add(R.id.fragmentContainer, RepliesFragment.newInstance(commentId), RepliesFragment.class.getSimpleName())
                .addToBackStack(null)
                .commit();
    }

    private void showUserProfile(String userId) {
        Intent i = UserProfileActivity.newIntent(this, userId);
        startActivity(i);
    }
}
