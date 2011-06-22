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

import android.util.Log;

public final class _Log {
	private static final boolean DEVELOP = false;
	private _Log(){
	}
	public static int e(String tag, String msg) {
		return Log.e(tag, msg);
	}
	public static int e(String tag, String msg, Throwable tr) {
		return Log.e(tag, msg, tr);
	}
	public static int w(String tag, String msg) {
		return Log.w(tag, msg);
	}
	public static int w(String tag, String msg, Throwable tr) {
		return Log.w(tag, msg, tr);
	}
	public static int v(String tag, String msg) {
		return DEVELOP ? Log.v(tag, msg) : 0;
	}
	public static int v(String tag, String msg, Throwable tr) {
		return DEVELOP ? Log.v(tag, msg, tr) : 0;
	}
	public static int d(String tag, String msg) {
		return DEVELOP ? Log.d(tag, msg) : 0;
	}
	public static int d(String tag, String msg, Throwable tr) {
		return DEVELOP ? Log.d(tag, msg, tr) : 0;
	}
	public static int i(String tag, String msg) {
		return DEVELOP ? Log.i(tag, msg) : 0;
	}
	public static int i(String tag, String msg, Throwable tr) {
		return DEVELOP ? Log.i(tag, msg, tr) : 0;
	}
}
