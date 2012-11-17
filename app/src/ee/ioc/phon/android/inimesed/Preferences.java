/*
 * Copyright 2012, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.inimesed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech;

/**
 * <p>Preferences activity.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private TextToSpeech mTts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		updatePreference(findPreference(getString(R.string.keyActionContact)));
		populateTtsLanguages();
	}


	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}


	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mTts != null) {
			mTts.shutdown();
		}
	}


	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePreference(findPreference(key));
	}


	private void updatePreference(Preference pref) {
		if (pref instanceof ListPreference) {
			ListPreference lp = (ListPreference) pref;
			if (getString(R.string.titleActionContact).equals(lp.getTitle())) {
				setSummary(pref, getString(R.string.summaryActionContact), lp.getEntry());
			} else if (getString(R.string.titleConfidenceLevel).equals(lp.getTitle())) {
				setSummary(pref, getString(R.string.summaryConfidenceLevel), lp.getEntry());
			}
		}
	}


	private void setSummary(Preference pref, String strText, CharSequence strArg) {
		pref.setSummary(String.format(strText, strArg));
	}


	/**
	 * <p>Populate a list of TTS languages that the user can choose from.
	 * We only list the languages that are
	 * (1) close to Estonian in terms to letter-to-sound mapping,
	 * (2) available in the current TTS engine.</p>
	 */
	private void populateTtsLanguages() {
		mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				ListPreference lp = (ListPreference) findPreference(getString(R.string.keyTtsLanguage));
				if (status == TextToSpeech.SUCCESS) {
					final List<CharSequence> entries = new ArrayList<CharSequence>();
					final List<CharSequence> entryValues = new ArrayList<CharSequence>();
					for (String localeAsString : getResources().getStringArray(R.array.localesTts)) {
						Locale locale = new Locale(localeAsString);
						int result = mTts.isLanguageAvailable(locale);
						if (result >= 0) {
							Log.i(locale.toString() + ": " + locale.getDisplayName(locale));
							entries.add(locale.getDisplayName(locale));
							entryValues.add(locale.toString());
						}
					}
					if (entries.size() > 1) {
						lp.setEntries(entries.toArray(new CharSequence[0]));
						lp.setEntryValues(entryValues.toArray(new CharSequence[0]));
					}
					else {
						lp.setEnabled(false);
					}
				} else {
					lp.setEnabled(false);
				}
			}
		});
	}

}