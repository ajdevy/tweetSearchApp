package com.twittersearch.app;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.twittersearch.app.inject.AppInjectionComponent;
import com.twittersearch.app.inject.AppInjectionModule;
import com.twittersearch.app.inject.DaggerAppInjectionComponent;

import org.fuckboilerplate.rx_social_connect.RxSocialConnect;

import io.victoralbertos.jolyglot.JacksonSpeaker;

public class TwitterSearchApp extends Application {

    private AppInjectionComponent injectionComponent;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        createInjectionComponent();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        RxSocialConnect.register(this, "encryptionKeyForSocialConnection")
                .using(new JacksonSpeaker());

        Stetho.initializeWithDefaults(this);
    }

    public AppInjectionComponent getInjector() {
        return injectionComponent;
    }

    public void createInjectionComponent() {
        injectionComponent = DaggerAppInjectionComponent.builder()
                .appInjectionModule(new AppInjectionModule(this))
                .build();
    }
}