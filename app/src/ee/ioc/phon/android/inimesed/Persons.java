package ee.ioc.phon.android.inimesed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Persons {

	public static final String RE_NONPRONOUNCEABLE = "[^A-Za-zÕÄÖÜõäöüáç]";

	public static final String PHON_CONNECTOR = "_";

	// This phrase represents all the names that cannot be converted into the
	// pronunciation format, e.g. because they exclusively
	// use a foreign character set.
	private static final String NONPRONOUNCABLE_NAME = "hääldamatunimi";

	private static final TextUtils.StringSplitter SPACE_SPLITTER = new TextUtils.SimpleStringSplitter(' ');

	private final Map<String, Set<Person>> mPersons = new HashMap<String, Set<Person>>();
	private final StringBuilder mNames = new StringBuilder();
	private final Dict mDict;
	private final boolean mSearchByNameParts;

	Persons(Context context) {
		mDict = new Dict(context);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mSearchByNameParts = prefs.getBoolean(context.getString(R.string.keySearchByNameParts),
				context.getResources().getBoolean(R.bool.defaultSearchByNameParts));
	}

	public boolean containsKey(String token) {
		return mPersons.containsKey(token);
	}


	public Set<Person> get(String token) {
		return mPersons.get(token);
	}


	public String getNames() {
		return mNames.toString();
	}


	public int size() {
		return mPersons.keySet().size();
	}


	public void add(long id, String key, String displayName) {
		String name = displayName.toLowerCase().replaceAll(RE_NONPRONOUNCEABLE, " ").trim().replaceAll(" +", " ");		
		SPACE_SPLITTER.setString(name);
		List<String> phonSplits = new ArrayList<String>();
		for (String split : SPACE_SPLITTER) {
			List<String> phons = PhonMapper.getPhons(split);
			if (! phons.isEmpty()) {
				String splitPhrase = TextUtils.join(PHON_CONNECTOR, phons).replaceAll(" ", PHON_CONNECTOR);
				mDict.add(splitPhrase, TextUtils.join(" ", phons));
				phonSplits.add(splitPhrase);
			}
		}

		if (phonSplits.isEmpty()) {
			mDict.add(NONPRONOUNCABLE_NAME);
			add(NONPRONOUNCABLE_NAME, id, key, displayName);
		} else {
			add(TextUtils.join(" ", phonSplits), id, key, displayName);
			if (mSearchByNameParts) {
				for (int i = 0; i < phonSplits.size(); i++) {
					if (phonSplits.get(i).length() < 3) {
						continue;
					}
					for (int j = i; j < phonSplits.size(); j++) {
						String phrase = phonSplits.get(i);
						for (int k = i + 1; k <= j; k++) {
							if (phonSplits.get(k).length() > 2) {
								phrase += " " + phonSplits.get(k);
							}
						}
						add(phrase, id, key, displayName);
					}
				}
			}
		}
	}


	public Dict getDict() {
		return mDict;
	}


	private void add(String phrase, long id, String key, String displayName) {
		Person person = new Person(id, key, displayName);
		Set<Person> persons = get(phrase);
		if (persons == null) {
			persons = new HashSet<Person>();
			if (mNames.length() > 0) {
				mNames.append(" | ");
			}
			mNames.append(phrase);
		}
		persons.add(person);
		mPersons.put(phrase, persons);
	}
}