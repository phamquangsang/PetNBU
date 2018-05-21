package com.petnbu.petnbu.feed.comment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ActivityCommentsBinding;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.util.ColorUtils;

public class CommentsActivity extends AppCompatActivity {

    private static final String EXTRA_FEED_ID = "extra_feed_id";

    private ActivityCommentsBinding mBinding;
    private CommentsViewModel mCommentsViewModel;
    private CommentsFragment mCommentsFragment;
    private String mFeedId;

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
        mCommentsViewModel.loadUserInfo().observe(this, this::checkToDisplayUserInfo);
        mCommentsViewModel.getOpenRepliesEvent().observe(this, this::showRepliesForComment);

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

        mBinding.edText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mBinding.tvPost.setEnabled(!TextUtils.isEmpty(mBinding.edText.getText().toString().trim()));
            }
        });
        mBinding.tvPost.setOnClickListener(v -> doPost());
    }

    private void checkToDisplayUserInfo(UserEntity user) {
        if (user != null) {
            Glide.with(this).asBitmap()
                    .load(user.getAvatar().getOriginUrl())
                    .apply(RequestOptions.centerCropTransform())
                    .into(new BitmapImageViewTarget(mBinding.imgProfile) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Context context = mBinding.imgProfile.getContext();
                            if (ColorUtils.isDark(resource)) {
                                mBinding.imgProfile.setBorderWidth(0);
                            } else {
                                mBinding.imgProfile.setBorderColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                                mBinding.imgProfile.setBorderWidth(1);
                            }
                            mBinding.imgProfile.setImageBitmap(resource);
                        }
                    });
        } else {
            finish();
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

    private void doPost() {
        String content = mBinding.edText.getText().toString().trim();
        mBinding.edText.getText().clear();
        mCommentsViewModel.sendComment(mFeedId, content, null);
    }

    private void showRepliesForComment(String commentId) {
        getSupportFragmentManager()
                .beginTransaction()
                .hide(mCommentsFragment)
                .add(R.id.fragmentContainer, RepliesFragment.newInstance(commentId), RepliesFragment.class.getSimpleName())
                .addToBackStack(null)
                .commit();
    }
}
