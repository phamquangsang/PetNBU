package com.petnbu.petnbu.api;

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


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Common class used by API responses.
 * @param <T>
 */
public class ApiResponse<T> {
    public final boolean isSucceed;
    @Nullable
    public final T body;
    @Nullable
    public final String errorMessage;

    public ApiResponse(Throwable error) {
        isSucceed = false;
        body = null;
        errorMessage = error.getMessage();
    }

    public ApiResponse(T responseType, boolean isSucceed, String errorMsg) {
        this.isSucceed = isSucceed;
        if(isSucceed) {
            body = responseType;
            errorMessage = null;
        } else {
            errorMessage = errorMsg;
            body = null;
        }
    }

    public boolean isSuccessful() {
        return isSucceed;
    }

}
