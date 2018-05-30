package com.petnbu.petnbu.userprofile;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentFeedProfileListBinding;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.UserEntity;

import timber.log.Timber;

public class UserProfileFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static String ARG_USER_ID = "user-id";

    private int mColumnCount = 1;
    private String mUserId;
    private UserProfileViewModel mViewModel;
    private ProfileFeedAdapter mAdapter;
    private FragmentFeedProfileListBinding mBinding;

    public UserProfileFragment() {
    }

    public static UserProfileFragment newInstance(int columnCount, String userId) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            mUserId = getArguments().getString(ARG_USER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_feed_profile_list, container, false);

        if (mColumnCount <= 1) {
            mBinding.list.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            mBinding.list.setLayoutManager(new GridLayoutManager(getContext(), mColumnCount));
        }
        mAdapter = new ProfileFeedAdapter();
        mBinding.list.setAdapter(mAdapter);
        mBinding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(recyclerView.getLayoutManager() instanceof LinearLayoutManager){
                    LinearLayoutManager layoutManager = (LinearLayoutManager)
                            recyclerView.getLayoutManager();
                    int lastPosition = layoutManager
                            .findLastVisibleItemPosition();
                    if (lastPosition == mAdapter.getItemCount() - 1) {
                        mViewModel.loadNextPage(mUserId);
                    }
                }else{
                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    int lastPosition = layoutManager
                            .findLastVisibleItemPosition();
                    if (lastPosition/3 == mAdapter.getItemCount() / mColumnCount) {
                        mViewModel.loadNextPage(mUserId);
                    }
                }

            }
        });
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getActivity()).get(UserProfileViewModel.class);
        mViewModel.getFeeds(mUserId, Paging.userFeedsPagingId(mUserId)).observe(this, feedsResource -> {
            Timber.i("onChanged %s :", feedsResource == null ? "null" : feedsResource.toString());
            if (feedsResource != null && feedsResource.data != null) {
                mAdapter.replace(feedsResource.data);
            }
        });
        mViewModel.getUser(mUserId).observe(this, userResource -> {
            if(userResource != null && userResource.data!=null){
                UserEntity userEntity = userResource.data;
                mBinding.tvUserNamePlaceHolder.setVisibility(View.GONE);
                mBinding.tvUserName.setVisibility(View.VISIBLE);
                mBinding.tvUserName.setText(userEntity.getName());

                GlideApp.with(mBinding.imgProfile)
                        .load(userEntity.getAvatar().getOriginUrl())
                        .into(mBinding.imgProfile);
            }
        });
    }
}
