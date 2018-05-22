package com.petnbu.petnbu.feed;

import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.petnbu.petnbu.BaseActivity;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.util.Utils;
import com.petnbu.petnbu.databinding.ActivityCreateFeedBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ColorUtils;
import com.petnbu.petnbu.views.HorizontalSpaceItemDecoration;

import java.util.ArrayList;

import timber.log.Timber;

public class CreateFeedActivity extends BaseActivity {

    public static final String EXTRA_EDIT_FEED_ID = "EDIT_FEED_ID";

    private ActivityCreateFeedBinding mBinding;
    private CreateEditFeedViewModel mCreateEditFeedViewModel;
    private RequestManager mRequestManager;

    private ProgressDialog mProgressDialog;
    private MenuItem mPostMenuItem;

    private PhotosAdapter mPhotosAdapter;
    private ArrayList<Photo> mSelectedPhotos = new ArrayList<>();
    private boolean mCameraClicked = false;
    private String mPostMenuTitle;

    public static Intent newIntent(Context context, String feedId) {
        Intent intent = new Intent(context, CreateFeedActivity.class);
        intent.putExtra(EXTRA_EDIT_FEED_ID, feedId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_feed);
        initialize();
    }

    private void initialize() {
        setSupportActionBar(mBinding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        String feedId = "";

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_EDIT_FEED_ID)) {
            feedId = getIntent().getStringExtra(EXTRA_EDIT_FEED_ID);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRequestManager = Glide.with(this);
        mCreateEditFeedViewModel = ViewModelProviders.of(this).get(CreateEditFeedViewModel.class);
        mCreateEditFeedViewModel.loadFeed(feedId).observe(this, feed -> {
            if (feed != null) {
                mSelectedPhotos.addAll(feed.getPhotos());
                mPhotosAdapter.notifyDataSetChanged();
                mBinding.edText.setText(feed.getContent());
            }
            mPostMenuTitle = feed != null ? getString(R.string.menu_action_save_title) :
                    getString(R.string.menu_action_create_title);
        });
        mCreateEditFeedViewModel.showLoadingEvent.observe(this, this::setLoadingVisibility);
        mCreateEditFeedViewModel.showMessageDialogEvent.observe(this, message -> Log.d("WTF", message));

        setPlaceHolderLayoutVisibility(true);
        mCreateEditFeedViewModel.loadUserInfo().observe(this, user -> {
            if (user != null) {
                Timber.i("user : %s", user.toString());
                mRequestManager.asBitmap()
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
                                getView().setImageBitmap(resource);
                            }
                        });
                mBinding.tvUserName.setText(user.getName());
                setPlaceHolderLayoutVisibility(false);
            } else {
                Timber.i("user is null");
            }
        });
        checkToRequestReadExternalPermission();

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

        int imageSize = Utils.getDeviceWidth(this) * 9 / 16;
        mPhotosAdapter = new PhotosAdapter(mRequestManager, mSelectedPhotos, new PhotosAdapter.ItemClickListener() {
            @Override
            public void onCameraIconClicked() {
                mCameraClicked = true;
                checkToRequestReadExternalPermission();
            }

            @Override
            public void onPhotoClicked(Photo photo) {

            }

            @Override
            public void onRemovePhotoClicked(int position) {
                mPhotosAdapter.removeItem(position);
                checkToEnablePostMenu();
            }
        }, imageSize);

        mBinding.rvMedia.addItemDecoration(new HorizontalSpaceItemDecoration((int) Utils.convertDpToPixel(this, 4)));
        mBinding.rvMedia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.rvMedia.setAdapter(mPhotosAdapter);
        mBinding.rvMedia.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mBinding.rvMedia.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mBinding.rvMedia.getLayoutParams();
                int minHeight = mBinding.edText.getTop() + mBinding.rvMedia.getTop() - mBinding.edText.getBottom()
                        - (mBinding.edText.getPaddingBottom() + mBinding.edText.getPaddingTop());
                mBinding.edText.setMinHeight(minHeight);
                layoutParams.topToBottom = mBinding.edText.getId();
                mBinding.rvMedia.setLayoutParams(layoutParams);
            }
        });
    }

    private void checkToRequestReadExternalPermission() {
        if (!requestReadExternalPermission()) {
            if (mCameraClicked) {
                openPhotoGallery(true);
                mCameraClicked = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_post, menu);
        mPostMenuItem = menu.findItem(R.id.action_post);
        mPostMenuItem.setTitle(mPostMenuTitle);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        checkToEnablePostMenu();
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_post) {
            mCreateEditFeedViewModel.saveFeed(mBinding.edText.getText().toString().trim(), mSelectedPhotos);
            finish();

        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_PERMISSIONS:
                if (mCameraClicked) {
                    openPhotoGallery(true);
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

                    Photo photo = new Photo();
                    photo.setOriginUrl(uri.toString());
                    mSelectedPhotos.add(photo);
                    mPhotosAdapter.notifyItemInserted(mSelectedPhotos.size() - 1);
                } else {
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            requestPersistablePermission(data, uri);

                            Photo photo = new Photo();
                            photo.setOriginUrl(uri.toString());
                            mSelectedPhotos.add(photo);
                        }
                        mPhotosAdapter.notifyItemRangeInserted(mSelectedPhotos.size() - clipData.getItemCount(),
                                clipData.getItemCount());
                    }
                }
            }
            checkToEnablePostMenu();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkToEnablePostMenu() {
        Utils.enableMenuItem(this, mPostMenuItem, !mPhotosAdapter.getPhotos().isEmpty());
    }

    private void setLoadingVisibility(boolean visible) {
        if (visible) {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                FadingCircle fadingCircle = new FadingCircle();
                fadingCircle.setColor(Color.BLACK);
                mProgressDialog.setIndeterminateDrawable(fadingCircle);
            }
            if (!mProgressDialog.isShowing()) {
                mProgressDialog.setMessage("Loading...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }
        } else {
            if (mProgressDialog != null) {
                mProgressDialog.setCancelable(true);
                mProgressDialog.dismiss();
            }
        }
    }

    private void setPlaceHolderLayoutVisibility(boolean placeHolderLayoutVisibility) {
        if (placeHolderLayoutVisibility) {
            mBinding.tvUserName.setVisibility(View.GONE);
            mBinding.tvUserNamePlaceHolder.setVisibility(View.VISIBLE);

            mBinding.imgProfile.setCircleBackgroundColor(ContextCompat.getColor(this, R.color.placeholderBackground));
        } else {
            mBinding.tvUserName.setVisibility(View.VISIBLE);
            mBinding.tvUserNamePlaceHolder.setVisibility(View.GONE);

            mBinding.imgProfile.setCircleBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        }
    }
}
