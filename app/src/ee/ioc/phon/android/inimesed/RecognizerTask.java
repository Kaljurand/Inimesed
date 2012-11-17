package ee.ioc.phon.android.inimesed;

import java.util.concurrent.LinkedBlockingQueue;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.os.Bundle;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.pocketsphinx;

/**
 * Speech recognition task, which runs in a worker thread.
 * 
 * It takes
 * the form of a long-running task which accepts requests to start and stop
 * listening, and emits recognition results to a listener.
 * 
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 * @author Kaarel Kaljurand (minor customization)
 */
public class RecognizerTask implements Runnable {

	/**
	 * This class implements a task which pulls blocks of audio from the system
	 * audio input and places them on a queue.
	 */
	@SuppressLint("NewApi")
	class AudioTask implements Runnable {
		/**
		 * Queue on which audio blocks are placed.
		 */
		LinkedBlockingQueue<short[]> mQueue;
		SpeechAudioRecord mAudioRecord;
		int block_size;
		boolean done;

		static final int DEFAULT_BLOCK_SIZE = 512;

		AudioTask() {
			this(new LinkedBlockingQueue<short[]>(), DEFAULT_BLOCK_SIZE);
		}

		AudioTask(LinkedBlockingQueue<short[]> q) {
			this(q, DEFAULT_BLOCK_SIZE);
		}

		AudioTask(LinkedBlockingQueue<short[]> q, int block_size) {
			this.done = false;
			mQueue = q;
			this.block_size = block_size;
			mAudioRecord = new SpeechAudioRecord(16000, 8192, true, true, true);
		}

		/*
		public int getBlockSize() {
			return block_size;
		}

		public void setBlockSize(int block_size) {
			this.block_size = block_size;
		}

		public LinkedBlockingQueue<short[]> getQueue() {
			return mQueue;
		}
		 */

		public void stop() {
			this.done = true;
		}

		public void run() {
			if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
				mAudioRecord.startRecording();
				while (!this.done) {
					int nshorts = this.readBlock();
					if (nshorts == AudioRecord.ERROR_INVALID_OPERATION) {
						Log.i("read: ERROR_INVALID_OPERATION");
						break;
					} else if (nshorts == AudioRecord.ERROR_BAD_VALUE) {
						Log.i("read: ERROR_BAD_VALUE");
						break;
					} else if (nshorts <= 0) {
						Log.i("read: " + nshorts);
						break;
					}
				}
				mAudioRecord.stop();
			} else {
				Log.i("AudioRecord not initialized");
			}
			mAudioRecord.release();
		}

