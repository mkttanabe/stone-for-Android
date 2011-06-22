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

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.content.Context;

public class stoneLogCache {
	private static final String TAG = "stoneSvc";
	Context mContext = null;
	private OutputStream mLogOutStream = null;
	private PrintWriter mLogWriter = null;
	private String mFileName = "";
	
	stoneLogCache(Context context) {
		mContext = context;
		mFileName = mContext.getString(R.string.FileNameLogCache);
	}

	public boolean WriteOpen(int mode) {
		try{
			mLogOutStream = mContext.openFileOutput(mFileName, mode);
			mLogWriter = new PrintWriter(new OutputStreamWriter(mLogOutStream, "UTF-8"));
			return true;
		} catch(Exception e){
			_Log.e(TAG, "WriteOpen: failed: " + e);
		}
		return false;
	}

	public boolean Append(String s) {
		if (mLogWriter == null) {
			return false;
		}
		try {
			mLogWriter.append(s);
			mLogWriter.flush();
		} catch(Exception e){
			_Log.e(TAG, "Append: failed: " + e);
			return false;
		}
		return true;
	}

	public boolean Close() {
		if (mLogWriter == null) {
			return false;
		}
		try {
			mLogWriter.close();
			mLogOutStream.close();
		} catch (Exception e) {
			_Log.e(TAG, "Close: failed: " + e);
			return false;
		}
		mLogWriter = null;
		mLogOutStream = null;
		return true;
	}

	public void Delete() {
		Close();
		//mContext.deleteFile(mFileName);
		File f = mContext.getFileStreamPath(mFileName);
		f.delete();
	}
}
