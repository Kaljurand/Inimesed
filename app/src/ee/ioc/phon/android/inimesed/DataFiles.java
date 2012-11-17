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

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import android.os.Environment;

/**
 * <p>Names of the files and directories that are stored on
 * the external storage.</p>
 *
 * @author Kaarel Kaljurand
 */
public class DataFiles {

	public static final String DIR = "Android/data/" + "ee.ioc.phon.android.inimesed" + "/files/";

	private final int mSampleRateInHz;

	private final File mFileHmm;
	private final File mFileJsgf;
	private final File mFileDict;
	private final File mFileLog;
	private final File mDirRawLog;

	DataFiles(Locale locale) {
		this(locale, 16000);
	}

	DataFiles(Locale locale, int sampleRate) {
		String baseDirAsString = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
		mFileHmm = new File(baseDirAsString + DIR + "/hmm/" + locale + "/" + sampleRate);
		mFileJsgf = new File(baseDirAsString + DIR + "/lm/" +  locale + "/lm.jsgf");
		mFileDict = new File(baseDirAsString + DIR + "/lm/" +  locale + "/lm.dic");
		mFileLog = new File(baseDirAsString + DIR + "/pocketsphinx.log");
		mDirRawLog = new File(baseDirAsString + DIR + "/raw/");
		mSampleRateInHz = sampleRate;
	}

	public boolean deleteDict() {
		return mFileDict.delete();
	}

	public boolean deleteLogfile() {
		return mFileLog.delete();
	}

	public boolean deleteJsgf() {
		return mFileJsgf.delete();
	}

	public boolean deleteRawLogDir() {
		try {
			FileUtils.cleanDirectory(mDirRawLog);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean createRawLogDir() {
		if (! mDirRawLog.exists()) {
			try {
				FileUtils.forceMkdir(mDirRawLog);
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	public String getLogfile() {
		return mFileLog.getAbsolutePath();
	}

	public String getRawLogDir() {
		return mDirRawLog.getAbsolutePath();
	}

	public String getHmm() {
		return mFileHmm.getAbsolutePath();
	}

	public String getDict() {
		return mFileDict.getAbsolutePath();
	}

	public String getJsgf() {
		return mFileJsgf.getAbsolutePath();
	}

	public int getSampleRateInHz() {
		return mSampleRateInHz;
	}

}