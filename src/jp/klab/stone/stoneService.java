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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class stoneService extends Service {
	private static final String TAG = "stoneSvc";

	private stoneUtil mUtil = null;
	private stonePref mPref = null;
	private stoneLogCache mLogCache = null;
	private Process mProcessStone = null;
	private int mPidStone = -1;
	private int mStatus = 0;
	private String mStonePath = "";
	private final RemoteCallbackList<IStoneCallback> mCallbacks = new RemoteCallbackList<IStoneCallback>();
	private IStoneService.Stub mServiceIf = new MyStub();

	// for foreground service
	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class};
	private static final Class[] mStopForegroundSignature = new Class[] {
		boolean.class};
	private NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private Notification mNotification;
	
	@Override
	public void onCreate() {
		_Log.d(TAG, "svcOnCreate");
		super.onCreate();
		mUtil = new stoneUtil(getApplicationContext());
		mPref = new stonePref(getApplicationContext());
		mLogCache = new stoneLogCache(getApplicationContext());
		mStonePath = mUtil.GetMyResFileDirectory() + "stone";
		boolean sts;
		int pid, check = -1;
		// clean up unattached native stone process
		while ((pid = mUtil.GetProcessIdByName(mStonePath)) != -1) {
			_Log.d(TAG, "svcOnCreate: found stone, pid=" + pid);
			check = pid;
			if (mUtil.SigIntProcess(pid, 10) != true) {
				_Log.w(TAG, "svcOnCreate: SigIntProcess failed, pid=" + pid);
				break;
			}
		}
		if (check != -1) {
			sts = mLogCache.WriteOpen(MODE_PRIVATE | MODE_APPEND);
			String param = mPref.GetLastParam();
			if (param.length() > 0) {
				ExecuteStoneProcess(param);
			}
		} else {
			sts = mLogCache.WriteOpen(MODE_PRIVATE);			
		}
		_Log.d(TAG, "svcOnCreate: openlog sts=" + sts);
		// for compatibility of startForeground()
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// older platform
			mStartForeground = mStopForeground = null;
		}
	}

	@Override
	public void onStart(Intent it, int id) {
		_Log.d(TAG, "svcOnStart");
		super.onStart(it, id);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		_Log.d(TAG, "svcOnStartCommand");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		_Log.d(TAG, "svcOnDestroy");
		super.onDestroy();
		if (mProcessStone != null) {
			TerminateStoneProcess();
		}
		mLogCache.Close();
		mLogCache.Delete();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		_Log.d(TAG, "svcOnBind");
		return mServiceIf;
	}

	// interface for activity
	private class MyStub extends IStoneService.Stub {
		public int __StoneProcessIsRunning() throws RemoteException {
			if (mPidStone != -1) {
				_Log.d(TAG, "__StoneProcessIsRunning: pid=" + mPidStone);
				return 1;
			}
			return 0;
		}
		public int __ExecuteStoneProcess(String param) throws RemoteException {
			_Log.d(TAG, "__ExecuteStoneProcess: start");
			ExecuteStoneProcess(param);
			return 0;
		}
		public int __TerminateStoneProcess() throws RemoteException {
			_Log.d(TAG, "__TerminateStoneProcess: start");
			TerminateStoneProcess();
			return 0;
		}
		public void __ClearStoneLogCache() throws RemoteException {
			_Log.d(TAG, "__ClearStoneLogCache: start");
			mLogCache.Close();
			mLogCache.WriteOpen(MODE_PRIVATE);
			return;
		}
		// register callback
		public void __registerCallback(IStoneCallback cb) {
			if (cb != null) {
				mCallbacks.register(cb);
			}
		}
		public void __unregisterCallback(IStoneCallback cb) {
			if (cb != null) {
				mCallbacks.unregister(cb);
			}
		}
	}

	// put stone log text to application GUI
	private void PutStoneLog(String log) {
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mCallbacks.getBroadcastItem(i).__PutStoneLog(log);
			} catch (RemoteException e) {
				_Log.e(TAG, "PutStoneLog: failed: " + e);
			}
		}
		mCallbacks.finishBroadcast();
	}

	// stone process exit notification
	private void NotifyStoneProcessDone(int sts) {
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mCallbacks.getBroadcastItem(i).__NotifyStoneProcessDone(sts);
			} catch (RemoteException e) {
				_Log.e(TAG, "NotifyStoneProcessDone: failed: " + e);
			}
		}
		mCallbacks.finishBroadcast();
	}

	// Convert stone parameter string to string array.
	// Because the parameter might have single or double quoted
	//  argument which contains white space.
	// example: -dd server:443/ssl,http 1443 'GET / HTTP/1.1'
	//	-> [-dd] [server:443/ssl,http] [1443] [GET / HTTP/1.1]
	private String[] CreateStoneParameterArray(String stonePath, String param) {
		final String SQ ="'"; // single quotation
		final String DQ ="\"";  // double quotation
		String p = stonePath + " " + param;
		p = p.replaceAll("[\\r\\n]", " "); // CR,LF to space
		String cmdline[] = null;
		String wk []= p.split(" +"); // split by space(s)

		boolean inQuote = false;
		boolean isSQ = false;
		int sq1, dq1, start = 0;
		int num = wk.length;
		// If a element contains SQ or DQ character, 
		//  the element text should be reconstructed.
		for (int i = 0; i < wk.length; i++) {
			//_Log.d(TAG, "before: " + i + "[" + wk[i] + "]");
			sq1 = wk[i].indexOf(SQ);
			dq1 = wk[i].indexOf(DQ);
			if (inQuote) { // inside quotation marks
				wk[start] += " " + wk[i];
				wk[i] = "";
				num--;
				if ((isSQ && sq1 != -1) || (!isSQ && dq1 != -1)) {
					wk[start] = wk[start].replaceAll(isSQ ? SQ : DQ, ""); // 2013.05
					inQuote = false;
				}
			} else { // outside quotation marks
				if (sq1 == -1 && dq1 == -1) {
					continue;
				}
				if (sq1 != -1) {
					if (dq1 == -1 || sq1 < dq1) {
						isSQ = true;
					} else {
						isSQ = false;
					}
				} else {
					isSQ = false;
				}
				// If a element contains multiple SQ or multiple DQ,
				//   the quotation marks might have been closed.
				if ((isSQ && wk[i].indexOf(SQ, sq1+1) != -1) ||
					(!isSQ && wk[i].indexOf(DQ, dq1+1) != -1)) {
					wk[i] = wk[i].replaceAll(isSQ ? SQ : DQ, ""); // 2013.05
					continue;
				}
				inQuote = true;
				start = i;
			}
		}
		cmdline = new String[num];
		int idx = 0;
		for (int i = 0; i < wk.length; i++) {
			if (wk[i].length() > 0) {
				cmdline[idx] = wk[i];
				//_Log.d(TAG, i + "[" + cmdline[idx] + "]");
				idx++;
			}
		}
		return cmdline;
	}

	private void doStone(String param) {
		try {
			_Log.d(TAG, "doStone: param=[" + param + "]");
			String p[] = CreateStoneParameterArray(mStonePath, param);
			mPref.SetLastParam(param);
			mPidStone = -1;
			mProcessStone = Runtime.getRuntime().exec(p);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(mProcessStone.getErrorStream()));
			int len;
			char[] buffer = new char[4096];
			mPidStone = mUtil.GetProcessIdByName(mStonePath);
			EnterForeground(R.string.app_name);
			while ((len = reader.read(buffer)) > 0) {
				String out = new String(buffer, 0, len);
				mLogCache.Append(out);
				PutStoneLog(out);
			}
			reader.close();
			mPidStone = -1;
			mProcessStone.waitFor();
			stopForegroundCompat(R.string.app_name);
			return;
		} catch (Exception e) {
			mProcessStone = null;
			_Log.e(TAG, "doStone: failed: " + e);
		}
	}

	private int ExecuteStoneProcess(final String param) {
		mStatus = 0;
		try {
			Runnable looper = new Runnable() {
				public void run() {
					doStone(param);
					NotifyStoneProcessDone(mStatus);
					_Log.d(TAG, "ExecuteStoneProcess: done");
				}
			};
			Thread threadStone;
			threadStone = new Thread(looper);
			threadStone.start();
		} catch (Exception e) {
			_Log.e(TAG, "ExecuteStoneProcess: failed: " + e);
		}
		return 0;
	}

	private int TerminateStoneProcess() {
		if (mPidStone != -1) {
			mStatus = 1;
			mUtil.SigIntProcess(mPidStone, 10);
		}
	 	return 0;
	}

	// switch to foreground
	private void EnterForeground(int id) {
		CharSequence text = getText(R.string.MsgStoneRunning);
		mNotification = new Notification(R.drawable.mini, 
					getText(R.string.WordBgStone), System.currentTimeMillis());
		Intent it = new Intent(this, stone.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, it, 
					Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// Set the info for the views that show in the notification panel.
		mNotification.setLatestEventInfo(this, getText(R.string.app_name),
								text, contentIntent);
		startForegroundCompat(id, mNotification);
	}

	private void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				_Log.w(TAG, "startForegroundCompat: Unable to invoke startForeground", e);
			} catch (IllegalAccessException e) {
				_Log.w(TAG, "startForegroundCompat: Unable to invoke startForeground", e);
			}
			return;
		}
		// Fall back on the old API.
		setForeground(true);
		mNM.notify(id, notification);
	}
	
	private void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				_Log.w(TAG, "stopForegroundCompat: Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e) {
				_Log.w(TAG, "stopForegroundCompat: Unable to invoke stopForeground", e);
			}
			return;
		}
		// Fall back on the old API.  Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		setForeground(false);
	}
}
