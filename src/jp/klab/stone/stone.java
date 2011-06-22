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

import jp.klab.stone.R.id;
import jp.klab.stone.certinstaller.CertFileList;
import jp.klab.stone.certinstaller.KeyStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
//import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class stone extends Activity  implements OnClickListener, Handler.Callback {
	private static final String TAG = "stone";
	private static final int[] mFontSizeTbl = {8, 12, 16};
	private static final int MAX_READFILESIZE = 8192;
	private static final int MSG_PUT_LOG     = 0;
	private static final int MSG_ENABLE_VIEW = 1;
	private static final int MSG_SHOW_MESSAGE = 2;

	private Handler mHandler = null;
	private Intent mItStoneService = null;
	private IStoneService mServiceIf = null;
	private ServiceConnection mServiceConn = null;
	private IStoneCallback mCallback = null;
	private stoneUtil mUtil = null; 
	private stonePref mPref = null;
	
	private Button mButtonRun, mButtonStop, mButtonClear, mButtonHistory;
	private EditText mEditStoneParam;
	private TextView mTextStoneLog;
	private ScrollView mScrollStoneLog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		_Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		//Window w = getWindow();
		//w.requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.main);
		//w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.mini);
		setTitle(R.string.TitleMain);

		mHandler = new Handler(this);
		mCallback = new MyStub();
		mUtil = new stoneUtil(getApplicationContext());
		mPref = new stonePref(getApplicationContext());
		
		mEditStoneParam = (EditText)findViewById(id.EditText01);
		mButtonRun = (Button)findViewById(id.ButtonRun);
		mButtonStop = (Button)findViewById(id.ButtonStop);
		mButtonClear = (Button)findViewById(id.ButtonClear);
		mButtonHistory = (Button)findViewById(id.ButtonHistory);
		mScrollStoneLog = (ScrollView)findViewById(id.ScrollView02);
		mTextStoneLog = (TextView)findViewById(id.stonelog);

		int FontSizeLevel = mPref.GetFontSizeSetting();
		mEditStoneParam.setTextSize(mFontSizeTbl[FontSizeLevel]);
		mTextStoneLog.setTextSize(mFontSizeTbl[FontSizeLevel]);

		mEditStoneParam.setText(R.string.DefaultParam);
		mTextStoneLog.setText("");

		mButtonRun.setOnClickListener(this);
		mButtonStop.setOnClickListener(this);
		mButtonClear.setOnClickListener(this);
		mButtonHistory.setOnClickListener(this);

		String MyDir = mUtil.GetMyResFileDirectory();
		mUtil.ExtractMyResFile(R.raw.stone, "stone", MyDir, "744");
		mUtil.ExtractMyResFile(R.raw.about, "about.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.readme, "readme.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.about_en, "about_en.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.readme_en, "readme_en.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.notice, "notice.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.notice1, "notice1.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.notice2, "notice2.html", MyDir, "644");
		mUtil.ExtractMyResFile(R.raw.notice3, "notice3.html", MyDir, "644");
		
		mItStoneService = new Intent(this, stoneService.class);
		startService(mItStoneService);
		mServiceConn = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				mServiceIf = IStoneService.Stub.asInterface(service);
				try {
					mServiceIf.__registerCallback(mCallback);
					if (StoneProcessIsRunning()) {
						EnableView(mButtonRun, false);
						EnableView(mButtonStop, true);
					} else {
						EnableView(mButtonRun, true);
						EnableView(mButtonStop, false);
					}
				} catch (RemoteException e) {
					_Log.e(TAG, "onCreate: registerCallback failed: " + e);
				}
			}
			public void onServiceDisconnected(ComponentName name) {
				mServiceIf = null;
			}
		};
		bindService(mItStoneService, mServiceConn, BIND_AUTO_CREATE);
	}

	@Override
    protected void onStart() {
		super.onStart();
		_Log.d(TAG, "onStart");
		LoadStoneParameter();
		String LogCache = mUtil.LoadText2(getString(R.string.FileNameLogCache),
											"UTF-8", MAX_READFILESIZE);
		if (LogCache.length() > 0) {
			mTextStoneLog.setText(LogCache);
			mScrollStoneLog.post(new Runnable() { // scroll to bottom
				public void run() {
					mScrollStoneLog.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		} else {
			String param = mEditStoneParam.getText().toString();
			if (param.equals(getString(R.string.DefaultParam))) {
				mTextStoneLog.setText("");
				// show usage 
				PutText(getString(R.string.Usage1));
				PutText(getString(R.string.Usage1A));
				PutText(getString(R.string.Usage1B));
				PutText(getString(R.string.Usage1C));
				PutText(getString(R.string.Usage1D));
				PutText(getString(R.string.Usage1E));
				PutText(getString(R.string.Usage2));
				PutText(getString(R.string.Usage2A));
			}
		}
	}

	@Override
    protected void onResume() {
	    _Log.d(TAG, "onResume" );
		super.onResume();
	}

	@Override
    protected void onPause() {
	    _Log.d(TAG, "onPause" );
		super.onPause();
	}

	@Override
    protected void onRestart() {
	    _Log.d(TAG, "onRestart" );
		super.onRestart();
	}

	@Override
    protected void onStop() {
	    _Log.d(TAG, "onStop" );
		super.onStop();
		SaveStoneParameter();
	}

	@Override
    protected void onDestroy() {
		_Log.d(TAG, "onDestroy" );
	    super.onDestroy();
		boolean StoneIsRunning = StoneProcessIsRunning();
		_Log.d(TAG, "onDestroy: StoneIsRunning=" + StoneIsRunning);
		unbindService(mServiceConn);
		if (!StoneIsRunning) {
			stopService(mItStoneService);
		}
	}

	// callbacks for service
	private class MyStub extends IStoneCallback.Stub {
		public void __PutStoneLog(String log) {
			PutText(log);
		}
		public void __NotifyStoneProcessDone(int sts) {
			_Log.d(TAG, "NotifyStoneProcessDone: sts=" + sts);
			EnableView(mButtonRun, true);
			EnableView(mButtonStop, false);
			if (sts == 0) { // stone exec error
				ShowDlgMessage(getString(R.string.MsgExecError));
			}
		}
	}

	// message handler - helps to use GUI from each threads
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_PUT_LOG:
			int len = mTextStoneLog.length();
			if (len > MAX_READFILESIZE) { // became too long
				mTextStoneLog.setText("");
			} else {
				mTextStoneLog.append((String)msg.obj);
				mScrollStoneLog.post(new Runnable() {
					public void run() { // scroll to bottom
						mScrollStoneLog.fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
			break;
		case MSG_ENABLE_VIEW:
			View v = (View)msg.obj;
			boolean bool = (msg.arg1 == 0) ? false  : true;
			v.setClickable(bool);
			v.setEnabled(bool);
			break;
		case MSG_SHOW_MESSAGE:
			AlertDialog.Builder dlg = new AlertDialog.Builder(this);
			dlg.setTitle(R.string.TitleMain);
			dlg.setIcon(R.drawable.icon);
			dlg.setMessage((String)msg.obj);
			dlg.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}
			);
			try {
				dlg.show();
			} catch (Exception e) {
				_Log.e(TAG, "handleMessage: MSG_SHOW_MESSAGE failed: " + e);
			}
			break;
		}
		return true;
	}

	// onClickListenr
	public void onClick(View v) {
		if (v == (View)mButtonRun) {
			onClickRun();
		} else if (v == (View)mButtonStop) {
			onClickStop();
		} else if (v == (View)mButtonClear) {
			onClickClear();
		} else if (v == (View)mButtonHistory) {
			onClickHistory();
		}
		return;
	}

	// ask whether stone process is avtive
	private boolean StoneProcessIsRunning() {
		boolean ret = false;
		try {
			if (mServiceIf != null&&
				mServiceIf.__StoneProcessIsRunning() != 0) {
				ret = true;
			}
		} catch (RemoteException e) {
			_Log.e(TAG, "StoneProcessIsRunning: failed: " + e);
		}
		return ret;
	}

	// [Run] button listener
	private void onClickRun() {
		try {
			if (mServiceIf.__StoneProcessIsRunning() == 0) {
				EnableView(mButtonRun, false);
				EnableView(mButtonStop, true);
				String text = mEditStoneParam.getText().toString();
				mServiceIf.__ExecuteStoneProcess(text);
			}
		} catch (RemoteException e) {
			_Log.e(TAG, "onClickRun: failed: " + e);
		}
		return;
	}

	// [stop] button listener
	private void onClickStop() {
		AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle(R.string.TitleMain);
		dlg.setIcon(R.drawable.icon);
		dlg.setMessage(R.string.MsgStopStone);
		dlg.setPositiveButton(R.string.WordYes,
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					try {
						mServiceIf.__TerminateStoneProcess();
						EnableView(mButtonRun, true);
						EnableView(mButtonStop, false);
						SaveStoneParameter();
					} catch (RemoteException e) {
						_Log.e(TAG, "onClickStop: failed: " + e);
					}
				}
			}
		);
    	dlg.setNegativeButton(R.string.WordNo,
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
				}
			}
		);
    	dlg.show();
		return;
	}

	// [Clear] button listener
	private void onClickClear() {
	AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle(R.string.TitleMain);
		dlg.setIcon(R.drawable.icon);
		dlg.setMessage(R.string.MsgClearLog);
		dlg.setPositiveButton(R.string.WordYes,
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					mTextStoneLog.setText("");
					try {
						mServiceIf.__ClearStoneLogCache();
					} catch (Exception e) {
						_Log.e(TAG, "onClickClear: failed: " + e);
					}
				}
			}
		);
		dlg.setNegativeButton(R.string.WordNo,
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
				}
			}
		);
		dlg.show();
		return;
	}

	// [History] button listener
	private void onClickHistory() {
		SaveStoneParameter();
		Intent it = new Intent(this, stoneParamHistory.class);
		startActivity(it);
		return;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		_Log.d(TAG, "onActivityResult: req=" + requestCode + " result=" + resultCode);
		if (requestCode == 0) {
			KeyStore mKeyStore = KeyStore.getInstance();
			int state = mKeyStore.test();
			_Log.d(TAG, "onActivityResult: keystore state=" + state);
			if (state == KeyStore.NO_ERROR) { // Credential storage has been unlocked
				// start CertFileList
				startActivity(new Intent(stone.this, CertFileList.class));
			}
		}
	}

	// create menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, Menu.NONE, R.string.WordFont).
			setIcon(android.R.drawable.ic_menu_edit);
		int nVerSdk = Integer.parseInt(Build.VERSION.SDK);
		if (nVerSdk > 4) { // for keystore I/F compatibility
			menu.add(0, 1, Menu.NONE, R.string.WordCert).
			setIcon(android.R.drawable.ic_menu_myplaces);
		}
		menu.add(0, 2, Menu.NONE, R.string.WordManual).
			setIcon(android.R.drawable.ic_menu_help);

		menu.add(0, 3, Menu.NONE, R.string.WordAbout).
			setIcon(android.R.drawable.ic_menu_info_details);

		menu.add(0, 4, Menu.NONE, R.string.WordExit).
			setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		return true;
	}

	// select menu item
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent it;
		//_Log.d(TAG, "onOptionsItemSelected: item=" +
		//			item.getItemId() + " " + item.getTitle());
		switch(item.getItemId()) {
		case 0: // font size
			final CharSequence[] labels = { getText(R.string.WordSmall),
											getText(R.string.WordMedium),
											getText(R.string.WordLarge) };
			int val = mPref.GetFontSizeSetting();
			AlertDialog.Builder dlg = new AlertDialog.Builder(stone.this);
			dlg.setTitle(R.string.MsgFontSize);
			dlg.setIcon(R.drawable.icon);
			dlg.setSingleChoiceItems(labels, val,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int val) {
						mPref.SetFontSizeSetting(val);
						mEditStoneParam.setTextSize(mFontSizeTbl[val]);
						mTextStoneLog.setTextSize(mFontSizeTbl[val]);
					}
				}
			);
			 dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			     public void onClick(DialogInterface dialog, int id) {
			         dialog.cancel();
			    }
			})
			.show();
			return true;

		case 1: // install certificate
			// get status of Credential Storage
			KeyStore mKeyStore = KeyStore.getInstance();
			int state = mKeyStore.test();
			// locked
			if (state == KeyStore.LOCKED) {
				// for Android 3.x
				it = new Intent("com.android.credentials.UNLOCK");
				try {
					startActivityForResult(it, 0);
				} catch (Exception e) { // 2.x below
					_Log.d(TAG, "credentials.UNLOCK: older");
					it = new Intent("android.credentials.UNLOCK");
					startActivityForResult(it, 0);
				}
			}
			// not initialized
			else if (state == KeyStore.UNINITIALIZED) {
				ShowDlgMessage(getString(R.string.MsgCredentialStorageNotInitialized));
			}
			// good!
			else if (state == KeyStore.NO_ERROR) {
				startActivity(new Intent(this, CertFileList.class));
			}
			return true;

		case 2: // Help
			it = new Intent(this, stoneHelp.class);
			it.putExtra("fname",
				mUtil.GetMyResFileDirectory() + getString(R.string.FileNameReadme));
			it.putExtra("title", getString(R.string.TitleHelp));
			startActivity(it);
			return	true;

		case 3: // Notice
			it = new Intent(this, stoneHelp.class);
			it.putExtra("fname",
				mUtil.GetMyResFileDirectory() + getString(R.string.FileNameAbout));
			it.putExtra("title", getString(R.string.TitleNotice));
			startActivity(it);
			return true;

		case 4: // exit
			onKeyDown(KeyEvent.KEYCODE_BACK, null);
			return	true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			AlertDialog.Builder dlg = new AlertDialog.Builder(this);
			dlg.setTitle(R.string.TitleMain);
			dlg.setIcon(R.drawable.icon);
			if (StoneProcessIsRunning()) {
				dlg.setMessage(R.string.MsgExit1);
			} else {
				dlg.setMessage(R.string.MsgExit2);
			}
			dlg.setPositiveButton(R.string.WordYes,
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}
			);
			dlg.setNegativeButton(R.string.WordNo,
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
					}
				}
			);
			dlg.show();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	private int SaveStoneParameter() {
		String param = mEditStoneParam.getText().toString();
		mPref.SetParamHistory(param);
		return 0;
	}

	private int LoadStoneParameter () {
		String [] history = mPref.GetParamHistory();
		if (history[0].length() > 0) {
			mEditStoneParam.setText(history[0]);
		}
		return 0;
	}

	private void PutText(String str) {
		Message lmsg;
		lmsg = new Message();
		lmsg.obj = str;
		lmsg.what = MSG_PUT_LOG;
		mHandler.sendMessage(lmsg);
	}

	private void EnableView(View v, boolean b) {
		Message lmsg;
		lmsg = new Message();
		lmsg.obj = v;
		lmsg.arg1 = (b) ? 1 : 0;
		lmsg.what = MSG_ENABLE_VIEW;
		mHandler.sendMessage(lmsg);
	}

	private void ShowDlgMessage(String str) {
		Message lmsg;
		lmsg = new Message();
		lmsg.obj = str;
		lmsg.what = MSG_SHOW_MESSAGE;
		mHandler.sendMessage(lmsg);
	}
}
