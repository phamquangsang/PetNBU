package com.petnbu.petnbu.di;

import android.app.Activity;

import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.login.LoginJavaActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(dependencies = {}, modules = {AppModule.class, WebServiceModule.class})
public interface AppComponent {
    void inject(LoginJavaActivity activity);

}
