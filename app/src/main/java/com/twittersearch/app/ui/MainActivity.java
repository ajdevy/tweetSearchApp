package com.twittersearch.app.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.twittersearch.app.R;
import com.twittersearch.app.TwitterSearchApp;
import com.twittersearch.app.tweets.TwitterController;
import com.twittersearch.app.tweets.ui.TimelineFragment;
import com.twittersearch.app.util.FragmentUtil;
import com.twittersearch.app.util.LogErrorAction;
import com.twittersearch.app.util.UiAvailabilityUtil;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends RxAppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    @Inject
    TwitterController twitterController;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.search_view)
    MaterialSearchView searchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        ((TwitterSearchApp) getApplication()).getInjector().inject(this);

        setupToolbar();

        if (!twitterController.isAuthenticated()) {
            twitterController.authenticate(this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        openTimelineFragment();
                    }, new LogErrorAction(TAG));
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        setTitle("");
        initSearchView();
        setupStatusBarColor();
    }

    private void initSearchView() {
        searchView.setVoiceSearch(false);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(final String query) {
                final Intent intent = new Intent(TimelineFragment.SearchBroadcastReceiver.ACTION_SEARCH);
                intent.putExtra(TimelineFragment.SearchBroadcastReceiver.EXTRA_SEARCH_TEXT, query);
                LocalBroadcastManager.getInstance(searchView.getContext()).sendBroadcast(intent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    final Intent intent = new Intent(TimelineFragment.SearchBroadcastReceiver.ACTION_SEARCH);
                    intent.putExtra(TimelineFragment.SearchBroadcastReceiver.EXTRA_SEARCH_TEXT, newText);
                    LocalBroadcastManager.getInstance(searchView.getContext()).sendBroadcast(intent);
                }
                return false;
            }
        });
    }

    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (twitterController.isAuthenticated()) {
            openTimelineFragment();
        }
    }

    private void setupStatusBarColor() {
        final Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary_green_dark));
            }
        }
    }

    public void openTimelineFragment() {
        if (findViewById(R.id.fragment_container) != null) {
            final String tag = TimelineFragment.class.getName();
            final Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (existingFragment == null) {
                if (UiAvailabilityUtil.isUiAvailable(this)) {
                    final TimelineFragment fragment = new TimelineFragment();
                    FragmentUtil.clearBackStack(this);
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, fragment, tag)
                            .commit();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }
}