package com.twittersearch.app.tweets.ui.drawer;

import android.support.annotation.StringRes;

public class TweetRefreshDrawerItem {

    public static final int NO_REFRESH_VALUE = -1;
    public static final int REFRESH_VALUE_2_SECONDS = 2;
    public static final int REFRESH_VALUE_5_SECONDS = 5;
    public static final int REFRESH_VALUE_30_SECONDS = 30;
    public static final int REFRESH_VALUE_1_MINUTE = 60;

    private boolean checked;
    @StringRes
    private int stringId;
    private int refreshValue;

    public TweetRefreshDrawerItem(@StringRes int stringId, int refreshValue) {
        this.stringId = stringId;
        this.refreshValue = refreshValue;
    }

    @StringRes
    public int getStringId() {
        return stringId;
    }

    public void setStringId(int stringId) {
        this.stringId = stringId;
    }

    public int getRefreshValue() {
        return refreshValue;
    }

    public void setRefreshValue(int refreshValue) {
        this.refreshValue = refreshValue;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}