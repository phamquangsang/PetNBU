package com.petnbu.petnbu.ui.user

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.FragmentFeedProfileListBinding
import com.petnbu.petnbu.model.Paging

class UserProfileFragment : Fragment() {

    private lateinit var mBinding: FragmentFeedProfileListBinding
    private lateinit var userProfileViewModel: UserProfileViewModel
    private val userId: String by lazy {
        arguments?.run {
            getString(ARG_USER_ID)
        } ?: ""
    }
    private lateinit var userProfileFeedsAdapter: UserProfileFeedsAdapter
    private var columnCount = 3

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_feed_profile_list, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        activity?.run {
            if (!userId.isEmpty()) {
                userProfileFeedsAdapter = UserProfileFeedsAdapter()
                mBinding.list.layoutManager = if (columnCount <= 1)
                    LinearLayoutManager(context)
                else
                    GridLayoutManager(context, columnCount)
                mBinding.list.adapter = userProfileFeedsAdapter
                mBinding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        (recyclerView.layoutManager as? LinearLayoutManager)?.run {
                            if (findLastVisibleItemPosition() == userProfileFeedsAdapter.itemCount - 1)
                                userProfileViewModel.loadNextPage(userId, Paging.userFeedsPagingId(userId))

                        } ?: (recyclerView.layoutManager as? GridLayoutManager)?.run {
                            if (findLastVisibleItemPosition() / 3 == userProfileFeedsAdapter.itemCount / columnCount)
                                userProfileViewModel.loadNextPage(userId, Paging.userFeedsPagingId(userId))
                        }
                    }
                })

                userProfileViewModel = ViewModelProviders.of(this).get(UserProfileViewModel::class.java)
                userProfileViewModel.apply {
                    getFeeds(userId, Paging.userFeedsPagingId(userId)).observe(this@UserProfileFragment, Observer { feeds ->
                        feeds?.run {
                            userProfileFeedsAdapter.submitList(this)
                        }
                    })

                    getUser(userId).observe(this@UserProfileFragment, Observer { user ->
                        user?.run {
                            mBinding.tvUserNamePlaceHolder.visibility = View.GONE
                            mBinding.tvUserName.visibility = View.VISIBLE
                            mBinding.tvUserName.text = name

                            GlideApp.with(this@UserProfileFragment)
                                    .load(avatar.originUrl)
                                    .into(mBinding.imgProfile)
                        }
                    })
                }
            } else {
                finish()
            }
        }
    }

    companion object {
        private const val ARG_USER_ID = "user-id"

        fun newInstance(userId: String): UserProfileFragment {
            return UserProfileFragment().apply {
                arguments = bundleOf(ARG_USER_ID to userId)
            }
        }
    }
}
