package com.twittersearch.app.tweets.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.f2prateek.rx.preferences.Preference;
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerView;
import com.trello.rxlifecycle.components.support.RxFragment;
import com.twittersearch.app.R;
import com.twittersearch.app.TwitterSearchApp;
import com.twittersearch.app.tweets.TwitterController;
import com.twittersearch.app.tweets.inject.SelectedRefreshTime;
import com.twittersearch.app.tweets.ui.drawer.TweetRefreshDrawerAdapter;
import com.twittersearch.app.tweets.ui.drawer.TweetRefreshDrawerItem;
import com.twittersearch.app.util.LogErrorAction;
import com.twittersearch.app.util.NetworkHelper;
import com.twittersearch.app.util.UiAvailabilityUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import twitter4j.Status;
import twitter4j.TwitterException;

public final class TimelineFragment extends RxFragment {

    private static final String TAG = TimelineFragment.class.getName();

    private static final int TWEET_LOAD_THRESHOLD = 30;

    private String lastKeyword = "";
    private LinearLayoutManager layoutManager;

    private Subscription searchTweetsSubscription;

    @Inject
    TwitterController twitterController;
    @Inject
    @SelectedRefreshTime
    Preference<Integer> selectedRefreshTimePreference;

    @Bind(R.id.tweets_recycler_view)
    RecyclerView timelineRecycler;
    @Bind(R.id.loading_more_tweets_progress_bar)
    ProgressBar progressLoadingMoreTweets;
    @Bind(R.id.right_drawer_recycler_view)
    RecyclerView drawerRecycler;
    @Bind(R.id.tweets_swipe_refresh_layout)
    SwipeRefreshLayout timelineSwipeRefreshLayout;
    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    private TweetsAdapter adapter;
    private PublishSubject<String> newTweetSearchSubject = PublishSubject.create();
    private TweetRefreshTimeBroadcastReceiver tweetRefreshTimeBroadcastReceiver;
    private CloseDrawerBroadcastReceiver closeDrawerBroadcastReceiver;
    private SearchBroadcastReceiver searchBroadcastReceiver;
    private Subscription tweetRefreshSubscription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TwitterSearchApp) getActivity().getApplication()).getInjector().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_timeline, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupDrawer();

        if (!NetworkHelper.isNetworkAvailable(getContext())) {
            showNoInternetErrorMessage();
        }
    }

    private void setupDrawer() {
        // Set the adapter for the list view
        drawerRecycler.setAdapter(new TweetRefreshDrawerAdapter(getContext(), selectedRefreshTimePreference));
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        drawerRecycler.setLayoutManager(layoutManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceivers();

        //used for throwing keywords to search and it delays and handles same item distinction
        //so the search is not triggered for same keywoard and more than 1 time/second
        newTweetSearchSubject
                .distinctUntilChanged()
                .buffer(1, TimeUnit.SECONDS)
                .filter(result -> result.size() > 0)
                .map(result -> result.get(result.size() - 1))
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::searchTweets, new LogErrorAction(TAG));

        scheduleTweetRefresh(selectedRefreshTimePreference.get());
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    private void setupEndlessLoading() {
        RxRecyclerView.scrollEvents(timelineRecycler)
                .onBackpressureDrop()
                .filter(recyclerViewScrollEvent ->
                        !isLoadingShown())
                .distinctUntilChanged()
                .map(result -> layoutManager.findLastVisibleItemPosition())
                .filter(lastVisibleItemPosition ->
                        lastVisibleItemPosition >= Math.max(0, adapter.getItemCount() - 1 - TWEET_LOAD_THRESHOLD))
                .compose(bindToLifecycle())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(lastVisibleItemPosition -> {
                    if (!isLoadingShown()) {
                        loadNextPageTweets(getSearchText());
                    }
                }, throwable -> {
                    Log.e(TAG, "got an error while preloading tweets", throwable);
                    showErrorToast(throwable.getMessage());
                });
    }

    private String getSearchText() {
        return lastKeyword;
    }

    private boolean isLoadingShown() {
        return progressLoadingMoreTweets != null && progressLoadingMoreTweets.getVisibility() == View.VISIBLE;
    }

    private void showLoading() {
        if (progressLoadingMoreTweets != null) {
            progressLoadingMoreTweets.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressLoadingMoreTweets != null) {
            progressLoadingMoreTweets.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        timelineRecycler.setHasFixedSize(true);
        adapter = new TweetsAdapter(getActivity());
        adapter.setHasStableIds(true);
        timelineRecycler.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getContext());
        timelineRecycler.setLayoutManager(layoutManager);
        setupEndlessLoading();
        timelineSwipeRefreshLayout.setOnRefreshListener(() ->
                refreshTweets()
                        .onErrorReturn(throwable -> {
                            handleTweetSearchExceptionObservable(throwable)
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .subscribe(showErrorResult -> {
                                    }, new LogErrorAction(TAG));
                            return new ArrayList<>();
                        })
                        .compose(bindToLifecycle())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            timelineRecycler.scrollToPosition(0);
                            handleRefreshResults(result);
                            timelineSwipeRefreshLayout.setRefreshing(false);
                        }, throwable -> {
                            handleNoInternetException(throwable);
                            timelineSwipeRefreshLayout.setRefreshing(false);
                            new LogErrorAction(TAG).call(throwable);
                        }));
        setupSwapRefreshLayout();
    }

    private void handleNoInternetException(Throwable throwable) {
        if (NetworkHelper.isNetwrokUnavailableException(throwable)) {
            showNoInternetErrorMessage();
        }
    }

    private Observable<List<Status>> refreshTweets() {
        final String searchText = getSearchText();
        if (twitterController.canSearchTweets(searchText)) {
            final Observable<List<Status>> refreshObservable;
            final Long firstTweetId = adapter.getFirstTweetId();
            if (adapter.hasItems() && firstTweetId != null) {
                refreshObservable = twitterController.refreshTweets(searchText, firstTweetId);
            } else {
                refreshObservable = twitterController.searchTweets(searchText);
            }
            return NetworkHelper
                    .checkNetworkAndExecute(getContext(), refreshObservable);
        } else {
            return Observable.just(new ArrayList<>());
        }
    }

    private void setupSwapRefreshLayout() {
        timelineSwipeRefreshLayout.setEnabled(TweetRefreshDrawerItem.NO_REFRESH_VALUE == selectedRefreshTimePreference.get());
    }

    private void showNoInternetErrorMessage() {
        showErrorToast(R.string.not_internet);
    }

    private void showErrorToast(@StringRes int errorStringId) {
        Toast.makeText(getContext(), errorStringId, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadNextPageTweets(final String keyword) {
        final Long lastTweetId = adapter.getLastTweetId();
        if (lastTweetId != null) {
            handleTweetSearch(keyword, twitterController.searchTweets(keyword, lastTweetId));
        }
    }

    private void searchTweets(final String keyword) {
        clearAdapter();
        handleTweetSearch(keyword, twitterController.searchTweets(keyword));
    }

    private void clearAdapter() {
        adapter.clear();
        adapter.notifyDataSetChanged();
    }

    private void newTweetSearch(String searchText) {
        newTweetSearchSubject.onNext(searchText);
    }

    private void handleTweetSearch(@NonNull String keyword, @NonNull Observable<List<Status>> tweetSearchObservable) {
        setTitle(keyword);
        showLoading();
        safelyUnsubscribe(searchTweetsSubscription);
        lastKeyword = keyword;

        if (twitterController.canSearchTweets(keyword)) {
            searchTweetsSubscription = NetworkHelper.checkNetworkAndExecute(getContext(), tweetSearchObservable)
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        handleSearchResults(result);
                        hideLoading();
                    }, this::handleTweetSearchException);
        }
    }

    private void handleTweetSearchException(Throwable throwable) {
        hideLoading();
        timelineSwipeRefreshLayout.setRefreshing(false);
        if (NetworkHelper.isNetwrokUnavailableException(throwable)) {
            handleNoInternetException(throwable);
        } else {
            final String message;
            if (throwable instanceof TwitterException) {
                message = twitterController.getErrorMessage((TwitterException) throwable);
            } else {
                message = throwable.getMessage();
            }
            showErrorToast(message);

            Log.e(TAG, "error during search", throwable);
        }
    }

    private Observable<Throwable> handleTweetSearchExceptionObservable(Throwable throwable) {
        return Observable.fromCallable(() -> {
            handleTweetSearchException(throwable);
            return throwable;
        });
    }

    private void setTitle(@NonNull String title) {
        final FragmentActivity activity = getActivity();
        if (UiAvailabilityUtil.isUiAvailable(activity)) {
            activity.setTitle(title);
        }
    }

    private void handleSearchResults(@NonNull List<Status> tweets) {
        if (!tweets.isEmpty()) {
            adapter.addAll(tweets);
        }
    }

    private void handleRefreshResults(@NonNull List<Status> tweets) {
        if (!tweets.isEmpty()) {
            adapter.addAllToTop(tweets);
        }
    }

    private void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
    }

    private void scheduleTweetRefresh(int tweetRefreshInterval) {
        safelyUnsubscribe(tweetRefreshSubscription);

        setupSwapRefreshLayout();

        if (TweetRefreshDrawerItem.NO_REFRESH_VALUE != tweetRefreshInterval) {
            tweetRefreshSubscription = Observable.interval(tweetRefreshInterval, TimeUnit.SECONDS)
                    .onBackpressureDrop()
                    .flatMap(result -> refreshTweets()
                            .onErrorReturn(throwable -> new ArrayList<>()))
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleRefreshResults, this::handleTweetSearchException);
        }
    }

    private void registerReceivers() {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        searchBroadcastReceiver = new SearchBroadcastReceiver(this);
        final IntentFilter intentFilter = new IntentFilter(SearchBroadcastReceiver.ACTION_SEARCH);
        localBroadcastManager.registerReceiver(searchBroadcastReceiver, intentFilter);

        tweetRefreshTimeBroadcastReceiver = new TweetRefreshTimeBroadcastReceiver(this);
        final IntentFilter tweetRefreshIntentFilter = new IntentFilter(TweetRefreshTimeBroadcastReceiver.ACTION_UPDATE_TWEET_REFRESH_TIME);
        localBroadcastManager.registerReceiver(tweetRefreshTimeBroadcastReceiver, tweetRefreshIntentFilter);

        closeDrawerBroadcastReceiver = new CloseDrawerBroadcastReceiver(this);
        final IntentFilter closeDrawerIntentFilter = new IntentFilter(CloseDrawerBroadcastReceiver.ACTION_CLOSE_DRAWER);
        localBroadcastManager.registerReceiver(closeDrawerBroadcastReceiver, closeDrawerIntentFilter);
    }

    private void unregisterReceivers() {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        localBroadcastManager.unregisterReceiver(searchBroadcastReceiver);
        localBroadcastManager.unregisterReceiver(tweetRefreshTimeBroadcastReceiver);
        localBroadcastManager.unregisterReceiver(closeDrawerBroadcastReceiver);
    }

    private void safelyUnsubscribe(final Subscription... subscriptions) {
        for (Subscription subscription : subscriptions) {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }
    }

    public static class SearchBroadcastReceiver extends BroadcastReceiver {

        public static final String ACTION_SEARCH = "ACTION_SEARCH";
        public static final String EXTRA_SEARCH_TEXT = "EXTRA_SEARCH_TEXT";

        private final WeakReference<TimelineFragment> timelineFragmentWeakReference;

        public SearchBroadcastReceiver(TimelineFragment timelineFragment) {
            this.timelineFragmentWeakReference = new WeakReference<>(timelineFragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final TimelineFragment timelineFragment = timelineFragmentWeakReference.get();
            if (UiAvailabilityUtil.isUiAvailable(timelineFragment) && intent != null && ACTION_SEARCH.equals(intent.getAction())) {
                final String searchText = intent.getStringExtra(EXTRA_SEARCH_TEXT);
                timelineFragment.newTweetSearch(searchText);
            }
        }
    }

    public static class TweetRefreshTimeBroadcastReceiver extends BroadcastReceiver {

        public static final String ACTION_UPDATE_TWEET_REFRESH_TIME = "ACTION_UPDATE_TWEET_REFRESH_TIME";
        public static final String EXTRA_TWEET_REFRESH_INTERVAL = "EXTRA_TWEET_REFRESH_INTERVAL";

        private final WeakReference<TimelineFragment> timelineFragmentWeakReference;

        public TweetRefreshTimeBroadcastReceiver(TimelineFragment timelineFragment) {
            this.timelineFragmentWeakReference = new WeakReference<>(timelineFragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final TimelineFragment timelineFragment = timelineFragmentWeakReference.get();
            if (UiAvailabilityUtil.isUiAvailable(timelineFragment) && intent != null && ACTION_UPDATE_TWEET_REFRESH_TIME.equals(intent.getAction())) {
                final int tweetRefreshInterval = intent.getIntExtra(EXTRA_TWEET_REFRESH_INTERVAL, TweetRefreshDrawerItem.REFRESH_VALUE_5_SECONDS);
                timelineFragment.scheduleTweetRefresh(tweetRefreshInterval);
            }
        }
    }

    public static class CloseDrawerBroadcastReceiver extends BroadcastReceiver {

        public static final String ACTION_CLOSE_DRAWER = "ACTION_CLOSE_DRAWER";

        private final WeakReference<TimelineFragment> timelineFragmentWeakReference;

        public CloseDrawerBroadcastReceiver(TimelineFragment timelineFragment) {
            this.timelineFragmentWeakReference = new WeakReference<>(timelineFragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final TimelineFragment timelineFragment = timelineFragmentWeakReference.get();
            if (UiAvailabilityUtil.isUiAvailable(timelineFragment) && intent != null && ACTION_CLOSE_DRAWER.equals(intent.getAction())) {
                timelineFragment.closeDrawer();
            }
        }
    }
}