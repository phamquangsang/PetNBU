/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.petnbu.petnbu.api

import timber.log.Timber

/**
 * Common class used by API responses.
 * @param <T>
</T> */
class ApiResponse<T> {
    val isSuccessful: Boolean
    val body: T?
    val error : Throwable?
    val errorMessage: String?

    constructor(error: Throwable) {
        isSuccessful = false
        body = null
        this.error = error
        errorMessage = error.message
        Timber.e(error)
    }

    constructor(responseType :T){
        isSuccessful = true
        body = responseType
        error = null
        errorMessage = null
    }

    constructor(responseType: T?, isSucceed: Boolean, errorMsg: String?) {
        this.isSuccessful = isSucceed
        this.error = null
        if (isSucceed) {
            body = responseType
            errorMessage = null
        } else {
            errorMessage = errorMsg
            body = null
        }
    }

}
