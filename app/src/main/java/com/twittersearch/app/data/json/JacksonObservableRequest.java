package com.twittersearch.app.data.json;

import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import java.util.Map;

import rx.Observable;
import rx.Subscriber;

public class JacksonObservableRequest<T> {

    private final Object requestData;
    private final int method;
    private final String url;
    private Class<T> responseType;
    private Map<String, String> headers;

    /**
     * Creates a new request.
     *
     * @param method      the HTTP method to use
     * @param url         URL to fetch the JSON from
     * @param requestData A {@link Object} to post and convert into json as the request. Null is allowed and indicates no parameters will be posted along with request.
     * @param headers     Headers to pass to the HTTP request
     */
    public JacksonObservableRequest(int method, String url, Map<String, String> headers, Object requestData, Class<T> responseType) {
        this.responseType = responseType;
        this.url = url;
        this.method = method;
        this.requestData = requestData;
        this.headers = headers;
    }

    public Observable<T> execute(@NonNull RequestQueue requestQueue) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                final Response.Listener responseListener = new Response.Listener<T>() {
                    @Override
                    public void onResponse(T response) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(response);
                            subscriber.onCompleted();
                        }
                    }
                };
                final Response.ErrorListener errorListener = error -> {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(error);
                        subscriber.onCompleted();
                    }
                };
                final JacksonRequest<T> request = new JacksonRequest<>(
                        method, url, headers, requestData, responseType, responseListener, errorListener);
                requestQueue.add(request);
            }
        });

    }
}