package com.petnbu.petnbu.util;

import android.app.Application;
import android.content.Context;
import android.support.design.widget.BaseTransientBottomBar;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Toaster {

    Application mContext;

    @Inject
    public Toaster(Application application){
        mContext = application;
    }

    public void makeText(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void makeTextLengthLong(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }
}
