package com.twittersearch.app.util;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public class FragmentUtil {

    private static final String TAG = FragmentUtil.class.getName();

    public static void clearBackStack(FragmentActivity fragmentActivity) {
        if (UiAvailabilityUtil.isUiAvailable(fragmentActivity)) {
            final FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
            while (fragmentManager.getBackStackEntryCount() != 0) {
                Log.d(TAG, "clearBackStack " + fragmentManager.getBackStackEntryCount());
                fragmentManager.popBackStackImmediate();
            }
        }
    }

    public static void remove(@NonNull Fragment fragment) {
        if (UiAvailabilityUtil.isUiAvailable(fragment.getActivity())) {
            fragment.getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }
}