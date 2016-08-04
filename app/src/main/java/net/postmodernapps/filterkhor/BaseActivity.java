package net.postmodernapps.filterkhor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class BaseActivity extends AppCompatActivity {

    private boolean mIsResumed;
    private boolean mRecreateOnResume;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.getInstance().setCurrentLanguageInConfig(this);
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(this);
        mInflater = inflater.cloneInContext(this);
        LayoutInflaterCompat.setFactory(mInflater, new LayoutFactoryWrapper(inflater.getFactory()));
        LocalBroadcastManager.getInstance(this).registerReceiver(mLanguageChangeReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLanguageChangeReceiver);
    }

    @Override
    protected void onStop() {
        mIsResumed = false;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
        if (mRecreateOnResume)
            recreateActivity();
    }

    @Override
    protected void onPause() {
        mIsResumed = false;
        super.onPause();
    }

    private final BroadcastReceiver mLanguageChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsResumed)
                recreateActivity();
            else
                mRecreateOnResume = true;
        }
    };

    private void recreateActivity() {
        mRecreateOnResume = false;
        this.recreate();
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater != null)
                return mInflater;
        }
        return super.getSystemService(name);
    }

    public class LayoutFactoryWrapper implements LayoutInflaterFactory
    {
        private android.view.LayoutInflater.Factory mParent;

        public LayoutFactoryWrapper(LayoutInflater.Factory parent)
        {
            mParent = parent;
        }

        @Override
        public View onCreateView(View view, String name, Context context, AttributeSet attrs)
        {
            View ret = App.createView(name, context, attrs);
            if (ret == null && mParent != null)
            {
                ret = mParent.onCreateView(name, context, attrs);
            }
            return ret;
        }
    }
}