		private int readBlock() {
			short[] buf = new short[this.block_size];
			int nshorts = mAudioRecord.read(buf, 0, buf.length);
			if (nshorts > 0) {
				Log.i("Posting " + nshorts + " samples to queue");
				mQueue.add(buf);
			}
			return nshorts;
		}
	}

	/**
	 * PocketSphinx native decoder object.
	 */
	Decoder mDecoder;
	/**
	 * Audio recording task.
	 */
	AudioTask mAudioTask;
	/**
	 * Thread associated with recording task.
	 */
	Thread audio_thread;
	/**
	 * Queue of audio buffers.
	 */
	LinkedBlockingQueue<short[]> mAudioQ;
	/**
	 * Listener for recognition results.
	 */
	RecognitionListener mRecListener;
	/**
	 * Whether to report partial results.
	 */
	boolean use_partials;

	/**
	 * State of the main loop.
	 */
	enum State {
		IDLE, LISTENING
	};
	/**
	 * Events for main loop.
	 */
	enum Event {
		NONE, START, STOP, SHUTDOWN
	};

	/**
	 * Current event.
	 */
	Event mailbox;

	public RecognitionListener getRecognitionListener() {
		return mRecListener;
	}

	public void setRecognitionListener(RecognitionListener rl) {
		mRecListener = rl;
	}

	public void setUsePartials(boolean use_partials) {
		this.use_partials = use_partials;
	}

	public boolean getUsePartials() {
		return this.use_partials;
	}

	/**
	 * TODO: move the conf. parameters to the resources
	 */
	public RecognizerTask(DataFiles df) {
		df.createRawLogDir();
		pocketsphinx.setLogfile(df.getLogfile());
		Config c = new Config();

		c.setString("-hmm",  df.getHmm());
		c.setString("-dict", df.getDict());
		c.setString("-jsgf",   df.getJsgf());
		c.setString("-rawlogdir", df.getRawLogDir());
		c.setFloat("-samprate", (float) df.getSampleRateInHz());
		c.setInt("-maxhmmpf", 2000);
		c.setInt("-maxwpf", 10);
		c.setInt("-pl_window", 2);
		c.setBoolean("-backtrace", true);
		c.setBoolean("-bestpath", false);
		mDecoder = new Decoder(c);
		mAudioQ = new LinkedBlockingQueue<short[]>();
		this.use_partials = false;
		this.mailbox = Event.NONE;
	}

	public void run() {
		/* Main loop for this thread. */
		boolean done = false;
		/* State of the main loop. */
		State state = State.IDLE;
		/* Previous partial hypothesis. */
		String partial_hyp = null;

		while (!done) {
			/* Read the mail. */
			Event todo = Event.NONE;
			synchronized (this.mailbox) {
				todo = this.mailbox;
				/* If we're idle then wait for something to happen. */
				if (state == State.IDLE && todo == Event.NONE) {
					try {
						Log.i("waiting");
						this.mailbox.wait();
						todo = this.mailbox;
						Log.i("got" + todo);
					} catch (InterruptedException e) {
						/* Quit main loop. */
						Log.e("Interrupted waiting for mailbox, shutting down");
						todo = Event.SHUTDOWN;
					}
				}
				/* Reset the mailbox before releasing, to avoid race condition. */
				this.mailbox = Event.NONE;
			}
			/* Do whatever the mail says to do. */
			switch (todo) {
			case NONE:
				if (state == State.IDLE)
					Log.e("Received NONE in mailbox when IDLE, threading error?");
				break;
			case START:
				if (state == State.IDLE) { 
					Log.i("START");
					mAudioTask = new AudioTask(mAudioQ, 1024);
					this.audio_thread = new Thread(mAudioTask);
					mDecoder.startUtt();
					this.audio_thread.start();
					state = State.LISTENING;
				}
				else
					Log.e("Received START in mailbox when LISTENING");
				break;
			case STOP:
				if (state == State.IDLE)
					Log.e("Received STOP in mailbox when IDLE");
				else {
					Log.i("STOP");
					assert mAudioTask != null;
					mAudioTask.stop();
					try {
						this.audio_thread.join();
					}
					catch (InterruptedException e) {
						Log.e("Interrupted waiting for audio thread, shutting down");
						done = true;
					}
					// Signal here that transcription has started
					if (mRecListener != null) {
						mRecListener.onEndOfSpeech();
					}
					/* Drain the audio queue. */
					short[] buf;
					while ((buf = mAudioQ.poll()) != null) {
						Log.i("Reading " + buf.length + " samples from queue");
						mDecoder.processRaw(buf, buf.length, false, false);
					}
					mDecoder.endUtt();
					mAudioTask = null;
					this.audio_thread = null;
					if (mRecListener != null) {
						Hypothesis hyp = mDecoder.getHyp();
						if (hyp == null) {
							Log.i("Recognition failure");
							mRecListener.onError(-1);
						}
						else {
							Bundle b = new Bundle();
							Log.i("Final hypothesis: " + hyp.getHypstr() + ", score: " + hyp.getBest_score());
							b.putString("hyp", hyp.getHypstr());
							b.putInt("bestScore", hyp.getBest_score());
							mRecListener.onResults(b);
						}
					}
					state = State.IDLE;
				}
				break;
			case SHUTDOWN:
				Log.i("SHUTDOWN");
				if (mAudioTask != null) {
					mAudioTask.stop();
					assert this.audio_thread != null;
					try {
						this.audio_thread.join();
					}
					catch (InterruptedException e) {
						/* We don't care! */
					}
				}
				mDecoder.endUtt();
				mAudioTask = null;
				this.audio_thread = null;
				state = State.IDLE;
				done = true;
				break;
			}
			/* Do whatever's appropriate for the current state. Actually this just means processing audio if possible. */
			if (state == State.LISTENING) {
				assert mAudioTask != null;
				try {
					short[] buf = mAudioQ.take();
					Log.i("Reading " + buf.length + " samples from queue");
					mDecoder.processRaw(buf, buf.length, false, false);
					Hypothesis hyp = mDecoder.getHyp();
					if (hyp != null) {
						String hypstr = hyp.getHypstr();
						if (hypstr != partial_hyp) {
							Log.i("Hypothesis: " + hyp.getHypstr() + ", score: " + hyp.getBest_score());
							if (mRecListener != null && hyp != null) {
								Bundle b = new Bundle();
								b.putString("hyp", hyp.getHypstr());
								mRecListener.onPartialResults(b);
							}
						}
						partial_hyp = hypstr;
					}
				} catch (InterruptedException e) {
					Log.i("Interrupted in audioq.take");
				}
			}
		}
	}

	public void start() {
		Log.i("signalling START");
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			Log.i("signalled START");
			this.mailbox = Event.START;
		}
	}

	public void stop() {
		Log.i("signalling STOP");
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			Log.i("signalled STOP");
			this.mailbox = Event.STOP;
		}
	}


	/**
	 * TODO: this is not called from anywhere, should it be?
	 */
	public void shutdown() {
		Log.i("signalling SHUTDOWN");
		synchronized (this.mailbox) {
			this.mailbox.notifyAll();
			Log.i("signalled SHUTDOWN");
			this.mailbox = Event.SHUTDOWN;
		}
	}
}