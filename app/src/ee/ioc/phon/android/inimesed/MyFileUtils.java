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

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MyFileUtils {

	private static final String LOG_TAG = MyFileUtils.class.getName();

	public static void saveFile(File f, String content) throws IOException {
		File dir = f.getParentFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Cannot create directory: " + dir);
		}
		FileUtils.writeStringToFile(f, content, "UTF8");
	}

	public static String loadFile(File f) throws IOException {
		return FileUtils.readFileToString(f, "UTF8");
	}

	public static void copyAssets(Context context, File baseDir) throws IOException {
		AssetManager assetManager = context.getAssets();
		String[] files = assetManager.list("hmm");

		for (String fromFile : files) {
			File toFile = new File(baseDir.getAbsolutePath() + "/" + fromFile);
			Log.i(LOG_TAG, "Copying: " + fromFile + " to " + toFile);
			InputStream in = assetManager.open("hmm/" + fromFile);
			FileUtils.copyInputStreamToFile(in, toFile);
		}
	}
}