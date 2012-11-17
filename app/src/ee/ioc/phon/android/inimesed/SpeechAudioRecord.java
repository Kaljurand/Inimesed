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

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;

// TODO: this should go into a separate library

@SuppressLint("NewApi")
public class SpeechAudioRecord extends AudioRecord {

	public SpeechAudioRecord(int sampleRateInHz, int bufferSizeInBytes)
			throws IllegalArgumentException {

		this(
				MediaRecorder.AudioSource.VOICE_RECOGNITION,
				sampleRateInHz,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes,
				false,
				false,
				false
				);
	}


	public SpeechAudioRecord(int sampleRateInHz, int bufferSizeInBytes, boolean noise, boolean gain, boolean echo)
			throws IllegalArgumentException {

		this(
				MediaRecorder.AudioSource.VOICE_RECOGNITION,
				sampleRateInHz,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes,
				noise,
				gain,
				echo
				);
	}


	// This is a copy of the AudioRecord constructor
	public SpeechAudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes)
			throws IllegalArgumentException {

		this(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, false, false, false);
	}


	public SpeechAudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes,
			boolean noise, boolean gain, boolean echo)
					throws IllegalArgumentException {

		super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);

		// The following would take effect only on Jelly Bean and higher.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			Log.i("Trying to clean up audio because running on SDK " + Build.VERSION.SDK_INT);

			if (noise && NoiseSuppressor.create(getAudioSessionId()) == null) {
				Log.i("NoiseSuppressor not present :(");
			} else {
				Log.i("NoiseSuppressor enabled!");
			}

			if (gain && AutomaticGainControl.create(getAudioSessionId()) == null) {
				Log.i("AutomaticGainControl not present :(");
			} else {
				Log.i("AutomaticGainControl enabled!");
			}

			if (echo && AcousticEchoCanceler.create(getAudioSessionId()) == null) {
				Log.i("AcousticEchoCanceler not present :(");
			} else {
				Log.i("AcousticEchoCanceler enabled!");
			}
		}
	}
}