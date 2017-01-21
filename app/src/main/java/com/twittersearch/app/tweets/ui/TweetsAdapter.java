package com.twittersearch.app.tweets.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.twittersearch.app.R;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import twitter4j.Status;

final class TweetsAdapter extends RecyclerView.Adapter<TweetsAdapter.ViewHolder> {

    private static final String TAG = TweetsAdapter.class.getName();

    private static final String LOGIN_FORMAT = "@%s";
    private static final String DATE_TIME_PATTERN = "dd MMM";

    private final Context context;
    private final LinkedList<Status> tweets;

    TweetsAdapter(final Context context) {
        this.context = context;
        this.tweets = new LinkedList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final Context context = parent.getContext();
        final View view = LayoutInflater.from(context).inflate(R.layout.item_tweet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Status tweet = tweets.get(position);
        Picasso.with(context).load(tweet.getUser().getProfileImageURL()).into(holder.avatarImageView);
        holder.nameTextView.setText(tweet.getUser().getName());
        final String formattedLogin = String.format(LOGIN_FORMAT, tweet.getUser().getScreenName());
        holder.loginNameTextView.setText(formattedLogin);
        final DateTime createdAt = new DateTime(tweet.getCreatedAt());
        final DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_TIME_PATTERN);
        holder.dateTextView.setText(formatter.print(createdAt));
        holder.messageTextView.setText(tweet.getText());
        holder.mapRetweetCount(tweet.getRetweetCount());

        new PatternEditableBuilder()
                .addPattern(Pattern.compile("(\\@|#)(\\w+)"),
                        Color.BLUE,
                        this::searchForTweets)
                .into(holder.messageTextView);
    }

    private void searchForTweets(String searchText) {
        final Intent intent = new Intent(TimelineFragment.SearchBroadcastReceiver.ACTION_SEARCH);
        intent.putExtra(TimelineFragment.SearchBroadcastReceiver.EXTRA_SEARCH_TEXT, searchText);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    @Nullable
    public Long getLastTweetId() {
        final int lastTweetIndex = getItemCount() - 1;
        if (lastTweetIndex >= 0) {
            final Status tweet = tweets.get(lastTweetIndex);
            return tweet.getId();
        }
        return null;
    }

    @Nullable
    public Long getFirstTweetId() {
        if (tweets.size() != 0) {
            final Status tweet = tweets.get(0);
            return tweet.getId();
        }
        return null;
    }

    public void addAll(List<Status> statuses) {
        final int initialTweetListSize = tweets.size();
        tweets.addAll(statuses);
        notifyItemRangeInserted(initialTweetListSize, tweets.size());
    }

    @Override
    public long getItemId(int position) {
        return tweets.get(position).getId();
    }

    public void clear() {
        tweets.clear();
    }

    public void addAllToTop(@NonNull List<Status> tweets) {
        this.tweets.addAll(0, tweets);
        notifyItemRangeInserted(0, tweets.size());
    }

    public boolean hasItems() {
        return !tweets.isEmpty();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.tweet_avatar_image_view)
        ImageView avatarImageView;
        @Bind(R.id.tweet_name_text_view)
        TextView nameTextView;
        @Bind(R.id.tweet_login_text_view)
        TextView loginNameTextView;
        @Bind(R.id.tweet_message_text_view)
        TextView messageTextView;
        @Bind(R.id.tweet_date_text_view)
        TextView dateTextView;
        @Bind(R.id.retweet_count_text_view)
        TextView retweetCountTextView;
        @Bind(R.id.retweet_count_image_view)
        ImageView retweetCountImageView;

        ViewHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void mapRetweetCount(int retweetCount) {
            if (retweetCount > 0) {
                retweetCountImageView.setVisibility(View.VISIBLE);
                retweetCountTextView.setVisibility(View.VISIBLE);
                retweetCountTextView.setText(String.valueOf(retweetCount));
            } else {
                retweetCountImageView.setVisibility(View.INVISIBLE);
                retweetCountTextView.setVisibility(View.INVISIBLE);
            }
        }
    }
}