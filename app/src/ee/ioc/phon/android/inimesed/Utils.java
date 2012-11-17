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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * <p>Some utils.</p>
 *
 * @author Kaarel Kaljurand
 */
public class Utils {
	public static String getVersionName(Context c) {
		PackageInfo info = getPackageInfo(c);
		if (info == null) {
			return "?.?.?";
		}
		return info.versionName;
	}

	private static PackageInfo getPackageInfo(Context c) {
		PackageManager manager = c.getPackageManager();
		try {
			return manager.getPackageInfo(c.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e("Couldn't find package information in PackageManager");
		}
		return null;
	}
}