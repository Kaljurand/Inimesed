package ee.ioc.phon.android.inimesed;

import android.os.Bundle;

/**
 * Interface for speech recognition callbacks.
 * 
 * Essentially a cut-down version of {@link android.speech.RecognitionListener},
 * to avoid dependencies on Froyo and methods we don't need or can't provide.
 * 
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 * 
 */
public interface RecognitionListener {
	/**
	 * Called on the recognition thread when partial results are available.
	 * 
	 * Note: This is not like android.speech.RecognitionListener in that it does
	 * not get called on the main thread.
	 * 
	 * @param b
	 *            Bundle containing the partial result string under the "hyp"
	 *            key.
	 */
	void onPartialResults(Bundle b);

	/**
	 * Called when final results are available.
	 * 
	 * Note: This is not like android.speech.RecognitionListener in that it does
	 * not get called on the main thread.
	 * 
	 * @param b
	 *            Bundle containing the final result string under the "hyp" key.
	 */
	void onResults(Bundle b);

	/**
	 * Called if a recognition error occurred.
	 * 
	 * Note: This will only ever be passed -1 for the moment, which corresponds
	 * to a recognition failure (null result).
	 * 
	 * @param err
	 *            Code representing the error that occurred.
	 */
	void onError(int err);

	void onEndOfSpeech();
}