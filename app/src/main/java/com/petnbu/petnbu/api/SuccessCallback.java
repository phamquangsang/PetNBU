package com.petnbu.petnbu.api;

public interface SuccessCallback<T> {

    void onSuccess(T t);

    void onFailed(Exception e);

}
