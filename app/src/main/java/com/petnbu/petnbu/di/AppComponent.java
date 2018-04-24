package com.petnbu.petnbu.di;

import android.app.Activity;

import com.petnbu.petnbu.login.LoginJavaActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {WebServiceModule.class})
public interface WebComponent {
    void inject(LoginJavaActivity activity);
}
