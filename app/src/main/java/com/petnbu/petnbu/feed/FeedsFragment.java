package com.petnbu.petnbu.feed;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.petnbu.petnbu.MainActivity;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.databinding.FragmentFeedsBinding;

import java.util.ArrayList;

public class FeedsFragment extends Fragment {

    private FragmentFeedsBinding mBinding;
    private FeedsViewModel mFeedsViewModel;
    private FeedsRecyclerViewAdapter mAdapter;

    public FeedsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_feeds, container, false);
        initialize();
        return mBinding.getRoot();
    }

    public void initialize() {
        mFeedsViewModel = MainActivity.obtainViewModel(getActivity());
        mFeedsViewModel.getFeeds().observe(this, feeds -> mAdapter.setFeeds(feeds));

        mAdapter = new FeedsRecyclerViewAdapter(getContext(), new ArrayList<>());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mBinding.rvFeeds.setLayoutManager(linearLayoutManager);
        mBinding.rvFeeds.post(() -> {
            int maxHeight = mBinding.rvFeeds.getHeight();
            mAdapter.setMaxPhotoHeight(maxHeight - (Utils.getToolbarHeight(getContext()) * 3 / 2));
            mBinding.rvFeeds.setAdapter(mAdapter);
        });
        mBinding.rvFeeds.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && mBinding.fabNewPost.getVisibility() == View.VISIBLE) {
                    mBinding.fabNewPost.hide();
                } else if (dy < 0 && mBinding.fabNewPost.getVisibility() != View.VISIBLE) {
                    mBinding.fabNewPost.show();
                }
            }
        });

        mBinding.fabNewPost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateFeedActivity.class);
            startActivity(intent);
        });
    }
}
