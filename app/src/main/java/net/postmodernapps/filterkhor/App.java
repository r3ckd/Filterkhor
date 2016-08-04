package net.postmodernapps.filterkhor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.tinymission.rss.Feed;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import net.postmodernapps.filterkhor.uiutil.FontManager;
import net.postmodernapps.filterkhor.widgets.MirroringImageView;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;

public class App extends MultiDexApplication implements SharedPreferences.OnSharedPreferenceChangeListener, ICacheWordSubscriber, SocialReader.SocialReaderFeedPreprocessor {
    public static final String LOGTAG = "App";
    public static final boolean LOGGING = true;

    public static final String EXIT_BROADCAST_ACTION = "net.postmodernapps.filterkhor.exit";
    public static final String SET_UI_LANGUAGE_BROADCAST_ACTION = "net.postmodernapps.filterkhor.setuilanguage";

    private static App m_singleton;

    public static Context m_context;
    public static SettingsUI m_settings;
    private static CacheWordHandler m_cacheWord;

    public SocialReader socialReader;
    private String mCurrentLanguage;

    private static final Charset ASCII = Charset.forName("US-ASCII");

    @Override
    public void onCreate() {
        m_singleton = this;
        m_context = this;
        m_settings = new SettingsUI(m_context);

        // Currently hardcode Farsi
        m_settings.setUiLanguage(Settings.UiLanguage.Farsi);

        applyUiLanguage(false);
        super.onCreate();

        socialReader = SocialReader.getInstance(this.getApplicationContext());
        socialReader.setCacheWordTimeout(0);
        socialReader.setFeedPreprocessor(this);

        m_cacheWord = new CacheWordHandler(this);
        m_cacheWord.connectToService();

        m_settings.registerChangeListener(this);

        mCurrentLanguage = getBaseContext().getResources().getConfiguration().locale.getLanguage();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static Context getContext() {
        return m_context;
    }

    public static App getInstance() {
        return m_singleton;
    }

    public static SettingsUI getSettings() {
        return m_settings;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Settings.KEY_UI_LANGUAGE)) {
            applyUiLanguage(true);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyUiLanguage(false);
    }

