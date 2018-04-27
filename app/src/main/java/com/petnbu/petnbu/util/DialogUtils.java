package com.petnbu.petnbu.util;

import android.content.Context;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.petnbu.petnbu.views.dialog.SimpleDialogFragment;

public class DialogUtils {

    public static void showSimpleDialog(Context context, String title, String content, View.OnClickListener okClickListener) {
        if (context != null && context instanceof AppCompatActivity) {
            FragmentTransaction transactionFragment = ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction();
            SimpleDialogFragment simpleDialogFragment = SimpleDialogFragment.newInstance(title, content);
            simpleDialogFragment.setOkClickListener(okClickListener);
            transactionFragment.add(simpleDialogFragment, SimpleDialogFragment.class.getSimpleName());
            transactionFragment.commitAllowingStateLoss();
        }
    }
}
