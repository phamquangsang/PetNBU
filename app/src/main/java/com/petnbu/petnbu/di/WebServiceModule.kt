package com.petnbu.petnbu.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.api.FirebaseService
import com.petnbu.petnbu.api.WebService

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

@Module
class WebServiceModule {

    @Singleton
    @Provides
    internal fun provideFirebaseFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        db.firestoreSettings = settings

        return db
    }

    @Singleton
    @Provides
    internal fun provideWedService(firebaseFirestore: FirebaseFirestore, appExecutors: AppExecutors): WebService {
        return FirebaseService(firebaseFirestore, appExecutors)
    }
}
