package com.petnbu.petnbu.feed.comment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ActivityCommentsBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.userprofile.UserProfileActivity;

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
        getWindow().setBackgroundDrawable(null);
        initialize();
    }

    private void initialize() {
        setSupportActionBar(mBinding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mCommentsViewModel = ViewModelProviders.of(this).get(CommentsViewModel.class);
        mCommentsViewModel.loadUserInfo().observe(this, userEntity -> {
            if (userEntity == null)
                finish();
        });
        mCommentsViewModel.getOpenRepliesEvent().observe(this, this::showRepliesForComment);
        mCommentsViewModel.getOpenUserProfileEvent().observe(this, this::showUserProfile);

        mFeedId = getIntent() != null ? getIntent().getStringExtra(EXTRA_FEED_ID) : "";
        if (!TextUtils.isEmpty(mFeedId)) {
            mCommentsFragment = CommentsFragment.newInstance(mFeedId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, mCommentsFragment, CommentsFragment.class.getSimpleName())
                    .commit();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
