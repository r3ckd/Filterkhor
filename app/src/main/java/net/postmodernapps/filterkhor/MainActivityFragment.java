package net.postmodernapps.filterkhor;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.tinymission.rss.Feed;

import net.postmodernapps.filterkhor.adapters.SubscribedFeedItemsAdapter;

import info.guardianproject.securereader.FeedFetcher;
import info.guardianproject.securereader.SocialReader;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout mSwipeLayout;
    private ListView mListView;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeLayout = (SwipeRefreshLayout)view.findViewById(R.id.swipe_refresh_layout);
        mSwipeLayout.setOnRefreshListener(this);

        mListView = (ListView)view.findViewById(R.id.listView);
        mListView.setAdapter(new SubscribedFeedItemsAdapter((SubscribedFeedItemsAdapter.SubscribedFeedItemsAdapterListener)getActivity()));
    }

    @Override
    public void onRefresh() {
        if (App.getInstance().socialReader.isOnline() != SocialReader.ONLINE)
        {
            mSwipeLayout.setRefreshing(false);
            Toast.makeText(getContext(), R.string.pulldown_to_sync_no_net, Toast.LENGTH_SHORT).show();
        } else {
            App.getInstance().socialReader.manualSyncSubscribedFeeds(new FeedFetcher.FeedFetchedCallback() {
                @Override
                public void feedFetched(Feed _feed) {
                    mSwipeLayout.setRefreshing(false);
                    ((SubscribedFeedItemsAdapter) mListView.getAdapter()).populateFromFeed(_feed);
                }
            });
        }
    }

    public void updateList() {
        ((SubscribedFeedItemsAdapter)mListView.getAdapter()).notifyDataSetChanged();
    }
}
