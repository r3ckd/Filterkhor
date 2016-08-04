package net.postmodernapps.filterkhor;

import info.guardianproject.securereader.Settings;
import android.content.Context;

public class SettingsUI extends Settings
{
	public SettingsUI(Context _context)
	{
		super(_context);
	}

	@Override
	public void resetSettings()
	{
		mPrefs.edit().clear().commit();
		super.resetSettings();
	}

	public String uiLanguageCode() {
		UiLanguage lang = uiLanguage();
		String language = "en";
		if (lang == UiLanguage.Farsi)
			language = "fa";
		else if (lang == UiLanguage.Tibetan)
			language = "bo";
		else if (lang == UiLanguage.Chinese)
			language = "zh";
		else if (lang == UiLanguage.Ukrainian)
			language = "uk";
		else if (lang == UiLanguage.Russian)
			language = "ru";
		else if (lang == UiLanguage.Spanish)
			language = "es";
		else if (lang == UiLanguage.Japanese)
			language = "ja";
		else if (lang == UiLanguage.Norwegian)
			language = "nb";
		else if (lang == UiLanguage.Turkish)
			language = "tr";
		return language;
	}
}
