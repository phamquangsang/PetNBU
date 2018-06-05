package com.petnbu.petnbu.ui.user

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ActivityUserProfileBinding

class UserProfileActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)
        window.setBackgroundDrawable(null)

        val userId = intent.getStringExtra(ARG_USER_ID) ?: ""
        supportFragmentManager
                .beginTransaction()
                .add(R.id.layoutRoot, UserProfileFragment.newInstance(userId))
                .commit()
    }

    companion object {
        private const val ARG_USER_ID = "user-id"

        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, UserProfileActivity::class.java).apply {
                putExtra(ARG_USER_ID, userId)
            }
        }
    }
}
