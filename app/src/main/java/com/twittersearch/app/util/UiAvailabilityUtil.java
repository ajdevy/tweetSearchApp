package com.twittersearch.app.util;

import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.twittersearch.app.tweets.ui.TimelineFragment;

public class UiAvailabilityUtil {

    public static boolean isUiAvailable(@Nullable FragmentActivity activity) {
        return activity != null && !activity.isFinishing();
    }

    public static boolean isUiAvailable(@Nullable TimelineFragment timelineFragment) {
        return timelineFragment != null && timelineFragment.isAdded() && isUiAvailable(timelineFragment.getActivity());
    }
}