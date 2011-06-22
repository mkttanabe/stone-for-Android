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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.content.Context;

public class stoneUtil {
	private static final String TAG = "stone";
	private static final String CMD_PS = "/system/bin/ps";
	private static final String CMD_CHMOD = "/system/bin/chmod";

	Context mContext = null;

	stoneUtil(Context context) {
		mContext = context;
	}

	// returns output path of RAW resource files 
	public String GetMyResFileDirectory() {
		return  mContext.getFilesDir().getAbsolutePath() + File.separator ;
	}

	// extract resource file 
	public int ExtractMyResFile(int resid, String fname, String outputDir, String priv) {
		String strOutFilePath = outputDir + fname;
	    try{
		    OutputStream os = new FileOutputStream(strOutFilePath);
		    byte[] buf = new byte[1024];
		    InputStream is = mContext.getResources().openRawResource(resid);
		    int len = 0;
		    while ((len = is.read(buf)) != -1) {
			    os.write(buf, 0, len);
			}
		    is.close();
		    os.close();
		    Process process = Runtime.getRuntime().exec(CMD_CHMOD + " " + priv + " " + strOutFilePath);
			process.waitFor();
		}
	    catch (Exception e) {
			_Log.i(TAG, "ExtractMyResFile: failed: " + e);
	    	return -1;
		}
		return 0;
	}	
	
	// save text to a file
	public boolean SaveText(String filename, String text, String encode) {
		boolean ret = false;
		try{
			OutputStream os = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
			PrintWriter writer =
				new PrintWriter(new OutputStreamWriter(os, encode));
			writer.append(text);
			//writer.flush();
			writer.close();
			os.close();
			ret = true;
		} catch(Exception e){
			_Log.e(TAG, "SaveText: failed: " + e);
		}
		return ret;
	}
	
	// load text from a file
	/* private String LoadText(String filename, String encode) {
		String ret = "";
		try{
			InputStream in = mContext.openFileInput(filename);
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(in, encode));
			String s;
			while ((s = reader.readLine())!= null){
				ret += s + "\r\n";
			}
			reader.close();
			in.close();
		}catch(Exception e){
			_Log.i(TAG, "LoadText: failed: " + e);
		}
		return ret;
	}*/

	// load text from a file
	public String LoadText2(String filename, String encode, int maxsize) {
		String ret = "";
		try{
			InputStream in = mContext.openFileInput(filename);
			int length = in.available();
			//Log.d(TAG, "LoadText [" + filename + "] " +  length + " bytes");
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(in, encode));
			// if file is too large, seek to middle of file  
			if (length > maxsize) {
				reader.skip(length - maxsize);
			}
			String s;
			int len = 0;
			while ((s = reader.readLine())!= null){
				len += s.length();
				if (len > maxsize) {
					break;
				}
				ret += s + "\n";
			}
			reader.close();
			in.close();
		} catch(Exception e){
			_Log.i(TAG, "LoadText2: failed: " + e);		
		}
		return ret;
	}
	
	// get process id of specified executable
	public int GetProcessIdByName(String strModulePath) {
		try {
			// Using 'ps' instead of ActivityManager.getRunningAppProcesses()
			Process p = Runtime.getRuntime().exec(CMD_PS);
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(p.getInputStream()));
			int read;
			char[] buf = new char[4096];
			StringBuffer output = new StringBuffer();
			output.setLength(0);
			while ((read = reader.read(buf)) > 0) {
				output.append(buf, 0, read);
			}
			reader.close();
			try {
				p.waitFor();
			} catch (Exception e) {
				return -1;
			}
			String lines = output.toString();
			String[] ary = lines.split("\n");
			for (int j = 0; j < ary.length;j++) {
				if (ary[j].indexOf(strModulePath) != -1) {
					// USER   PID PPID VSIZE RSS  WCHAN  PC NAME
					// app_33 790 778  1748  1160 c009b74c afd0dc74 S /dir/dir/stone
					String[] aryStoneLine = ary[j].split(" +");
					return Integer.parseInt(aryStoneLine[1]); // [1] => pid
				}
			}
		} catch (Exception e) {
			_Log.e(TAG, "GetProcessIdByName: failed: " + e);		
		}
		return -1; // not exist
	}

	// ask whether specified pid's process is exist
	public boolean ProcessIsExist(int pid) {
		try {
			Process p = Runtime.getRuntime().exec(CMD_PS + " " + pid);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			int len;
			char[] buf = new char[1024];
			StringBuffer output = new StringBuffer();
			output.setLength(0);
			while ((len = reader.read(buf)) > 0) {
				output.append(buf, 0, len);
			}
			reader.close();
			try {
				p.waitFor();
			} catch (Exception e) {
				return false;
			}
			String lines = output.toString();
			String[] ary = lines.split("\n");
			if (ary.length > 1) {
				return true;
			}
		} catch (Exception e) {
			_Log.e(TAG, "ProcessIsExist: failed: " + e);		
		}
		return false;
	}
	
	// send SIGINT to a process
	public boolean SigIntProcess(int pid, int retry) {
		int cnt = retry + 1;
		while (ProcessIsExist(pid) != false) {
			if (cnt-- <= 0) {
				_Log.e(TAG, "SigIntProcess: can't terminate pid=" + pid);
				return false;
			}
			android.os.Process.sendSignal(pid, 2);
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				continue;
			}
		}
	  	return true;
	}
	
}
