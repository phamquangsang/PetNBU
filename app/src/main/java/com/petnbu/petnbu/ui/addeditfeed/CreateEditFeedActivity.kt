package com.petnbu.petnbu.ui.addeditfeed

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ActivityCreateFeedBinding
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.ColorUtils
import com.petnbu.petnbu.util.NavigationUtils
import com.petnbu.petnbu.util.PermissionUtils
import com.petnbu.petnbu.util.Utils
import com.petnbu.petnbu.views.HorizontalSpaceItemDecoration
import timber.log.Timber

class CreateEditFeedActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityCreateFeedBinding
    private lateinit var createEditFeedViewModel: CreateEditFeedViewModel
    private var postMenuItem: MenuItem? = null
    private lateinit var photosAdapter: PhotosAdapter

    private var isCameraClicked = false
    private var postMenuTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("onCreate : savedInstanceState = %s", savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_feed)
        window.setBackgroundDrawable(null)
        initialize(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SELECTED_PHOTOS, createEditFeedViewModel.selectedPhotos)
    }

    private fun initialize(savedInstanceState: Bundle?) {
        Timber.i("initialize: ")
        setSupportActionBar(mBinding.toolBar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        var feedId = ""

        val intent = intent
        if (intent != null && intent.hasExtra(EXTRA_EDIT_FEED_ID)) {
            feedId = getIntent().getStringExtra(EXTRA_EDIT_FEED_ID)
        }

        createEditFeedViewModel = ViewModelProviders.of(this).get(CreateEditFeedViewModel::class.java)
        mBinding.viewModel = createEditFeedViewModel

        if (savedInstanceState != null && createEditFeedViewModel.selectedPhotos.isEmpty()) {
            val previousPhotos = savedInstanceState.getParcelableArrayList<Photo>(SELECTED_PHOTOS)
            createEditFeedViewModel.selectedPhotos.clear()
            createEditFeedViewModel.selectedPhotos.addAll(previousPhotos)

        }
        createEditFeedViewModel.getFeed(feedId).observe(this, Observer { feed ->
            if (feed != null && savedInstanceState == null) {
                createEditFeedViewModel.selectedPhotos.addAll(feed.photos)
                photosAdapter.notifyDataSetChanged()
                mBinding.edText.setText(feed.content)
            }
            postMenuTitle = if (feed != null)
                getString(R.string.menu_action_save_title)
            else
                getString(R.string.menu_action_create_title)
        })

        setPlaceHolderLayoutVisibility(true)
        createEditFeedViewModel.loadUserInfo().observe(this, Observer { user ->
            if (user != null) {
                Timber.i("user : %s", user.toString())
                GlideApp.with(this).asBitmap()
                        .load(user.avatar.originUrl)
                        .centerInside()
                        .into(object : BitmapImageViewTarget(mBinding.imgProfile) {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                val context = mBinding.imgProfile.context
                                if (ColorUtils.isDark(resource)) {
                                    mBinding.imgProfile.borderWidth = 0
                                } else {
                                    mBinding.imgProfile.borderColor = ContextCompat.getColor(context, android.R.color.darker_gray)
                                    mBinding.imgProfile.borderWidth = 1
                                }
                                getView().setImageBitmap(resource)
                            }
                        })
                mBinding.tvUserName.text = user.name
                setPlaceHolderLayoutVisibility(false)
            } else {
                Timber.i("user is null")
            }
        })
        checkToRequestReadExternalPermission()

        mBinding.edText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                checkToEnablePostMenu()
            }
        })

        val imageSize = Utils.getDeviceWidth(this) * 9 / 16
        photosAdapter = PhotosAdapter(this, createEditFeedViewModel.selectedPhotos, object : PhotosAdapter.ItemClickListener {

            override fun onCameraIconClicked() {
                isCameraClicked = true
                checkToRequestReadExternalPermission()
            }

            override fun onPhotoClicked(photo: Photo) {}

            override fun onRemovePhotoClicked(position: Int) {
                photosAdapter.removeItem(position)
                checkToEnablePostMenu()
            }
        }, imageSize)

        mBinding.rvMedia.addItemDecoration(HorizontalSpaceItemDecoration(Utils.convertDpToPixel(this, 4f).toInt()))
        mBinding.rvMedia.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mBinding.rvMedia.adapter = photosAdapter
        mBinding.rvMedia.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mBinding.rvMedia.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val minHeight = (mBinding.edText.top + mBinding.rvMedia.top - mBinding.edText.bottom
                        - (mBinding.edText.paddingBottom + mBinding.edText.paddingTop))
                mBinding.edText.minHeight = minHeight

                mBinding.rvMedia.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = mBinding.edText.id
                }
            }
        })
    }

    private fun checkToRequestReadExternalPermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!PermissionUtils.requestPermissions(this, REQUEST_READ_EXTERNAL_PERMISSIONS, *permissions)) {
            if (isCameraClicked) {
                NavigationUtils.openPhotoGallery(this, true, OPEN_GALLERY_REQUEST_CODE)
                isCameraClicked = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.create_post, menu)
        postMenuItem = menu.findItem(R.id.action_post)
        postMenuItem?.apply {
            title = postMenuTitle
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val result = super.onPrepareOptionsMenu(menu)
        checkToEnablePostMenu()
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_post) {
            createEditFeedViewModel.saveFeed(mBinding.edText.text.toString().trim(), createEditFeedViewModel.selectedPhotos)
            finish()

        } else if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_PERMISSIONS -> if (isCameraClicked) {
                NavigationUtils.openPhotoGallery(this, true, OPEN_GALLERY_REQUEST_CODE)
                isCameraClicked = false
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPEN_GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.run photoUri@ {
                    PermissionUtils.requestPersistablePermission(this@CreateEditFeedActivity, data, this)

                    Utils.getBitmapSize(this@CreateEditFeedActivity, this)?.run {
                        createEditFeedViewModel.selectedPhotos.add(Photo(originUrl = this@photoUri.toString()).apply {
                            width = outWidth
                            height = outHeight
                        })
                        photosAdapter.notifyItemInserted(createEditFeedViewModel.selectedPhotos.size - 1)
                    }
                } ?: data?.clipData?.run {
                    for (i in 0 until itemCount) {
                        val item = getItemAt(i)
                        PermissionUtils.requestPersistablePermission(this@CreateEditFeedActivity, data, item.uri)

                        Utils.getBitmapSize(this@CreateEditFeedActivity, item.uri)?.run {
                            createEditFeedViewModel.selectedPhotos.add(Photo(item.uri.toString()).apply {
                                width = outWidth
                                height = outHeight
                            })
                        }
                    }
                    photosAdapter.notifyItemRangeInserted(createEditFeedViewModel.selectedPhotos.size - itemCount, itemCount)
                }
            }
            checkToEnablePostMenu()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkToEnablePostMenu() {
        postMenuItem?.let {
            Utils.enableMenuItem(this, it, !photosAdapter.photos.isEmpty())
        }
    }

    private fun setPlaceHolderLayoutVisibility(placeHolderLayoutVisibility: Boolean) {
        mBinding.tvUserName.isVisible = !placeHolderLayoutVisibility
        mBinding.tvUserNamePlaceHolder.isVisible = placeHolderLayoutVisibility
        mBinding.imgProfile.circleBackgroundColor = ContextCompat.getColor(this,
                if (placeHolderLayoutVisibility) R.color.placeholderBackground else android.R.color.transparent)
    }

    companion object {
        private const val EXTRA_EDIT_FEED_ID = "EDIT_FEED_ID"
        private const val SELECTED_PHOTOS = "selected-photos"
        private const val REQUEST_READ_EXTERNAL_PERMISSIONS = 1
        private const val OPEN_GALLERY_REQUEST_CODE = 1

        fun newIntent(context: Context, feedId: String): Intent {
            return Intent(context, CreateEditFeedActivity::class.java).apply {
                putExtra(EXTRA_EDIT_FEED_ID, feedId)
            }
        }
    }
}
