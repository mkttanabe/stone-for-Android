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

import jp.klab.stone.R.id;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
//import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class stoneHelp extends Activity {
	private static final String TAG = "stone";
	WebView webview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		_Log.d(TAG, "HelpOnCreate" );
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		if (bundle == null) {
			finish();
			return;
		}
		String fname = bundle.getString("fname");
		File file = new File(fname);
		if (!file.exists()) {
			finish();
			return;
		}
		String title = bundle.getString("title");
		//Window w = getWindow();
		//w.requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.webview);
		//w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.mini);
		setTitle(title);
		webview = (WebView)findViewById(id.webkitWebView1);
		webview.setWebViewClient(new MyWebViewClient(this));
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setMinimumFontSize(12);
		webview.getSettings().setSupportZoom(true);
		webview.getSettings().setJavaScriptEnabled(true);
		String url = "file://" + fname; 
		webview.loadUrl(url);
	}

	@Override
	protected void onStart() {
		_Log.d(TAG, "HelpOnStart" );
		super.onStart();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
			webview.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}

class MyWebViewClient extends WebViewClient {
	private ProgressDialog mProgressDlg;
	Context mContext;

	public MyWebViewClient(Context context) {
		super();
		mContext = context;
		mProgressDlg = null;
	}

	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		super.onPageStarted(view, url, favicon);
		if (url.substring(0,7).equalsIgnoreCase("file://")) {
			// skip progressDialog if url is local-file.
			return;
		}
		mProgressDlg = new ProgressDialog(mContext);
		mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDlg.setMessage(mContext.getString(R.string.MsgWait));
		mProgressDlg.setCancelable(true);
		mProgressDlg.show();
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		super.onPageFinished(view, url);
		if (mProgressDlg != null) {
			mProgressDlg.dismiss();
			mProgressDlg = null;
		}
	}

	@Override
	public void onReceivedError(WebView view, int errorCode,
								String description, String failingUrl) {
		super.onReceivedError(view, errorCode, description, failingUrl);
		if (mProgressDlg != null) {
			mProgressDlg.dismiss();
			mProgressDlg = null;
		}
	}
}

