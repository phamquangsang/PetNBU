package com.petnbu.petnbu.extensions

import android.os.Build
import android.os.Trace
import com.petnbu.petnbu.BuildConfig
import com.petnbu.petnbu.PetApplication
import timber.log.Timber
import kotlin.system.measureTimeMillis

fun Any.toJson(): String = PetApplication.appComponent.gson.toJson(this)

fun begin(tag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && BuildConfig.DEBUG) {
        Trace.beginSection(tag)
    }
}

fun end() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && BuildConfig.DEBUG) {
        Trace.endSection()
    }
}

inline fun <R> beginSysTrace(tag: String, crossinline block: () -> R): R {
    val result by lazy {
        block()
    }
    val measuredTimeInMillis = measureTimeMillis {
        begin(tag)
        result
        end()
    }
    Timber.d("$tag in $measuredTimeInMillis")
    return result
}