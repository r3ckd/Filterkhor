package net.postmodernapps.filterkhor.adapters;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

import java.util.ArrayList;
import java.util.Date;

import net.postmodernapps.filterkhor.App;
import net.postmodernapps.filterkhor.R;
import net.postmodernapps.filterkhor.models.PluggableTransport;
import net.postmodernapps.filterkhor.models.ServiceDescription;
import net.postmodernapps.filterkhor.models.Technology;

public class SubscribedFeedItemsAdapter extends BaseAdapter {

    public interface SubscribedFeedItemsAdapterListener {
        void onListUpdated(Date when);
    }

    private class ServiceEntry {
        public String name;
        public String description;
        public int status;

        public ServiceEntry(String name, String description, int status) {
            this.name = name;
            this.description = description;
            this.status = status;
        }
    }

    private SubscribedFeedItemsAdapterListener mListener;
    private AsyncTask<Void, Void, Feed> mUpdateTask;
    private ServiceDescription mServiceList;
    private ArrayList<ServiceEntry> mEntries;

    public SubscribedFeedItemsAdapter(SubscribedFeedItemsAdapterListener listener) {
        mListener = listener;
        updateFeedsFromDB();
    }

    private void updateFeedsFromDB() {
        if (mUpdateTask != null)
            mUpdateTask.cancel(true);
        mUpdateTask = new AsyncTask<Void, Void, Feed>() {

            @Override
            protected Feed doInBackground(Void... params) {
                Feed feed = App.getInstance().socialReader.getSubscribedFeedItems();
                return feed;
            }

            @Override
            protected void onPostExecute(Feed feed) {
                super.onPostExecute(feed);
                populateFromFeed(feed);
            }
        };
        mUpdateTask.execute();
    }

    @Override
    public void notifyDataSetChanged() {
        updateFeedsFromDB();
    }

    @Override
    public int getCount() {
        if (mEntries == null)
            return 0;
        return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mEntries.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ServiceEntry item = (ServiceEntry)getItem(position);
        if (convertView == null) {
            int viewType = this.getItemViewType(position);
            if (viewType == 1)
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_item_up, parent, false);
            else
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_item_down, parent, false);
        }

        TextView tv = (TextView) convertView.findViewById(R.id.tvTitle);
        tv.setText(item.name);
        tv.setVisibility(TextUtils.isEmpty(item.name) ? View.GONE : View.VISIBLE);
        tv = (TextView) convertView.findViewById(R.id.tvSubTitle);
        tv.setText(item.description);
        tv.setVisibility(TextUtils.isEmpty(item.description) ? View.GONE : View.VISIBLE);
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        ServiceEntry item = (ServiceEntry)getItem(position);
        return item.status;
    }

    public void  populateFromFeed(Feed feed) {
        Item item = null;
        if (feed != null) {
            ArrayList<Item> items = feed.getItems();
            if (items != null && items.size() > 0) {
                for (Item i : items) {
                    if (item == null || item.getPubDate() == null ||
                            (i.getPubDate() != null && i.getPubDate().compareTo(item.getPubDate()) > 0)) {
                        item = i;
                    }
                }
            }
        }
        populateFromItem(item);
    }

    private void populateFromItem(Item item) {
        if (item != null) {
            try {
                //Resources res = App.getContext().getResources();
                //InputStream in_s = res.openRawResource(R.raw.services);
                //byte[] b = new byte[in_s.available()];
                //in_s.read(b);
                //String json = new String(b);

                String json = item.getContentEncoded();
                mServiceList = ServiceDescription.fromJSONString(json);
                mEntries = new ArrayList<ServiceEntry>();
                for (Technology t : mServiceList.technologies.values()) {
                    if (t.pluggable_transports == null || t.pluggable_transports.size() == 0) {
                        ServiceEntry e = new ServiceEntry(t.name, null, t.status);
                        mEntries.add(e);
                    } else {
                        String pluggableTransportPrefix = t.name + " Pluggable Transport\n";
                        StringBuilder sb1 = new StringBuilder();
                        StringBuilder sb2 = new StringBuilder();
                        ServiceEntry e1 = new ServiceEntry(t.name, null, 1);
                        ServiceEntry e2 = new ServiceEntry(t.name, null, 0);
                        for (PluggableTransport pt : t.pluggable_transports) {
                            if (pt.status == 1)
                                sb1.append("\t" + pt.transport_name + "\n");
                            else if (pt.status == 0)
                                sb2.append("\t" + pt.transport_name + "\n");
                        }
                        if (sb1.length() > 0) {
                            e1.description = pluggableTransportPrefix + sb1.toString();
                            mEntries.add(e1);
                        }
                        if (sb2.length() > 0) {
                            e2.description = pluggableTransportPrefix + sb2.toString();
                            mEntries.add(e2);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mServiceList = null;
            mEntries = null;
        }
        SubscribedFeedItemsAdapter.super.notifyDataSetChanged();
        if (mListener != null)
            mListener.onListUpdated(item == null ? null : item.getPubDate());
    }
}
