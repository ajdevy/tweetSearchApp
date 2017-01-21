package com.twittersearch.app.util;

import android.util.Log;

import rx.functions.Action1;

public class LogErrorAction implements Action1<Throwable> {
    private final String tag;

    public LogErrorAction(String tag) {
        this.tag = tag;
    }

    @Override
    public void call(Throwable throwable) {
        Log.e(tag,"got an error",throwable);
    }
}