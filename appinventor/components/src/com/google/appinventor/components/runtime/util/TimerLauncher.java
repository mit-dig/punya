// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime.util;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TimerLauncher extends BroadcastReceiver{
	private static boolean launched = false;
	
	public static void launch(Context context) {
		Log.v("TimerLauncher", "Launched!");
		Intent i = new Intent(context.getApplicationContext(), TimerManager.class);
		context.getApplicationContext().startService(i);
		launched = true;
	}
	
	public static boolean isLaunched() {
		return launched;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		launch(context);
	}

}
