package com.petnbu.petnbu.di;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.WebService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class WebServiceModule {

    public WebServiceModule() {

    }

    @Singleton
    @Provides
    FirebaseFirestore provideFirebaseFirestore(){
        return FirebaseFirestore.getInstance();
    }

    @Singleton
    @Provides
    WebService provideWedService(FirebaseFirestore firebaseFirestore){
        return new FirebaseService(firebaseFirestore);
    }
}
