package com.petnbu.petnbu.api

interface SuccessCallback<T> {

    fun onSuccess(t: T)

    fun onFailed(e: Exception)

}
