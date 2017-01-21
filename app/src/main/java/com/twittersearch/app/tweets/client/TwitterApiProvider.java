package com.twittersearch.app.tweets.client;

import android.support.annotation.NonNull;

import com.twittersearch.app.tweets.TwitterController;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public final class TwitterApiProvider {

    private static final int MAX_TWEETS_PER_REQUEST = 100;
    private final Twitter twitterInstance;

    public TwitterApiProvider(String twitterToken, String twitterTokenSecret) {
        final Configuration configuration = createConfiguration(twitterToken, twitterTokenSecret);
        final TwitterFactory twitterFactory = new TwitterFactory(configuration);
        twitterInstance = twitterFactory.getInstance();
    }

    private Configuration createConfiguration(String twitterToken, String twitterTokenSecret) {
        final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setDebugEnabled(true)
                .setOAuthConsumerKey(TwitterController.REST_CONSUMER_KEY)
                .setOAuthConsumerSecret(TwitterController.REST_CONSUMER_SECRET)
                .setOAuthAccessToken(twitterToken)
                .setOAuthAccessTokenSecret(twitterTokenSecret);

        return configurationBuilder.build();
    }

    public Observable<List<Status>> searchTweets(final String keyword) {
        return observableSearch(new Query(keyword).count(MAX_TWEETS_PER_REQUEST));
    }

    public Observable<List<Status>> searchTweets(final String keyword, final long maxTweetId) {
        return observableSearch(new Query(keyword).maxId(maxTweetId).count(MAX_TWEETS_PER_REQUEST));
    }

    public Observable<List<Status>> refreshTweets(String keyword, long firstTweetId) {
        return observableSearch(new Query(keyword).sinceId(firstTweetId).count(MAX_TWEETS_PER_REQUEST));
    }

    private Observable<List<Status>> observableSearch(@NonNull Query query) {
        return Observable.create(new Observable.OnSubscribe<List<Status>>() {
            @Override
            public void call(Subscriber<? super List<Status>> subscriber) {
                try {
                    final QueryResult result = twitterInstance.search(query);
                    subscriber.onNext(result.getTweets());
                    subscriber.onCompleted();
                } catch (TwitterException e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}