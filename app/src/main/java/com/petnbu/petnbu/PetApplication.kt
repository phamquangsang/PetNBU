package com.petnbu.petnbu

import android.app.Application
import android.os.StrictMode
import com.petnbu.petnbu.di.AppComponent
import com.petnbu.petnbu.di.AppModule
import com.petnbu.petnbu.di.DaggerAppComponent
import timber.log.Timber


class PetApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll() // or .detectAll() for all detectable problems
                    .penaltyDeath()
                    .penaltyLog()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build())
        }
        appComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build()
    }

    companion object {
        @JvmStatic
        lateinit var appComponent: AppComponent
            private set
    }
}
