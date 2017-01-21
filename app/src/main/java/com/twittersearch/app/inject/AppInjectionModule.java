package com.twittersearch.app.inject;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.twittersearch.app.tweets.TwitterController;
import com.twittersearch.app.tweets.inject.SelectedRefreshTime;
import com.twittersearch.app.tweets.inject.TwitterToken;
import com.twittersearch.app.tweets.inject.TwitterTokenSecret;
import com.twittersearch.app.tweets.ui.drawer.TweetRefreshDrawerItem;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppInjectionModule {

    private static final java.lang.String PREF_TWITTER_TOKEN = "twitterToken";
    private static final java.lang.String PREF_TWITTER_TOKEN_SECRET = "twitterTokenSecret";
    private static final java.lang.String PREF_SELECTED_REFRESH_TIME = "selectedRefreshTime";

    private Application mApplication;

    public AppInjectionModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Application providesApplication() {
        return mApplication;
    }

    @Provides
    @Singleton
    RxSharedPreferences provideRxSharedPreferences(Application app) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        return RxSharedPreferences.create(preferences);
    }

    @Provides
    @Singleton
    @TwitterToken
    Preference<String> provideTwitterToken(RxSharedPreferences preferences) {
        return preferences.getString(PREF_TWITTER_TOKEN, "");
    }

    @Provides
    @Singleton
    @TwitterTokenSecret
    Preference<String> provideTwitterTokenSecret(RxSharedPreferences preferences) {
        return preferences.getString(PREF_TWITTER_TOKEN_SECRET, "");
    }

    @Provides
    @Singleton
    @SelectedRefreshTime
    Preference<Integer> provideSelectedRefreshTime(RxSharedPreferences preferences) {
        return preferences.getInteger(PREF_SELECTED_REFRESH_TIME, TweetRefreshDrawerItem.REFRESH_VALUE_5_SECONDS);
    }

    @Provides
    @Singleton
    TwitterController provideTwitterController(Application app,
                                               @TwitterToken Preference<String> twitterToken,
                                               @TwitterTokenSecret Preference<String> twitterTokenSecret) {
        return new TwitterController(app, twitterToken, twitterTokenSecret);
    }
}