package com.petnbu.petnbu.ui

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.View

abstract class BaseBindingViewHolder<T : ViewDataBinding, M>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val mBinding: T = DataBindingUtil.bind(itemView)!!

    abstract fun bindData(item: M)

    abstract fun bindData(item: M, payloads: List<Any>)
}
