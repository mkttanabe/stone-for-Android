/*
 * Copyright (C) 2011 KLab Inc.
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

package jp.klab.stone;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class stonePref {
	private static final String TAG = "stone";
	private static final String PREFKEY_HISTORY = "History";
	private static final String PREFKEY_FONTSIZE = "FontSize";
	private static final String PREFKEY_LASTPARAM = "LastParam";
	public static final int HISTORY_MAX = 20;
	private SharedPreferences mPref = null;
	
	public stonePref (Context context) {
		mPref = context.getSharedPreferences(TAG, Activity.MODE_PRIVATE);		
	}

	// get font size value
	public int GetFontSizeSetting() {
		int FontSizeVal = mPref.getInt(PREFKEY_FONTSIZE, 1); // 1=default
		return FontSizeVal;
	}

	// save font size value
	public int SetFontSizeSetting(int level) {
		mPref.edit().putInt(PREFKEY_FONTSIZE, level).commit();
		return 0;
	}

	// get last parameter string
	public String GetLastParam() {
		return mPref.getString(PREFKEY_LASTPARAM, "");
	}

	// save string as a last parameter
	public void SetLastParam(String cmd) {
		mPref.edit().putString(PREFKEY_LASTPARAM, cmd).commit();
	}
	
	// get stone parameter history
	public String[] GetParamHistory() {
		String [] array = new String [HISTORY_MAX];
		for (int i = 0; i < HISTORY_MAX; i++) {
			array[i] = mPref.getString(PREFKEY_HISTORY + String.format("%02d", i), "");
		}
		return array;
	}

	// insert specified parameter string to top of history stack
	public String[] SetParamHistory(String cmd) {
		String param = cmd.replaceAll("[ \\r\\n]*$", ""); // trim tail
		String [] history = GetParamHistory();

		if (param.length() <= 0 || history[0].equals(param)) {
			return history;
		}
		for (int i = 0; i < HISTORY_MAX; i++) {
			mPref.edit().putString(PREFKEY_HISTORY + String.format("%02d", i++), "").commit();
		}
		// save as 0th data
		mPref.edit().putString(PREFKEY_HISTORY + "00", param).commit();
		// relocate other data
		for (int i = 0, j = 1; i < HISTORY_MAX && j < HISTORY_MAX; i++) {
			String str = history[i].replaceAll("[\\r\\n]*$", "");
			if (str.length() > 0 && str.equals(param) != true) {
				mPref.edit().putString(PREFKEY_HISTORY + String.format("%02d", j++), str).commit();
			}
		}
		return GetParamHistory(); // returns relocated array
	}
	
	// delete a parameter history data
	public String[] RemoveParamHistory(int idx) {
		String [] history = GetParamHistory();
		history[idx] = "";
		// clear all
		for (int i = 0; i < HISTORY_MAX; i++) {
			mPref.edit().putString(PREFKEY_HISTORY + String.format("%02d", i), "").commit();
		}
		// relocate
		for (int i = 0, j = 0; i < HISTORY_MAX; i++) {
			String str = history[i];
			if (str.length() > 0) {
				mPref.edit().putString(PREFKEY_HISTORY + String.format("%02d", j++), str).commit();
			}
		}
		return GetParamHistory(); // returns relocated array
	}
}
