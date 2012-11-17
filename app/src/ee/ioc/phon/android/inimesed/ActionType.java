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

import android.content.Intent;

public enum ActionType {

	// TODO: get strings from resources
	VIEW(Intent.ACTION_VIEW, ""),
	DIAL(Intent.ACTION_DIAL, "valin"),
	CALL(Intent.ACTION_CALL, "helistan");

	private final String mIntentAction;
	private final String mConfirmationPhrase;

	ActionType(String intentAction, String confirmationPhrase) {
		mIntentAction = intentAction;
		mConfirmationPhrase = confirmationPhrase;
	}

	public String getIntentAction() {
		return mIntentAction;
	}

	public String getConfirmationPhrase(String name) {
		return mConfirmationPhrase + " " + name;
	}

	// TODO: get strings from resources
	public static ActionType convertTagToActionType(String tag) {
		if ("helista".equals(tag)) {
			return CALL;
		}
		if ("vali".equals(tag)) {
			return DIAL;
		}
		return VIEW;
	}

	public static ActionType convertIntentActionToActionType(String intentAction) {
		if (CALL.getIntentAction().equals(intentAction)) {
			return CALL;
		}
		if (DIAL.getIntentAction().equals(intentAction)) {
			return DIAL;
		}
		return VIEW;
	}

	public boolean isNumberAction() {
		return this.equals(CALL) || this.equals(DIAL);
	}

}