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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Simple letter-to-phonetic symbol mapper. Inspired by:
 *
 * https://github.com/alumae/et-pocketsphinx-tutorial/blob/master/scripts/est-l2p.py
 *
 * TODO: replace by: https://github.com/alumae/et-g2p
 *
 * @author Kaarel Kaljurand
 */
public class PhonMapper {

	// Context sensitive handling of certain characters, e.g. kpt and i
	// Assumes lowercase characters.
	private static final Map<Pattern, String> PATTERNS;
	static {
		Map<Pattern, String> patterns = new HashMap<Pattern, String>();
		patterns.put(Pattern.compile("kk"), "K");
		patterns.put(Pattern.compile("pp"), "P");
		patterns.put(Pattern.compile("tt"), "T");
		patterns.put(Pattern.compile("ph"), "f");
		patterns.put(Pattern.compile("sch"), "S");
		patterns.put(Pattern.compile("^ch([aeiou])"), "tS$1");
		patterns.put(Pattern.compile("^ch([^aeiou])"), "k$1"); // Christian == Kristjan
		patterns.put(Pattern.compile("cz"), "tS");
		patterns.put(Pattern.compile("([aeiou])ch"), "$1hh");
		patterns.put(Pattern.compile("([aeiou])ck"), "$1K");
		patterns.put(Pattern.compile("^c(?=[i])"), "s");
		patterns.put(Pattern.compile("([aeiou]|^)sh(?=($|[aeiou]))"), "$1S");
		patterns.put(Pattern.compile("([aeiouõäöümnlrv])k(?=($|[lrmnvjaeiouõäöü]))"), "$1K");
		patterns.put(Pattern.compile("([aeiouõäöümnlrv])p(?=($|[lrmnvjaeiouõäöü]))"), "$1P");
		patterns.put(Pattern.compile("([aeiouõäöümnlrv])t(?=($|[lrmnvjaeiouõäöü]))"), "$1T");
		patterns.put(Pattern.compile("([^aeiouõäöü])i([aeiouõäöü])"), "$1j$2");
		patterns.put(Pattern.compile("([aeiouõäöü])i([aeiouõäöü])"), "$1ij$2");
		// delete word-initial 'h'
		patterns.put(Pattern.compile("^h([aeiouõäöü])"), "$1");
		PATTERNS = Collections.unmodifiableMap(patterns);
	}

	// a ae e f h i j k kk l m n o oe ou p pp r s sh t tt u ue v
	// Keys are formal symbols (e.g. K)
	// Values are phonetic symbols which must agree with the acoustic model.
	private static final Map<String, String> phons;
	static {
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("a", "a");
		aMap.put("á", "a");
		aMap.put("ä", "ae");
		aMap.put("e", "e");
		aMap.put("f", "f");
		aMap.put("h", "h");
		aMap.put("i", "i");
		aMap.put("y", "j");
		aMap.put("j", "j");
		aMap.put("c", "k");
		aMap.put("q", "k");
		aMap.put("g", "k");
		aMap.put("k", "k");
		aMap.put("K", "kk");
		aMap.put("l", "l");
		aMap.put("m", "m");
		aMap.put("n", "n");
		aMap.put("o", "o");
		aMap.put("ö", "oe");
		aMap.put("õ", "ou");
		aMap.put("b", "p");
		aMap.put("p", "p");
		aMap.put("P", "pp");
		aMap.put("r", "r");
		aMap.put("z", "s");
		aMap.put("s", "s");
		aMap.put("ç", "s");
		aMap.put("S", "sh");
		aMap.put("š", "sh");
		aMap.put("ž", "sh");
		aMap.put("d", "t");
		aMap.put("t", "t");
		aMap.put("T", "tt");
		aMap.put("u", "u");
		aMap.put("ü", "ue");
		aMap.put("v", "v");
		aMap.put("w", "v");
		aMap.put("x", "k s");
		phons = Collections.unmodifiableMap(aMap);
	}


	/**
	 * TODO: implement something more sophisticated
	 */
	public static List<String> getPhons(String str) {
		List<String> phons = new ArrayList<String>();
		str = str.toLowerCase();
		for (Entry<Pattern,String> entry : PATTERNS.entrySet()) {
			str = entry.getKey().matcher(str).replaceAll(entry.getValue());
		}
		for (String ch : str.split("")) {
			String phon = getPhon(ch);
			if (phon != null) {
				phons.add(phon);
			}
		}
		return phons;
	}


	private static String getPhon(String str) {
		return phons.get(str);
	}
}