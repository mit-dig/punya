// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime.util;

import com.google.appinventor.components.runtime.ProbeBase;

import android.content.SharedPreferences;
import android.util.Log;
import edu.mit.media.funf.storage.HttpUploadService;
import edu.mit.media.funf.storage.RemoteFileArchive;

public class HttpsUploadService extends HttpUploadService {
	private SharedPreferences prefs;

	// this class will register itself to FunfManager for OAuth2 upload periodically
	@Override
	protected RemoteFileArchive getRemoteArchive(String name) {
	prefs = ProbeBase.getSystemPrefs(this);
    String access_token = ""; 
    // Getting Access Token
    try {
 
      access_token = prefs.getString("accessToken", "");
      Log.d("UPLOADDATA", "access_token"+access_token);
    } catch(Exception ex) {
    }
    return new HttpsArchive(name, access_token);
	}

}