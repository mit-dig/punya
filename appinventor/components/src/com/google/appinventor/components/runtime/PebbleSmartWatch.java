// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.util.UUID;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

/* 
 * @author wli17@mit.edu (Weihua Li)
 */
@DesignerComponent(version = YaVersion.PEBBLESMARTWATCH_COMPONENT_VERSION, 
    description = "<p></p> ",
    category = ComponentCategory.SOCIAL,
    nonVisible = true, 
    iconName = "images/pebble.png")
@UsesPermissions(permissionNames = 
        "android.permission.WAKE_LOCK, "
        +"android.permission.BLUETOOTH, "
        +"android.permission.BLUETOOTH_ADMIN"
        )
public final class PebbleSmartWatch extends AndroidNonvisibleComponent
        implements Component, OnDestroyListener {

    private final String TAG = "PebbleSmartWatch";
    protected Activity mainUIThreadActivity;
    protected Context context;
    
    // the tuple key corresponding to the weather icon displayed on the watch
    private static final int ICON_KEY = 0;
    // the tuple key corresponding to the temperature displayed on the watch
    private static final int MSG = 1;
    // the tuple key corresponding to the vibration on the watch
    private static final int VIBR_KEY = 2;
    // This UUID identifies the weather app
    private static final UUID APP_UUID = UUID.fromString("28AF3DC7-E40D-490F-BEF2-29548C8B0600");

    public PebbleSmartWatch(ComponentContainer container) {
        super(container.$form());
        form.registerForOnDestroy(this);
        
        context = container.$context();
        mainUIThreadActivity = container.$context();
        Log.i(TAG,"");
    }
    
    @SimpleFunction(description = "Send a text message to the pebble.")
    public void SendMessageToPebble(int iconId, boolean vibrate, String message) {
    	
        // Build up a Pebble dictionary containing the weather icon and the current temperature in degrees celsius
        PebbleDictionary data = new PebbleDictionary();
        data.addUint8(ICON_KEY, (byte) iconId);
        data.addString(MSG, message);
        
        if (vibrate) {
        	data.addInt8(VIBR_KEY, (byte) 1);
        } else {
        	data.addInt8(VIBR_KEY, (byte) 0);       	
        }
        
        // Send the assembled dictionary to the weather watch-app; this is a no-op if the app isn't running or is not
        // installed
        PebbleKit.sendDataToPebble(context, APP_UUID, data);
    }

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub		
	}
}