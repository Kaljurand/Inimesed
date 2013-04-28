/*
 * Copyright 2012-2013, Institute of Cybernetics at Tallinn University of Technology
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * <p>The main activity.</p>
 *
 * @author Kaarel Kaljurand
 */
public class InimesedActivity extends Activity {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	public static final String DEFAULT_SORT_ORDER = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

	// TODO: take all the strings from the resources
	// These are the symbols that the internal search phrase is made of, i.e.
	// lowercase letters, space, and underscore.
	public static final String RE_NAME = "[a-z _]+";
	public static final Pattern RE_COMMAND =
			Pattern.compile("(helista|vali) palun (" + RE_NAME + ")");

	private static final String UTT_COMPLETED_FEEDBACK = "UTT_COMPLETED_FEEDBACK";

	private static final String LOG_TAG = InimesedActivity.class.getName();

	private static final String MSG = "MSG";
	private static final int MSG_STATUS = 1;
	private static final int MSG_INFO = 2;

	private SimpleMessageHandler mMessageHandler;

	private final DataFiles mDf = new DataFiles(new Locale("et_EE"));

	// Recognizer task, which runs in a worker thread.
	private RecognizerTask mRt;

	// Time at which current recognition started.
	private Date mStartDate;

	// Number of seconds of speech.
	private float mSpeechDur;

	// Are we listening?
	boolean mListening = false;

	// Label for: instructions, recognizing..., command
	private TextView mTvStatus;

	// Label for: loading messages, error messages
	private TextView mTvInfo;

	private ListView mLvContacts;

	private static final int TTS_DATA_CHECK_CODE = 1;
	private TextToSpeech mTts;

	private String mCurrentSortOrder;

