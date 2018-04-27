package com.petnbu.petnbu.views.dialog;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.DialogSimpleBinding;

public class SimpleDialogFragment extends DialogFragment implements View.OnClickListener {

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_JUSTIFY = "justify";

    private DialogSimpleBinding mBinding;
    private String title;
    private String message;
    private int justify;

    private View.OnClickListener okClickListener;

    public SimpleDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static SimpleDialogFragment newInstance(String title, String message) {
        SimpleDialogFragment matchingDialogFragment = new SimpleDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        args.putInt(KEY_JUSTIFY, Gravity.CENTER);
        matchingDialogFragment.setArguments(args);
        return matchingDialogFragment;
    }

    public static SimpleDialogFragment newInstance(String title, String message, int justify) {
        SimpleDialogFragment matchingDialogFragment = new SimpleDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        args.putInt(KEY_JUSTIFY, justify);
        matchingDialogFragment.setArguments(args);
        return matchingDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(KEY_TITLE, "");
            message = getArguments().getString(KEY_MESSAGE, "");
            justify = getArguments().getInt(KEY_JUSTIFY, Gravity.CENTER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_simple, container, false);
        setEventListeners();
        initialize();
        return mBinding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.DialogFragmentStyle);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void setEventListeners() {
        mBinding.btnOk.setOnClickListener(this);
    }

    private void initialize() {
        if (TextUtils.isEmpty(title)) {
            mBinding.tvTitle.setVisibility(View.GONE);
        } else {
            mBinding.tvTitle.setVisibility(View.VISIBLE);
            mBinding.tvTitle.setText(title);
        }
        if (TextUtils.isEmpty(message)) {
            mBinding.tvMessage.setVisibility(View.GONE);
        } else {
            mBinding.tvMessage.setVisibility(View.VISIBLE);
            mBinding.tvMessage.setText(message);
            mBinding.tvMessage.setGravity(justify);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBinding.btnOk) {
            if (okClickListener != null) {
                okClickListener.onClick(v);
            }
            SimpleDialogFragment.this.dismiss();
        }
    }

    public void setOkClickListener(View.OnClickListener okClickListener) {
        this.okClickListener = okClickListener;
    }
}
