package com.petnbu.petnbu;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class BaseBindingViewHolder <T extends ViewDataBinding, M> extends RecyclerView.ViewHolder {

    public final T mBinding;

    public BaseBindingViewHolder(View itemView) {
        super(itemView);
        mBinding = DataBindingUtil.bind(itemView);
    }

    public abstract void bindData(M item);
}
