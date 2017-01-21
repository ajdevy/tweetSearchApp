package com.twittersearch.app.tweets.ui.drawer;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.f2prateek.rx.preferences.Preference;
import com.twittersearch.app.R;
import com.twittersearch.app.tweets.ui.TimelineFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public final class TweetRefreshDrawerAdapter extends RecyclerView.Adapter<TweetRefreshDrawerAdapter.DrawerItemViewHolder> {

    private static final String TAG = TweetRefreshDrawerAdapter.class.getName();

    private static final int VIEW_TYPE_ITEM_CHECKED = 563;
    private static final int VIEW_TYPE_ITEM = 564;

    private final Context context;
    private final Preference<Integer> selectedRefreshTimePreference;
    private final List<TweetRefreshDrawerItem> items;

    public TweetRefreshDrawerAdapter(final Context context, Preference<Integer> selectedRefreshTimePreference) {
        this.context = context;
        this.selectedRefreshTimePreference = selectedRefreshTimePreference;
        items = new ArrayList<>();
        items.add(new TweetRefreshDrawerItem(R.string.no_refresh, TweetRefreshDrawerItem.NO_REFRESH_VALUE));
        items.add(new TweetRefreshDrawerItem(R.string.two_seconds, TweetRefreshDrawerItem.REFRESH_VALUE_2_SECONDS));
        items.add(new TweetRefreshDrawerItem(R.string.five_seconds, TweetRefreshDrawerItem.REFRESH_VALUE_5_SECONDS));
        items.add(new TweetRefreshDrawerItem(R.string.thirty_seconds, TweetRefreshDrawerItem.REFRESH_VALUE_30_SECONDS));
        items.add(new TweetRefreshDrawerItem(R.string.one_minute, TweetRefreshDrawerItem.REFRESH_VALUE_1_MINUTE));
        selectItem(selectedRefreshTimePreference.get());
        notifyDataSetChanged();
    }

    private void selectItem(int itemRefreshValue) {
        for (TweetRefreshDrawerItem item : items) {
            item.setChecked(item.getRefreshValue() == itemRefreshValue);
        }
        selectedRefreshTimePreference.set(itemRefreshValue);
    }

    @Override
    public DrawerItemViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == VIEW_TYPE_ITEM_CHECKED) {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_drawer_checked, parent, false);
            return new DrawerItemViewHolder(view);
        } else {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_drawer, parent, false);
            return new DrawerItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(DrawerItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        if (position < items.size()) {
            if (items.get(position).isChecked()) {
                return VIEW_TYPE_ITEM_CHECKED;
            } else {
                return VIEW_TYPE_ITEM;

            }
        }
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class DrawerItemViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.drawer_item_text_view)
        TextView itemNameTextView;

        DrawerItemViewHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(@NonNull TweetRefreshDrawerItem drawerItem) {
            itemView.setOnClickListener(view -> {
                selectItem(drawerItem.getRefreshValue());
                notifyDataSetChanged();

                updateTweetRefreshTime(drawerItem);
                closeDrawer();
            });
            itemNameTextView.setText(drawerItem.getStringId());
        }

        private void closeDrawer() {
            LocalBroadcastManager.getInstance(itemView.getContext()).sendBroadcast(new Intent(TimelineFragment.CloseDrawerBroadcastReceiver.ACTION_CLOSE_DRAWER));
        }

        private void updateTweetRefreshTime(@NonNull TweetRefreshDrawerItem drawerItem) {
            final Intent intent = new Intent(TimelineFragment.TweetRefreshTimeBroadcastReceiver.ACTION_UPDATE_TWEET_REFRESH_TIME);
            intent.putExtra(TimelineFragment.TweetRefreshTimeBroadcastReceiver.EXTRA_TWEET_REFRESH_INTERVAL, drawerItem.getRefreshValue());
            LocalBroadcastManager.getInstance(itemView.getContext()).sendBroadcast(intent);
        }
    }
}