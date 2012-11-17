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

import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * <p>Provides a JSGF grammar and a dictionary generated from the
 * device address book.</p>
 *
 * @author Kaarel Kaljurand
 */
public class ContactsGrammar {

	private final Persons mPersons;

	private final String mJsgf;

	public ContactsGrammar(Context context, String selection) {
		mPersons = new Persons(context);
		Cursor c = getContacts(context, selection);
		while (c.moveToNext()) {
			long id = c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID));
			String key = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
			String displayName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			if (displayName != null) {
				mPersons.add(id, key, displayName);
			}
		}
		mJsgf = namesToJsgf(context, mPersons.getNames());
	}

	public Set<Person> getPersons(String token) {
		return mPersons.get(token);
	}

	public String getJsgf() {
		return mJsgf;
	}

	public String getDict() {
		return mPersons.getDict().toString();
	}

	public int getContactsSize() {
		return mPersons.size();
	}


	/**
	 * TODO: currently the tags are in the grammar but they are not used.
	 * Is there a Sphinx commandline para to replace the recognized words by tags.
	 * TODO: use weights
	 * TODO: take the command words from the resources
	 *
	 * @param context
	 * @param names
	 * @return
	 */
	private String namesToJsgf(Context context, String names) {
		//String[] actions = context.getResources().getStringArray(R.array.wordsActionContact);
		//String[] phones = context.getResources().getStringArray(R.array.wordsPhoneType);
		StringBuilder sb = new StringBuilder();
		sb.append("#JSGF V1.0;\ngrammar contacts;\n");
		//sb.append("public <command> = /1/ <command_view> | /1.5/ <command_call>;\n");
		sb.append("public <command> = <command_view> | <command_call>;\n");
		sb.append("<command_view> = <name>;\n");
		sb.append("<command_call> = (helista | vali) palun <name>;\n");
		//sb.append("<command_call> = (helista {CALL} | vali {DIAL}) palun <name>;\n");
		//sb.append("<phone> = " + TextUtils.join(" | ", phones) + ";\n");
		sb.append("<name> = " + names + ";\n");
		return sb.toString();
	}


	/**
	 * Obtains the contact list for the currently selected account.
	 */
	private Cursor getContacts(Context context, String selection) {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.LOOKUP_KEY,
				ContactsContract.Contacts.DISPLAY_NAME
		};
		String[] selectionArgs = null;
		String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
		ContentResolver cr = context.getContentResolver();
		return cr.query(uri, projection, selection, selectionArgs, sortOrder);
	}
}