    private void applyUiLanguage(boolean sendNotifications) {
        // Update language!
        //
        String language = m_settings.uiLanguageCode();
        if (language.equals(mCurrentLanguage))
            return;
        mCurrentLanguage = language;
        setCurrentLanguageInConfig(m_context);

        // Notify activities (if any)
        if (sendNotifications)
            LocalBroadcastManager.getInstance(m_context).sendBroadcastSync(new Intent(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
    }

    public void setCurrentLanguageInConfig(Context context) {
        Configuration config = new Configuration();
        String language = m_settings.uiLanguageCode();
        Locale loc = new Locale(language);
        if (Build.VERSION.SDK_INT >= 17)
            config.setLocale(loc);
        else
            config.locale = loc;
        Locale.setDefault(loc);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    private String getCWPassword() {

        String passphrase = App.getSettings().launchPassphrase();
        if (TextUtils.isEmpty(passphrase)) {
            passphrase = UUID.randomUUID().toString();
            App.getSettings().setLaunchPassphrase(passphrase);
        }
        return passphrase;
    }

    @Override
    public void onCacheWordUninitialized() {
        try {
            m_cacheWord.setPassphrase(getCWPassword().toCharArray());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCacheWordLocked() {
        try {
            m_cacheWord.setPassphrase(getCWPassword().toCharArray());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCacheWordOpened() {}


    public static View createView(String name, Context context, AttributeSet attrs)
    {
        View returnView = null;

        if (name.equals("ImageView"))
        {
            TypedArray a = context.obtainStyledAttributes(attrs, new int[] { R.attr.doNotMirror });
            boolean dontMirror = false;
            if (a != null)
                dontMirror = a.getBoolean(0, false);
            a.recycle();
            if (dontMirror)
                returnView = new ImageView(context, attrs);
            else
                returnView = new MirroringImageView(context, attrs);
        }

        // API 17 still has some trouble with handling RTL layouts automatically.
        if (name.equals("RelativeLayout") && Build.VERSION.SDK_INT == 17 && getInstance().isRTL()) {
            if (returnView == null)
                returnView = new RelativeLayout(context, attrs);
            RelativeLayout relativeLayout = (RelativeLayout) returnView;
            relativeLayout.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) child.getLayoutParams();
                    if (lp != null) {
                        int[] rules = lp.getRules();
                        if (rules[RelativeLayout.START_OF] != 0) {
                            lp.removeRule(RelativeLayout.LEFT_OF);
                        }
                        if (rules[RelativeLayout.END_OF] != 0) {
                            lp.removeRule(RelativeLayout.RIGHT_OF);
                        }
                        if (rules[RelativeLayout.ALIGN_PARENT_START] != 0) {
                            lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        }
                        if (rules[RelativeLayout.ALIGN_PARENT_END] != 0) {
                            lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        }
                    }
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            });
        }

        if (attrs != null && (returnView == null || returnView instanceof TextView)) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView);
            String fontName = a.getString(R.styleable.CustomFontTextView_font);
            int idAppearance = a.getResourceId(R.styleable.CustomFontTextView_android_textAppearance, 0);
            a.recycle();
            if (fontName == null && idAppearance != 0) {
                a = context.getTheme().obtainStyledAttributes(idAppearance, R.styleable.CustomFontTextView);
                if (a != null)
                {
                    fontName = a.getString(R.styleable.CustomFontTextView_font);
                    a.recycle();
                }
            }
            if (fontName != null)
            {
                Typeface font = FontManager.getFontByName(context, fontName);
                if (font != null) {

                    // Is the view created?
                    if (returnView == null)
                    {
                        if (name.equals("TextView")) {
                            returnView = new TextView(context, attrs);
                        } else if (name.equals("EditText")) {
                            returnView = new EditText(context, attrs);
                        } else if (name.equals("RadioButton")) {
                            returnView = new RadioButton(context, attrs);
                        } else if (name.equals("Button")) {
                            returnView = new Button(context, attrs);
                        }
                    }
                    if (returnView != null) {
                        TextView tv = (TextView)returnView;
                        tv.setTypeface(font);
                    }
                }
            }
        }

        return returnView;
    }

    public boolean isRTL()
    {
        if (Build.VERSION.SDK_INT >= 17)
        {
            return (getBaseContext().getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        }
        else
        {
            // Handle old devices by looking at current language
            Configuration config = getBaseContext().getResources().getConfiguration();
            if (config.locale != null)
            {
                String language = config.locale.getLanguage();
                if (language.startsWith("ar") || language.startsWith("fa"))
                    return true;
            }
            return false;
        }
    }

    @Override
    public String onGetFeedURL(Feed feed) {
        if (feed.getFeedURL().contains("%DATEHASH%")) {
            // Get the date in UTC format
            DateFormat df = new SimpleDateFormat("ddMMyyyy", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcDate = df.format(new Date());

            // Construct the filename
            String fileName = "gt_" + utcDate + "_IR";

            // Hash it!
            final HashCode hashCode = Hashing.sha1().hashString(fileName, Charset.defaultCharset());

            String url = feed.getFeedURL().replace("%DATEHASH%", hashCode.toString());
            return url;
        }
        return null;
    }

    @Override
    public InputStream onFeedDownloaded(Feed feed, final InputStream content, Map<String, String> headers) {

        PipedInputStream in = null;

        try {
            final PipedOutputStream out = new PipedOutputStream();
            in = new PipedInputStream(out);

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create to String
                        StringBuilder sb=new StringBuilder();
                        BufferedReader br = new BufferedReader(new InputStreamReader(content));
                        String read;
                        while((read = br.readLine()) != null) {
                            sb.append(read);
                        }

                        String[] cipherParts = sb.toString().replace("\n", "").replace("\r", "").split(":");
                        String base64Data = cipherParts[0];
                        String base64IV = cipherParts[1];

                        String encryptionKey = "be97aa8a79ad039093cb34fb9714bd7f1caa33ec691a543acfd403a9286d5b54";

                        byte[] iv = Base64.decode(base64IV, Base64.DEFAULT);
                        byte [] cipherBytes = Base64.decode(base64Data, Base64.DEFAULT);

                        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(App.hexStringToByteArray(encryptionKey), "AES"), new IvParameterSpec(iv));

                        byte[] result = cipher.doFinal(cipherBytes);
                        int cb = result.length;
                        if (cb > 1) {
                            int pads = result[cb - 1];
                            cb -= pads;
                        }

                        if (LOGGING) {
                            String res = new String(result, "UTF-8");
                            Log.d(LOGTAG, "Res is " + res);
                        }

                        out.write(result, 0, cb);
                        out.flush();
                        out.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return in;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
