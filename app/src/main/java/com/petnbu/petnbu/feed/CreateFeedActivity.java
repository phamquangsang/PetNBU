package com.petnbu.petnbu.feed;

import android.Manifest;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.databinding.ActivityCreateFeedBinding;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ColorUtils;
import com.petnbu.petnbu.views.HorizontalSpaceItemDecoration;

import java.util.ArrayList;

import timber.log.Timber;

public class CreateFeedActivity extends AppCompatActivity {

    public static final int PICK_IMAGE = 1;
    private static final int REQUEST_READ_EXTERNAL_PERMISSIONS = 8;

    public static final int GALLERY_INTENT_CALLED = 3;
    public static final int GALLERY_KITKAT_INTENT_CALLED = 2;

    private ActivityCreateFeedBinding mBinding;
    private CreateFeedViewModel mCreateFeedViewModel;
    private RequestManager mRequestManager;

    private ProgressDialog mProgressDialog;
    private MenuItem postMenuItem;

    private PhotosAdapter mPhotosAdapter;
    private ArrayList<Photo> mSelectedPhotos = new ArrayList<>();
    private boolean cameraClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_feed);
        setUp();
    }

    private void setUp() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRequestManager = Glide.with(this);
        mCreateFeedViewModel = ViewModelProviders.of(this).get(CreateFeedViewModel.class);
        mCreateFeedViewModel.createdFeedLiveData.observe(this, success -> finish());
        mCreateFeedViewModel.showLoadingLiveData.observe(this, this::setLoadingVisibility);
        mCreateFeedViewModel.loadUserInfos().observe(this, user -> {
            if (user != null) {
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
            } else {
                finish();
            }
        });
        requestReadExternalPermission();

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
        mPhotosAdapter = new PhotosAdapter(this, mRequestManager, mSelectedPhotos, new PhotosAdapter.ItemClickListener() {
            @Override
            public void onCameraIconClicked() {
                cameraClicked = true;
                requestReadExternalPermission();
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

    protected boolean requestReadExternalPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_READ_EXTERNAL_PERMISSIONS);
            return true;
        } else {
            if (cameraClicked) {

                if (Build.VERSION.SDK_INT < 19) {
                    Intent intent = new Intent();
                    intent.setType("*/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, GALLERY_INTENT_CALLED);
                } else {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setType("*/*");
                    startActivityForResult(intent, GALLERY_KITKAT_INTENT_CALLED);
                }

                cameraClicked = false;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_post, menu);
        postMenuItem = menu.findItem(R.id.action_post);
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
            mCreateFeedViewModel.createFeed(mBinding.edText.getText().toString().trim(), mSelectedPhotos);
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
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (cameraClicked) {

                        if (Build.VERSION.SDK_INT < 19) {
                            Intent intent = new Intent();
                            intent.setType("*/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(intent, GALLERY_INTENT_CALLED);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("*/*");
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            startActivityForResult(intent, GALLERY_KITKAT_INTENT_CALLED);
                        }

                        cameraClicked = false;
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                if(data.getData() != null){
                    Uri uri = data.getData();
                    requestPersistablePermission(data, uri);
                    BitmapFactory.Options options = Utils.getBitmapSize(this, uri);

                    Photo photo = new Photo();
                    Timber.i("decodeUri: %s", uri.toString());
                    photo.setOriginUrl(uri.toString());
                    photo.setWidth(options.outWidth);
                    photo.setHeight(options.outHeight);
                    mSelectedPhotos.add(photo);
                    mPhotosAdapter.notifyItemInserted(mSelectedPhotos.size() - 1);
                }else{
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            requestPersistablePermission(data, uri);
                            BitmapFactory.Options options = Utils.getBitmapSize(this, uri);

                            Photo photo = new Photo();
                            photo.setOriginUrl(uri.toString());
                            photo.setWidth(options.outWidth);
                            photo.setHeight(options.outHeight);
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

    private void requestPersistablePermission(Intent data, Uri uri) {
        if (Build.VERSION.SDK_INT >= 19) {
            uri = data.getData();
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            try {
                if (uri != null) {
                    CreateFeedActivity.this.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } else {
                    //todo notify user something wrong with selected photo
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkToEnablePostMenu() {
        Utils.enableMenuItem(this, postMenuItem, !mPhotosAdapter.getPhotos().isEmpty());
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
}
