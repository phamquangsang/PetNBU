package com.petnbu.petnbu.ui;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public abstract class BaseBindingViewHolder <T extends ViewDataBinding, M> extends RecyclerView.ViewHolder {

    public final T mBinding;

    public BaseBindingViewHolder(View itemView) {
        super(itemView);
        mBinding = DataBindingUtil.bind(itemView);
    }

    public abstract void bindData(M item);

    public abstract void bindData(M item, List<Object> payloads);
}
