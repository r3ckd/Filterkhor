package net.postmodernapps.filterkhor;

import info.guardianproject.securereader.Settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.ScrollView;

import info.guardianproject.cacheword.ICacheWordSubscriber;

public class SettingsActivity extends BaseActivity implements ICacheWordSubscriber
{
	private static final boolean LOGGING = false;
	private static final String LOGTAG = "Settings";

	Settings mSettings;
	private ScrollView rootView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		setTitle(R.string.action_settings);
		mSettings = App.getSettings();

		rootView = (ScrollView) findViewById(R.id.root);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		populateProfileTab();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void populateProfileTab()
	{
		View tabView = rootView;

		this.hookupRadioButton(tabView, "syncFrequency", Settings.SyncFrequency.class, R.id.rbSyncManual, R.id.rbSyncWhenRunning, R.id.rbSyncInBackground);
		this.hookupRadioButton(tabView, "syncNetwork", Settings.SyncNetwork.class, R.id.rbSyncNetworkWifiAndMobile, R.id.rbSyncNetworkWifiOnly);
	}

	private class ResourceValueMapping
	{
		private final int mResId;
		private final Object mValue;

		public ResourceValueMapping(int resId, Object value)
		{
			mResId = resId;
			mValue = value;
		}

		public int getResId()
		{
			return mResId;
		}

		public Object getValue()
		{
			return mValue;
		}
	}

	private void hookupCheckbox(View parentView, int resIdCheckbox, String methodNameOfGetter)
	{
		if (LOGGING)
			Log.v(LOGTAG, methodNameOfGetter);

		CheckBox cb = (CheckBox) parentView.findViewById(resIdCheckbox);
		if (cb == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to find checkbox: " + resIdCheckbox);
			return;
		}

		try
		{
			String methodNameOfSetter = "set" + String.valueOf(methodNameOfGetter.charAt(0)).toUpperCase() + methodNameOfGetter.substring(1);

			final Method getter = mSettings.getClass().getMethod(methodNameOfGetter, (Class[]) null);
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[] { boolean.class });
			if (getter == null || setter == null)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter);
				return;
			}

			// Set initial value
			cb.setChecked((Boolean) getter.invoke(mSettings, (Object[]) null));

			// Set listener
			cb.setOnCheckedChangeListener(new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
											 boolean isChecked) {
					try
					{
						setter.invoke(mSettings, isChecked);
					}
					catch (Exception e)
					{
						if (LOGGING)
							Log.v(LOGTAG, "Failed checked change listener: " + e.toString());
					}

				}
			});
		}
		catch (NoSuchMethodException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalArgumentException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalAccessException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (InvocationTargetException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
	}

	private void hookupBinaryRadioButton(View parentView, int resIdRadioTrue, int resIdRadioFalse, String methodNameOfGetter)
	{
		hookupRadioButtonWithArray(parentView, methodNameOfGetter, boolean.class, new ResourceValueMapping[] { new ResourceValueMapping(resIdRadioTrue, true),
				new ResourceValueMapping(resIdRadioFalse, false) });
	}

	private void hookupRadioButton(View parentView, String methodNameOfGetter, Class<?> enumClass, int... resIds)
	{
		Object[] constants = enumClass.getEnumConstants();
		if (constants.length != resIds.length)
		{
			if (LOGGING)
				Log.w(LOGTAG, "hookupRadioButton: mismatched classes!");
			return;
		}

		ArrayList<ResourceValueMapping> mappings = new ArrayList<ResourceValueMapping>();

		int idx = 0;
		for (int resId : resIds)
		{
			mappings.add(new ResourceValueMapping(Integer.valueOf(resId), constants[idx++]));
		}

		hookupRadioButtonWithArray(parentView, methodNameOfGetter, enumClass, mappings.toArray(new ResourceValueMapping[] {}));
	}

	private void hookupRadioButtonWithArray(View parentView, String methodNameOfGetter, Class<?> valueType, ResourceValueMapping[] values)
	{
		try
		{
			String methodNameOfSetter = "set" + String.valueOf(methodNameOfGetter.charAt(0)).toUpperCase() + methodNameOfGetter.substring(1);

			final Method getter = mSettings.getClass().getMethod(methodNameOfGetter, (Class[]) null);
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[] { valueType });
			if (getter == null || setter == null)
			{
				if (LOGGING)
					Log.w(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter);
				return;
			}

			RadioButtonChangeListener listener = new RadioButtonChangeListener(mSettings, getter, setter);

			Object currentValueInSettings = getter.invoke(mSettings, (Object[]) null);

			for (ResourceValueMapping value : values)
			{
				int resId = value.getResId();
				if (resId == 0)
					continue; // Ignore this value, cant be set in the ui

				RadioButton rb = (RadioButton) parentView.findViewById(resId);
				if (rb == null)
				{
					if (LOGGING)
						Log.w(LOGTAG, "Failed to find checkbox: " + resId);
					return;
				}
				if (currentValueInSettings.equals(value.getValue()))
					rb.setChecked(true);
				rb.setTag(value.getValue());
				rb.setOnCheckedChangeListener(listener);
			}
		}
		catch (NoSuchMethodException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalArgumentException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalAccessException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (InvocationTargetException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
	}

	private class RadioButtonChangeListener implements RadioButton.OnCheckedChangeListener
	{
		private final Settings mSettings;
		private final Method mGetter;
		private final Method mSetter;

		public RadioButtonChangeListener(Settings settings, Method getter, Method setter)
		{
			mSettings = settings;
			mGetter = getter;
			mSetter = setter;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			try
			{
				if (isChecked)
				{
					Object currentValueInSettings = mGetter.invoke(mSettings, (Object[]) null);
					Object valueOfThisRB = ((RadioButton) buttonView).getTag();
					if (!currentValueInSettings.equals(valueOfThisRB))
						mSetter.invoke(mSettings, valueOfThisRB);
				}
			}
			catch (Exception e)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Failed checked change listener: " + e.toString());
			}
		}
	}

//	@Override
//	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
//	{
//		mLastChangedSetting = key;
//		super.onSharedPreferenceChanged(sharedPreferences, key);
//		if (key == Settings.KEY_PROXY_TYPE)
//		{
//			mSettings.setRequireProxy(mSettings.proxyType() != Settings.ProxyType.None);
//			if (mSettings.requireProxy())
//				App.getInstance().socialReader.connectProxy(this);
//		}
//		if (key.equals(Settings.KEY_PROXY_TYPE) || key.equals(Settings.KEY_SYNC_MODE))
//		{
//			updateLeftSideMenu();
//		}
//	}
//
//	public void screenshotsSectionClicked(View v) {
//		CheckBox check = (CheckBox) v.findViewById(R.id.chkEnableScreenshots);
//		check.toggle();
//	}

	@Override
	public void onCacheWordLocked() {
	}

	@Override
	public void onCacheWordOpened() {
	}

	@Override
	public void onCacheWordUninitialized() {
	}
}
