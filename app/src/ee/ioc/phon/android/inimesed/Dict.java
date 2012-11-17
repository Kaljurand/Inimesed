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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.text.TextUtils;

/**
 * <p>Dictionary generation.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Dict {

	private final StringBuilder mDict = new StringBuilder();
	private final Set<String> mWords = new HashSet<String>();

	private static final String NL = System.getProperty("line.separator");

	public Dict(Context context) {
		String[] actions = context.getResources().getStringArray(R.array.wordsActionContact);
		String[] phones = context.getResources().getStringArray(R.array.wordsPhoneType);
		add(actions);
		add(phones);
		add(context.getResources().getStringArray(R.array.wordsOther));
	}


	public void add(String[] strings) {
		for (String str : strings) {
			add(str);
		}
	}


	public void add(String str) {
		if (! mWords.contains(str)) {
			add(str, asStr(PhonMapper.getPhons(str)));
		}
	}


	public void add(String key, String value) {
		if (! mWords.contains(key)) {
			mDict.append(key);
			mDict.append("  "); // two spaces
			mDict.append(value);
			mDict.append(NL);
			mWords.add(key);
		}
	}


	public String toString() {
		return mDict.toString();
	}


	public static String asStr(List<String> phons) {
		// If the dictionary entry contains only non-pronounceable characters,
		// then we represent it as "mingi jura".
		if (phons.isEmpty()) {
			return "m i n k i j u r a";
		}
		return TextUtils.join(" ", phons);
	}

}