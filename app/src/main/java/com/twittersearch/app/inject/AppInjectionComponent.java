package com.twittersearch.app.inject;

import com.twittersearch.app.ui.MainActivity;
import com.twittersearch.app.tweets.ui.TimelineFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppInjectionModule.class})
public interface AppInjectionComponent {

    void inject(MainActivity mainActivity);

    void inject(TimelineFragment timelineFragment);
}