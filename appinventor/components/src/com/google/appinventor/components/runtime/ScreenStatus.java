// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.probe.builtin.RunningApplicationsProbe;
import edu.mit.media.funf.probe.builtin.ScreenProbe;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

@DesignerComponent(version = YaVersion.SCREENSTATUS_COMPONENT_VERSION, 
		description = "Return information about the  " , 
		category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/screenProbe.png")
@SimpleObject
@UsesLibraries(libraries = "funf.jar")
public class ScreenStatus extends ProbeBase{

	private final String TAG = "ScreenStatus";
	private ScreenProbe probe;
	private final String SCREENSTATUS_PROBE = "edu.mit.media.funf.probe.builtin.ScreenProbe";
	
	private boolean screen_on;
	private long timestamp;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 900; //scan for screen status every 900 seconds (15 minutes)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	
	public ScreenStatus(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		form.registerForOnDestroy(this);
		 
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, ScreenProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;
			
	}
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			//do nothing. ScreenStatus does not have onDataComplete event
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data of running applications");
			/* returned json format 
 			{"screenOn":true,"timestamp":946695532.248}
			 */
			//debug

			Log.i(TAG, "DATA: " + data.toString());
			//debug
			
			//save data to DB is enabledSaveToDB is true
			if(enabledSaveToDB){
				
				saveToDB(completeProbeUri, data);
			}

			Message msg = myHandler.obtainMessage();
			msg.obj = data;	
			myHandler.sendMessage(msg);

 
		}

	};
	
	final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			IJsonObject data = (IJsonObject) msg.obj;
			Log.i(TAG, "Update component's varibles.....");
			
			screen_on = data.get(ProbeKeys.ScreenKeys.SCREEN_ON).getAsBoolean();
			timestamp = data.get(ProbeKeys.ScreenKeys.TIMESTAMP).getAsLong();
			
			
			Log.i(TAG, " before call ApplicationsInfoReceived()");
			ScreenInfoReceived();
			Log.i(TAG, " after call ApplicationsInfoReceived()");

		}

	};	

	/**
	 * Indicates whether the sensor should "run once" to listen for current screenStatus
	 */

  @SimpleFunction(description = "Enable screen status sensor to run once")
  @Override
	public void Enabled(boolean enabled) {
		// TODO Auto-generated method stub
		if (this.enabled != enabled)
			this.enabled = enabled;

		if (enabled) {
			probe.registerListener(listener);
			Log.i(TAG, "register listener for run-once");
		} else {
			probe.unregisterListener(listener);
			Log.i(TAG, "unregister run-once listener");
		}
		
	}

	@Override
	public void unregisterDataRequest() {
		// TODO Auto-generated method stub
		Log.i(TAG, "Unregistering data requests.");
		mBoundFunfManager.unrequestAllData2(listener);

		Log.i(TAG, "After Unregistering data requests.");
		
	}

	@Override
	public void registerDataRequest(int interval, int duration) {
		Log.i(TAG, "Registering data requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, SCREENSTATUS_PROBE);
		Log.i(TAG, "Data request: " + dataRequest.toString());
		mBoundFunfManager.requestData(listener, dataRequest);
		
	}
	

	
	/**
	 * Indicates that the running applications info has been received.
	 */
	@SimpleEvent
	public void ScreenInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "ScreenInfoReceived() is called");
					EventDispatcher.dispatchEvent(ScreenStatus.this,
							"ScreenInfoReceived");
				}
			});

		}

	}
	
	/**
	 * Returns whether the screen is currently on
	 */
	@SimpleProperty(description = "The status of whether the screen on or not.")
	public boolean ScreenOn() {
		Log.i(TAG, "returning screen_on: " + screen_on);
		return screen_on;
	}
	
	/**
	 * Returns the timestamp of latest reading 
	 */
	@SimpleProperty(description = "The timestamp of this sensor event.")
	public float Timestamp() {
		Log.i(TAG, "returning timestamp: " + timestamp);
		return timestamp;
	}
	
	/*
	 * Returns the default interval between each scan for this probe
	 */
	@SimpleProperty(description = "The default interval (in seconds) between each scan for this probe")
	public float DefaultInterval(){
		
		return SCHEDULE_INTERVAL;
	}
	
	/*
	 * Returns the default duration of each scan for this probe
	 */
	@SimpleProperty(description = "The default duration (in seconds) of each scan for this probe")
	public float DefaultDuration(){
		
		return SCHEDULE_DURATION;
	}
	

}
