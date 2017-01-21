package com.twittersearch.app.tweets;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.f2prateek.rx.preferences.Preference;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.twittersearch.app.R;
import com.twittersearch.app.tweets.client.TwitterApiProvider;
import com.twittersearch.app.tweets.inject.TwitterToken;
import com.twittersearch.app.tweets.inject.TwitterTokenSecret;

import org.fuckboilerplate.rx_social_connect.Response;
import org.fuckboilerplate.rx_social_connect.RxSocialConnect;

import java.util.List;

import rx.Observable;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TwitterController {

    private static final String TAG = TwitterController.class.getName();

    public static final String REST_CONSUMER_KEY =
            "2wHbUr2YQUMiybEuM2Xh0o1vN";
    public static final String REST_CONSUMER_SECRET =
            "wsescxiAv45K1V6Gn4HcnxDKrFG4o6SKGgfs0F8vic5l6EpzgA";
    public static final String REST_CALLBACK_URL = "oauth://com.hintdesk.twitter_oauth_list";

    private static final int API_RATE_LIMIT_EXCEEDED_ERROR_CODE = 88;

    @TwitterToken
    private Preference<String> twitterTokenPreference;
    @TwitterTokenSecret
    private Preference<String> twitterTokenSecretPreference;
    private Context context;
    private TwitterApiProvider twitterApiProvider;

    public TwitterController(Context context, Preference<String> twitterTokenPreference, Preference<String> twitterTokenSecretPreference) {
        this.context = context;
        this.twitterTokenPreference = twitterTokenPreference;
        this.twitterTokenSecretPreference = twitterTokenSecretPreference;
        if (isAuthenticated()) {
            createTwitterApiProvider();
        }
    }

    private void createTwitterApiProvider() {
        twitterApiProvider = new TwitterApiProvider(twitterTokenPreference.get(), twitterTokenSecretPreference.get());
    }

    public boolean isAuthenticated() {
        return !TextUtils.isEmpty(twitterTokenPreference.get()) && !TextUtils.isEmpty(twitterTokenSecretPreference.get());
    }

    public Observable<Response<Activity, OAuth1AccessToken>> authenticate(Activity activity) {

        final OAuth10aService twitterService = new ServiceBuilder()
                .apiKey(REST_CONSUMER_KEY)
                .apiSecret(REST_CONSUMER_SECRET)
                .callback(REST_CALLBACK_URL)
                .build(TwitterApi.instance());
        return RxSocialConnect.with(activity, twitterService)
                .doOnNext(response -> {
                    OAuth1AccessToken token = response.token();
                    twitterTokenPreference.set(token.getToken());
                    twitterTokenSecretPreference.set(token.getTokenSecret());
                    createTwitterApiProvider();
                });
    }

    public Observable<List<Status>> searchTweets(final String keyword) {
        return twitterApiProvider.searchTweets(keyword);
    }

    public Observable<List<Status>> searchTweets(final String keyword, final long lastTweetId) {
        return twitterApiProvider.searchTweets(keyword, lastTweetId);
    }

    public Observable<List<Status>> refreshTweets(final String keyword, final long firstTweetId) {
        return twitterApiProvider.refreshTweets(keyword, firstTweetId);
    }

    public boolean canSearchTweets(String searchText) {
        return isAuthenticated() && (!searchText.trim().isEmpty());
    }

    public String getErrorMessage(TwitterException throwable) {
        if (throwable.getErrorCode() == API_RATE_LIMIT_EXCEEDED_ERROR_CODE && throwable.exceededRateLimitation()) {
            return context.getString(R.string.too_many_requests);
        } else {
            return throwable.getErrorMessage();
        }
    }
}