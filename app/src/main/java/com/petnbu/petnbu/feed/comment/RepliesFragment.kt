package com.petnbu.petnbu.feed.comment

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.FragmentRepliesCommentsBinding
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.NavigationUtils
import com.petnbu.petnbu.util.PermissionUtils
import com.petnbu.petnbu.util.SnackbarUtils
import com.petnbu.petnbu.util.Utils

class RepliesFragment : Fragment() {

    private lateinit var mBinding: FragmentRepliesCommentsBinding
    private lateinit var commentsViewModel: CommentsViewModel

    private lateinit var repliesRecyclerViewAdapter: RepliesRecyclerViewAdapter
    private var commentId: String = ""
    private var isCameraClicked = false
    private var selectedPhoto: Photo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.run {
            commentId = getString(EXTRA_COMMENT_ID) ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_replies_comments, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        activity?.run {
            commentsViewModel = ViewModelProviders.of(this).get(CommentsViewModel::class.java)
            mBinding.viewModel = commentsViewModel

            repliesRecyclerViewAdapter = RepliesRecyclerViewAdapter(this, commentId, object : RepliesRecyclerViewAdapter.OnItemClickListener {

                override fun onPhotoClicked(photo: Photo) {}

                override fun onLikeClicked(commentId: String) {
                    commentsViewModel.likeSubCommentClicked(commentId)
                }
            }, commentsViewModel)
        }
        commentsViewModel.loadSubComments(commentId).observe(this, Observer { comments -> repliesRecyclerViewAdapter.submitList(comments) })
        commentsViewModel.subCommentLoadMoreState.observe(this, Observer { loadMoreState ->
            loadMoreState?.run {
                mBinding.rvComments.post { repliesRecyclerViewAdapter.addLoadMore = isRunning }

                errorMessageIfNotHandled?.run {
                    SnackbarUtils.showSnackbar(mBinding.layoutRoot, this)
                }
            }
        })

        mBinding.rvComments.layoutManager = LinearLayoutManager(activity)
        mBinding.rvComments.adapter = repliesRecyclerViewAdapter
        mBinding.rvComments.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                (recyclerView.layoutManager as? LinearLayoutManager)?.run {
                    if (findLastVisibleItemPosition() >= repliesRecyclerViewAdapter.itemCount - 2 && repliesRecyclerViewAdapter.itemCount > 0)
                        commentsViewModel.loadSubCommentsNextPage(commentId)
                }
            }
        })

        mBinding.layoutInputComment.imgCamera.setOnClickListener { _ ->
            isCameraClicked = true
            checkToRequestReadExternalPermission()
        }
        mBinding.layoutInputComment.edText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                checkToEnablePostMenu()
            }
        })
        mBinding.layoutInputComment.tvPost.setOnClickListener { _ -> doPost() }
    }

    private fun checkToRequestReadExternalPermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!PermissionUtils.requestPermissions(this, REQUEST_READ_EXTERNAL_PERMISSIONS, *permissions)) {
            if (isCameraClicked) {
                NavigationUtils.openPhotoGallery(this, false, OPEN_GALLERY_REQUEST_CODE)
                isCameraClicked = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPEN_GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.run photoUri@ {
                    PermissionUtils.requestPersistablePermission(activity, data, this)

                    Utils.getBitmapSize(activity, this)?.run {
                        selectedPhoto = Photo().apply {
                            originUrl = this@photoUri.toString()
                            width = outWidth
                            height = outHeight
                        }
                        showSelectedPhoto()
                        checkToEnablePostMenu()
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_PERMISSIONS -> if (isCameraClicked) {
                NavigationUtils.openPhotoGallery(this, false, OPEN_GALLERY_REQUEST_CODE)
                isCameraClicked = false
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkToEnablePostMenu() {
        mBinding.layoutInputComment.tvPost.isEnabled = !mBinding.layoutInputComment.edText.text.toString().trim().isNullOrEmpty() || selectedPhoto != null
    }

    private fun showSelectedPhoto() {
        mBinding.layoutInputComment.layoutSelectedPhoto.visibility = View.VISIBLE
        mBinding.layoutInputComment.imgRemoveSelectedPhoto.setOnClickListener { _ ->
            selectedPhoto = null
            mBinding.layoutInputComment.layoutSelectedPhoto.visibility = View.GONE
            mBinding.layoutInputComment.imgSelectedPhoto.setImageDrawable(null)
            checkToEnablePostMenu()
        }

        activity?.run activity@ {
            selectedPhoto?.run {
                GlideApp.with(this@activity)
                        .load(originUrl)
                        .centerInside()
                        .into(mBinding.layoutInputComment.imgSelectedPhoto)
            }
        }
    }

    private fun doPost() {
        val content = mBinding.layoutInputComment.edText.text.toString().trim()
        commentsViewModel.sendCommentByComment(commentId, content, selectedPhoto)

        mBinding.layoutInputComment.edText.text.clear()
        mBinding.layoutInputComment.layoutSelectedPhoto.visibility = View.GONE
        mBinding.layoutInputComment.imgSelectedPhoto.setImageDrawable(null)
        selectedPhoto = null
        mBinding.rvComments.scrollToPosition(0)
        checkToEnablePostMenu()
    }

    companion object {
        private const val EXTRA_COMMENT_ID = "extra_comment_id"
        private const val REQUEST_READ_EXTERNAL_PERMISSIONS = 1
        private const val OPEN_GALLERY_REQUEST_CODE = 1

        fun newInstance(commentId: String): RepliesFragment {
            return RepliesFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_COMMENT_ID, commentId)
                }
            }
        }
    }
}
