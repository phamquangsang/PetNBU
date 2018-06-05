package com.petnbu.petnbu.ui.comment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ActivityCommentsBinding
import com.petnbu.petnbu.userprofile.UserProfileActivity

class CommentsActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityCommentsBinding
    private lateinit var commentsViewModel: CommentsViewModel
    private val commentsFragment: CommentsFragment by lazy { CommentsFragment.newInstance(feedId) }
    private val feedId: String
        get() = intent.getStringExtra(EXTRA_FEED_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_comments)
        window.setBackgroundDrawable(null)
        initialize()
    }

    private fun initialize() {
        setSupportActionBar(mBinding.toolBar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        commentsViewModel = ViewModelProviders.of(this).get(CommentsViewModel::class.java)
        commentsViewModel.loadUserInfo().observe(this, Observer { if (it == null) finish() })
        commentsViewModel.openRepliesEvent.observe(this, Observer { it?.run { showRepliesForComment(this) }})
        commentsViewModel.openUserProfileEvent.observe(this, Observer { it?.run { showUserProfile(this) }})

        if (!feedId.isNullOrEmpty()) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainer, commentsFragment, CommentsFragment::class.java.simpleName)
                    .commit()
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun showRepliesForComment(commentId: String) {
        supportFragmentManager
                .beginTransaction()
                .hide(commentsFragment)
                .add(R.id.fragmentContainer, RepliesFragment.newInstance(commentId), RepliesFragment::class.java.simpleName)
                .addToBackStack(null)
                .commit()
    }

    private fun showUserProfile(userId: String) {
        startActivity(UserProfileActivity.newIntent(this, userId))
    }

    companion object {
        private const val EXTRA_FEED_ID = "extra_feed_id"

        fun newIntent(context: Context, feedId: String): Intent {
            return Intent(context, CommentsActivity::class.java).apply {
                putExtra(EXTRA_FEED_ID, feedId)
            }
        }
    }
}