	private SharedPreferences mPrefs;
	private String mSelection;
	private Cursor mCursor;
	private ContactsGrammar mCg;
	private Resources mRes;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRes = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// TODO: this should be done also when TTS is switched on in the settings
		if (isUseTts()) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);
		}

		mSelection = "(" + ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'" + ")";

		if (mPrefs.getBoolean(getString(R.string.keyShowHasPhoneNumber), mRes.getBoolean(R.bool.defaultShowHasPhoneNumber))) {
			mSelection = mSelection + " AND " + "(" + ContactsContract.Contacts.HAS_PHONE_NUMBER + " = '1')";
		}

		Log.i(LOG_TAG, "DB query: " + mSelection);

		mTvInfo = (TextView) findViewById(R.id.tvInfo);
		mTvStatus = (TextView) findViewById(R.id.tvStatus);

		mMessageHandler = new SimpleMessageHandler(mTvStatus, mTvInfo);

		mLvContacts = (ListView) findViewById(R.id.lvContacts);
		mLvContacts.setFastScrollEnabled(true);

		mLvContacts.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Cursor cursor = (Cursor) parent.getItemAtPosition(position); 
				String key = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
				handleContact(id, key,
						ActionType.convertIntentActionToActionType(
								mPrefs.getString(getString(R.string.keyActionContact), Intent.ACTION_VIEW)));
			}
		});

		mCurrentSortOrder = mPrefs.getString(getString(R.string.prefCurrentSortOrder), DEFAULT_SORT_ORDER);

		populateContactList(false, mCurrentSortOrder);
		new LoadRecognizer().execute();
	}


	@Override
	public void onStart() {
		super.onStart();
		updateTitle();
	}


	@Override
	public void onStop() {
		super.onStop();
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(getString(R.string.prefCurrentSortOrder), mCurrentSortOrder);
		editor.commit();
	}


	@Override
	public void onDestroy() {
		Log.i("onDestroy");
		super.onDestroy();

		// Stop TTS
		if (mTts != null) {
			mTts.shutdown();
		}

		// Close the contacts cursor
		mCursor.close();

		// Delete grammar, dict, and log
		boolean success = mDf.deleteDict();
		if (!success) {
			Log.e("Failed to delete: " + mDf.getDict());
		}
		mDf.deleteJsgf();
		mDf.deleteLogfile();
		mDf.deleteRawLogDir();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuMainSortByName:
			updateQuery(ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
			return true;
		case R.id.menuMainSortByLastTimeContacted:
			updateQuery(ContactsContract.Contacts.LAST_TIME_CONTACTED + " DESC");
			return true;
		case R.id.menuMainSortByTimesContacted:
			updateQuery(ContactsContract.Contacts.TIMES_CONTACTED + " DESC");
			return true;
		case R.id.menuMainSortByStarred:
			updateQuery(ContactsContract.Contacts.STARRED + " DESC");
			return true;
		case R.id.menuMainSettings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.menuMainAbout:
			startActivity(new Intent(this, About.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		Log.i(LOG_TAG, "Key down: " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
			if (! mListening) actionDown();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)  {
		Log.i(LOG_TAG, "Key up: " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
			if (mListening) actionUp();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TTS_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				Log.i(LOG_TAG, "CHECK_VOICE_DATA_PASS");
				mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS) {
							String selectedLanguage = mPrefs.getString(getString(R.string.keyTtsLanguage), getString(R.string.defaultTtsLanguage));
							boolean success = setTtsLang(selectedLanguage);
							if (! success) {
								// TODO: this message will not be shown very long,
								// so the user will probably not notice it.
								mTvInfo.setText(String.format(getString(R.string.errorTtsLangNotAvailable), selectedLanguage));
							}
						} else {
							mTvInfo.setText(getString(R.string.errorTtsInitError));
							Log.e(LOG_TAG, getString(R.string.errorTtsInitError));
						}
					}
				});
			} else {
				Log.i(LOG_TAG, "Need to install TTS data");
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}


	private void confirmAndExecute(final long id, final String key, final ActionType actionType, String command) {
		if (mTts != null) {
			mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
				@Override
				public void onUtteranceCompleted(String utteranceId) {
					Log.i(LOG_TAG, "onUtteranceCompleted: " + utteranceId);
					if (utteranceId.equals(UTT_COMPLETED_FEEDBACK)) {
						handleContact(id, key, actionType);
					}
				}
			});
			HashMap<String, String> params = new HashMap<String, String>();
			// The default seems to be STREAM_MUSIC, so let's use that unless there is a good reason to
			// use something else. The goal is to get the TTS into the headset, if it's plugged in,
			// the STREAM_ALARM did not go into the headset.
			//params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTT_COMPLETED_FEEDBACK);
			mTts.speak(command, TextToSpeech.QUEUE_FLUSH, params);
			// mTts.speak(command, TextToSpeech.QUEUE_ADD, params);
		}
	}


	/**
	 * <p>Try to set the TTS engine to speak in the given language.
	 * If it fails then return <code>false</code>. It would still speak,
	 * we just don't know which language it will be.</p>
	 */
	private boolean setTtsLang(String localeAsStr) {
		if (localeAsStr == null) {
			return false;
		}
		Log.i(LOG_TAG, "Default TTS engine:" + mTts.getDefaultEngine());
		Locale locale = new Locale(localeAsStr);
		if (mTts.isLanguageAvailable(locale) >= 0) {
			mTts.setLanguage(locale);
			Log.i(LOG_TAG, "Set TTS to locale: " + locale);
			return true;
		}
		Log.e(LOG_TAG, String.format(getString(R.string.errorTtsLangNotAvailable), localeAsStr));
		return false;
	}


	private void actionDown() {
		mStartDate = new Date();
		mListening = true;
		mTvStatus.setText("");
		mTvInfo.setText("");
		mRt.start();
	}


	private void actionUp() {
		Date end_date = new Date();
		long nmsec = end_date.getTime() - mStartDate.getTime();
		mSpeechDur = (float)nmsec / 1000;
		mRt.stop();
		if (mListening) {
			mListening = false;
		}
	}


	private void populateContactList(boolean showInvisible, String sortOrder) {
		mCursor = getContacts(showInvisible, sortOrder);
		String[] fields = new String[] {
				ContactsContract.Contacts.PHOTO_ID, // TODO: this is currently ignored
				ContactsContract.Data.DISPLAY_NAME,
				ContactsContract.Contacts.STARRED,
				ContactsContract.Contacts.LAST_TIME_CONTACTED,
				ContactsContract.Contacts.TIMES_CONTACTED
		};
		int[] slots = new int[] {
				R.id.contactIcon,
				R.id.contactDisplayName,
				R.id.contactStarred,
				R.id.contactLastTimeContacted,
				R.id.contactTimesContacted,
		};
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_item_contact, mCursor, fields, slots);

		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			private final String formatDate = getString(R.string.formatDate);
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == cursor.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED)) {
					long lastTimeContacted = cursor.getLong(columnIndex);
					if (lastTimeContacted == 0) {
						view.setVisibility(View.GONE);
					} else {
						((TextView) view).setText(DateFormat.format(formatDate, (long) lastTimeContacted));
						view.setVisibility(View.VISIBLE);
					}
					return true;
				} else if (columnIndex == cursor.getColumnIndex(ContactsContract.Contacts.STARRED)) {
					if (cursor.getInt(columnIndex) == 1) {
						view.setVisibility(View.VISIBLE);
					} else {
						view.setVisibility(View.GONE);
					}
					return true;
				} else if (columnIndex == cursor.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)) {
					int timesContacted = cursor.getInt(columnIndex);
					if (timesContacted > 0) {
						((TextView) view).setText("" + timesContacted);
						view.setVisibility(View.VISIBLE);
					} else {
						view.setVisibility(View.GONE);
					}
					return true;
				} else if (columnIndex == cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID)) {
					// TODO: we currently do not use the PHOTO_ID
					long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
					Uri uri = getContactUri(id);
					InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), uri);

					if (input == null) {
						((ImageView) view).setImageResource(R.drawable.ic_contact_picture);
					} else {
						((ImageView) view).setImageBitmap(BitmapFactory.decodeStream(input));
					}
					return true;
				}
				return false;
			}
		});

		mLvContacts.setAdapter(adapter);
	}


	/**
	 * Obtains the contact list for the currently selected account.
	 *
	 * @return A cursor for for accessing the contact list.
	 */
	private Cursor getContacts(boolean showInvisible, String sortOrder) {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.LOOKUP_KEY,
				ContactsContract.Contacts.PHOTO_ID, // TODO: this is currently ignored
				ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.Contacts.STARRED,
				ContactsContract.Contacts.LAST_TIME_CONTACTED,
				ContactsContract.Contacts.TIMES_CONTACTED
		};
		String[] selectionArgs = null;
		return managedQuery(uri, projection, mSelection, selectionArgs, sortOrder);
	}


	private void updateQuery(String sortOrder) {
		mCurrentSortOrder = sortOrder;
		populateContactList(false, sortOrder);
	}


	private void handleContact(long id, String key, ActionType actionType) {
		Intent inIntent = getIntent();
		Log.i("IN: " + inIntent.getAction() + " " + inIntent.getData());
		if (inIntent.getAction().equals(Intent.ACTION_PICK)) {
			Intent intent = new Intent();
			Uri uri = getContactUri(id);
			// TODO: this did not work
			//Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key);
			Log.i("OUT: " + uri);
			intent.setData(uri);
			setResult(Activity.RESULT_OK, intent);
			finish();
		} else {
			launchActionViewByKey(id, key, actionType);
		}
	}


	private void handleContactSelectedBySpeech(ContactsGrammar cg, String command, float confidence) {
		ActionType actionType = ActionType.VIEW;
		String name = command;

		// parse the command to establish name
		Matcher m = RE_COMMAND.matcher(command);
		if (m.matches()) {
			actionType = ActionType.convertTagToActionType(m.group(1));
			name = m.group(2);
		}

		Set<Person> persons = cg.getPersons(name);
		if (persons == null || persons.isEmpty()) {
			mTvStatus.setText(String.format(getString(R.string.msgNameNotFound), name));
			return;
		} else if (persons.size() > 1) {
			String label = name.replaceAll(Persons.PHON_CONNECTOR, "·");
			launchAmbiguityResolver(String.format(getString(R.string.msgNameAmbiguous), label), persons, actionType);
			mTvStatus.setText("");
			return;
		}
		Person p = persons.iterator().next();
		mTvStatus.setText(p.getDisplayName());

		if (confidence > (float) getConfidenceLevel()) {
			if (isUseTts()) {
				confirmAndExecute(p.getId(), p.getKey(), actionType, actionType.getConfirmationPhrase(p.getDisplayName()));
			} else {
				handleContact(p.getId(), p.getKey(), actionType);
			}
		}
		setSelection(p.getId());
	}


	private boolean isUseTts() {
		return mPrefs.getBoolean(
				getString(R.string.keyUseTts), mRes.getBoolean(R.bool.defaultUseTts));
	}

	private int getConfidenceLevel() {
		String level = mPrefs.getString(
				getString(R.string.keyConfidenceLevel), mRes.getString(R.string.defaultConfidenceLevel));
		return Integer.parseInt(level);
	}


	/**
	 * <p>Determine the position in the list which corresponds to the contact ID.
	 * TODO: we are doing a linear search, which is slow</p>
	 *
	 * @param id contact ID
	 */
	private void setSelection(long id) {
		ListAdapter adapter = mLvContacts.getAdapter();
		for (int position = 0; position < adapter.getCount(); position++) {
			if (adapter.getItemId(position) == id) {
				mLvContacts.setSelection(position);
				//mLvContacts.smoothScrollToPosition(123);
				return;
			}
		}
	}


	private void launchActionViewByKey(long id, String key, ActionType actionType) {
		Uri uri = getDataUri(actionType, id, key);
		Intent intent = new Intent(actionType.getIntentAction(), uri);
		List<ResolveInfo> activities = getIntentActivities(intent);
		if (activities.isEmpty()) {
			String action = ActionType.convertIntentActionToActionType(intent.getAction()).toString();
			mMessageHandler.sendMessage(createMessage(MSG_INFO,
					String.format(getString(R.string.errorActionNotSupported), action, uri)));
		} else {
			Log.i(LOG_TAG, "Launching: " + actionType);
			startActivity(intent);
		}
	}


	private Uri getDataUri(ActionType actionType, long id, String key) {
		if (actionType.isNumberAction()) {
			String number = null;
			Map<String, Integer> numbers = getPhoneNumber(id);
			if (numbers.size() == 1) {
				number = numbers.keySet().iterator().next();
			} else {
				for (Entry<String, Integer> entry : numbers.entrySet()) {
					if (entry.getValue() == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN) {
						number = entry.getKey();
					}
				}
			}
			if (number != null) {
				return Uri.parse("tel:" + number);
			}
		}
		return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key);
	}


	/**
	 * TODO: for some reason this finds only a single number
	 * @param id contact ID
	 * @return map from phone numbers to number types
	 */
	private Map<String, Integer> getPhoneNumber(long id) {
		Map<String, Integer> numbers = new HashMap<String, Integer>();
		Cursor cursor = getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				null,
				ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
				new String[] {"" + id},
				null
				);
		if (cursor.moveToFirst()) {
			String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			int type = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
			numbers.put(number, type);
		}
		cursor.close();
		return numbers;
	}


	private void updateTitle() {
		if (mCursor != null && ! mCursor.isClosed()) {
			int voicesearchableContacts = 0;
			if (mCg != null) {
				voicesearchableContacts = mCg.getContactsSize();
			}
			setTitle(String.format(getString(R.string.tvContacts), mCursor.getCount(), voicesearchableContacts));
		}
	}


	private Uri getContactUri(long id) {
		return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		//return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, "" + id);
	}


	private List<ResolveInfo> getIntentActivities(Intent intent) {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
		return activities;
	}


	private void launchAmbiguityResolver(String title, Set<Person> persons, final ActionType actionType) {
		final List<Person> items = new ArrayList<Person>(persons);
		List<String> names = new ArrayList<String>();
		for (Person p : items) {
			names.add(p.toString());
		}
		final CharSequence[] namesArray = names.toArray(new CharSequence[names.size()]);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setItems(namesArray, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				handleContact(items.get(item).getId(), items.get(item).getKey(), actionType);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private static Message createMessage(int type, String str) {
		Bundle b = new Bundle();
		b.putString(MSG, str);
		Message msg = Message.obtain();
		msg.what = type;
		msg.setData(b);
		return msg;
	}


	private static class SimpleMessageHandler extends Handler {
		private final TextView mTv1;
		private final TextView mTv2;

		SimpleMessageHandler(TextView tv1, TextView tv2) {
			mTv1 = tv1;
			mTv2 = tv2;
		}
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			String msgAsString = b.getString(MSG);
			switch (msg.what) {
			case MSG_STATUS:
				mTv1.setText(msgAsString);
				break;
			case MSG_INFO:
				mTv2.setText(msgAsString);
				break;
			}
		}
	}


	private class LoadRecognizer extends AsyncTask<Void, String, RecognizerTask> {

		protected RecognizerTask doInBackground(Void... params) {
			try {
				updateAcousticModelsIfNeeded();
				publishProgress(getString(R.string.progressUpdateGrammar));
				mCg = new ContactsGrammar(getApplicationContext(), mSelection);
				MyFileUtils.saveFile(new File(mDf.getDict()), mCg.getDict());
				MyFileUtils.saveFile(new File(mDf.getJsgf()), mCg.getJsgf());
				publishProgress(getString(R.string.progressLoadRecognizer));
				return new RecognizerTask(mDf);
			} catch (IOException e) {
				publishProgress(String.format(getString(R.string.error), e.getMessage()));
				return null;
			}
		}

		protected void onProgressUpdate(final String... progress) {
			mTvInfo.setText(progress[0]);
		}

		protected void onPostExecute(RecognizerTask recognizerTask) {
			updateTitle();
			if (recognizerTask != null) {
				mTvInfo.setText("");
				mRt = recognizerTask;

				// Push-and-hold button
				ImageButton b = (ImageButton) findViewById(R.id.buttonMicrophone);
				b.setOnTouchListener(new OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							actionDown();
							break;
						case MotionEvent.ACTION_UP:
							actionUp();
							break;
						default:
							;
						}
						return false;
					}
				});
				b.setVisibility(View.VISIBLE);

				mTvStatus.setText(getString(R.string.tvInstructions));

				Thread t = new Thread(recognizerTask);
				recognizerTask.setRecognitionListener(new RecognitionListener() {

					@Override
					public void onPartialResults(Bundle b) {
						mTvStatus.post(new Runnable() {
							public void run() {
								// It's confusing to show partial results if the input is so short
								//mTvStatus.setText(hyp);
								// so we just show some dots to indicate that something is going on...
								mTvStatus.setText(mTvStatus.getText() + ".");
							}
						});
					}

					@Override
					public void onResults(Bundle b) {
						final String hyp = b.getString("hyp");
						final int bestScore = b.getInt("bestScore");
						mTvInfo.post(new Runnable() {
							public void run() {
								Date end_date = new Date();
								long nmsec = end_date.getTime() - mStartDate.getTime();
								float rec_dur = (float)nmsec / 1000;
								float confidence = getConfidence(bestScore, nmsec);
								mTvInfo.setText(String.format(getString(R.string.tvInfo), mSpeechDur, rec_dur / mSpeechDur, confidence));
								handleContactSelectedBySpeech(mCg, hyp, confidence);
							}
						});
					}

					@Override
					public void onError(final int err) {
						mTvInfo.post(new Runnable() {
							public void run() {
								mTvStatus.setText("");
								mTvInfo.setText(getString(R.string.msgRecognitionError));
							}
						});
					}

					@Override
					public void onEndOfSpeech() {
						mMessageHandler.sendMessage(createMessage(MSG_STATUS,
								getString(R.string.progressRecognizing)));
					}

				});
				t.start();
			}
		}

		/**
		 * <p>TODO: not sure this is the correct way to scale the score,
		 * to make it independent of the length of the recording.</p>
		 */
		private float getConfidence(int score, long duration) {
			return ((float) score) / ((float) duration);
		}

		private void updateAcousticModelsIfNeeded() throws IOException {
			int currentVersion = mRes.getInteger(R.integer.versionAcousticModel);
			int previousVersion = mPrefs.getInt(getString(R.string.prefVersionAcousticModel), 0);
			File hmmDir = new File(mDf.getHmm());
			if (! hmmDir.exists() || previousVersion < currentVersion) {
				publishProgress(getString(R.string.progressUpdateAcousticData));
				MyFileUtils.copyAssets(getApplicationContext(), hmmDir);
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putInt(getString(R.string.prefVersionAcousticModel), currentVersion);
				editor.commit();
				Log.i("Updated acoustic models to version: " + currentVersion);
			}
		}
	}
}