package com.twittersearch.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.net.ConnectException;

import rx.Observable;

public class NetworkHelper {

    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isNetwrokUnavailableException(Throwable throwable) {
        return throwable != null && throwable instanceof ConnectException;
    }

    public static <T> Observable<T> checkNetworkAndExecute(@NonNull Context context, @NonNull Observable<T> networkObservable) {
        return Observable.just(NetworkHelper.isNetworkAvailable(context))
                .flatMap(isNetworkAvailable -> {
                    if (isNetworkAvailable) {
                        return Observable.just(null);
                    } else {
                        return Observable.error(new ConnectException("Network not available"));
                    }
                })
                .flatMap(result -> networkObservable);
    }
}