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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
//import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class stoneParamHistory extends Activity implements OnItemLongClickListener, OnItemClickListener  {
	private static final String TAG = "stone";
	private TextView mMsgText;
	String[] mCmdHistory = null;
	ListView mList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		_Log.d(TAG, "HistoryOnCreate" );
		super.onCreate(savedInstanceState);
		//Window w = getWindow();
		//w.requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.history);
		//w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.mini);
		setTitle(R.string.TitleHistory);
	}

	@Override
    protected void onStart() {
		_Log.d(TAG, "HistoryOnStart" );
		super.onStart();
		stonePref pref = new stonePref(getApplicationContext());
		setContentView(R.layout.history);
		mMsgText = (TextView)findViewById(R.id.MsgText);
		mMsgText.setText(R.string.MsgHistoryUsage);
		mList = (ListView)findViewById(id.HistoryList);
		
		mCmdHistory = pref.GetParamHistory();
		mList.setOnItemClickListener(this);
		mList.setOnItemLongClickListener(this);
		SetAdapter(mCmdHistory);
	}

	private void SetAdapter(String [] history) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.history_adapt, history);
		mList.setAdapter(adapter);
	}

	// Tap
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//_Log.d(TAG, "pos=" + position + " data=[" + mCmdHistory[position]);
		if (mCmdHistory[position].length() <= 0) {
			return;
		}
		stonePref pref = new stonePref(getApplicationContext());
		pref.SetParamHistory(mCmdHistory[position]);
		finish();
	}

	// Press-and-hold
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		//_Log.d(TAG, "pos=" + position + " data=[" + mCmdHistory[position]);
		if (mCmdHistory[position].length() <= 0) {
			return false;
		}
		final stonePref pref = new stonePref(getApplicationContext());
		AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle(R.string.TitleMain);
		dlg.setIcon(R.drawable.icon);
		// "Remove [%s] from list？"
		dlg.setMessage(String.format(getString(R.string.MsgQueryHistoryRemove),
							mCmdHistory[position]));
		dlg.setPositiveButton(getString(R.string.WordYes),
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					mCmdHistory = pref.RemoveParamHistory(position);
					SetAdapter(mCmdHistory); // refresh
				}
			}
		);
		dlg.setNegativeButton(getString(R.string.WordNo),
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
				}
			}
		);
		dlg.show();
		return true;
	}
}
