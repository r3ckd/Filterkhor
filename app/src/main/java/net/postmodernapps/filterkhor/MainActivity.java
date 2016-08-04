package net.postmodernapps.filterkhor;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Date;

import net.postmodernapps.filterkhor.adapters.SubscribedFeedItemsAdapter;
import net.postmodernapps.filterkhor.uiutil.UIHelpers;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncService;
//import net.hockeyapp.android.CrashManager;
//import net.hockeyapp.android.UpdateManager;

public class MainActivity extends BaseActivity implements SocialReader.SocialReaderLockListener, SubscribedFeedItemsAdapter.SubscribedFeedItemsAdapterListener {

    private TextView mTextViewLastUpdateDate;
    private Date mListLastUpdateDate;
    private Handler mHandler;

    private static final int TICK = 1000 * 60; // Milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHandler = new Handler();

        mTextViewLastUpdateDate = new TextView(this);
        mTextViewLastUpdateDate.setText("");
        mTextViewLastUpdateDate.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.END | Gravity.CENTER_VERTICAL));
        toolbar.addView(mTextViewLastUpdateDate);

        App.getInstance().socialReader.setLockListener(this);
        
        App.getInstance().socialReader.setSyncServiceListener(new SyncService.SyncServiceListener() {
            @Override
            public void syncEvent(SyncService.SyncTask syncTask) {
                if (syncTask.type == SyncService.SyncTask.TYPE_FEED && syncTask.status == SyncService.SyncTask.FINISHED) {
                    updateList();
                }
            }
        });
        //checkForUpdates();
    }

    private void updateList() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.mainFragment);
        if (f != null) {
            MainActivityFragment fragment = (MainActivityFragment)f;
            fragment.updateList();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.getInstance().socialReader.onPause();
        //unregisterManagers();

        mHandler.removeCallbacks(mUpdateTimestamp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.getInstance().socialReader.onResume();
        updateList();
        //checkForCrashes();

        mHandler.removeCallbacks(mUpdateTimestamp);
        mHandler.post(mUpdateTimestamp);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //unregisterManagers();
    }

    /*
    private void checkForCrashes() {
        CrashManager.register(this);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocked() {

    }

    @Override
    public void onUnlocked() {
        updateList();
    }

    @Override
    public void onListUpdated(Date when) {
        mListLastUpdateDate = when;
        mHandler.removeCallbacks(mUpdateTimestamp);
        mHandler.post(mUpdateTimestamp);
    }

    private final Runnable mUpdateTimestamp = new Runnable()
    {
        @Override
        public void run()
        {
            if (mListLastUpdateDate != null) {
                mTextViewLastUpdateDate.setText(UIHelpers.dateDiffDisplayString(mListLastUpdateDate, MainActivity.this, R.string.updated_never, R.string.updated_recently, R.string.updated_minutes, R.string.updated_minute, R.string.updated_hours, R.string.updated_hour, R.string.updated_days, R.string.updated_day));
            } else {
                mTextViewLastUpdateDate.setText(null);
            }

            mHandler.postDelayed(mUpdateTimestamp, TICK);
        }
    };
}
