// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
//import edu.mit.media.funf.probe.ProbeFactory;
//import edu.mit.media.funf.probe.ProbeFactory.BasicProbeFactory;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ProximitySensorKeys;
import edu.mit.media.funf.probe.builtin.LightSensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;
import edu.mit.media.funf.probe.builtin.SensorProbe;
//import edu.mit.media.funf.JsonUtils;

/**
 * Detects whether or not the phone is close to the ear or not.
 * 
 * @author abhagi@mit.edu (Anshul Bhagi)
 * @author fuming@mit.edu (Fuming Shih)
 * 
 */
@DesignerComponent(version = YaVersion.PROXIMITYSENSOR_COMPONENT_VERSION, description = "A component that detects whether the phone is close to an object or not.", category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/proximity.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WAKE_LOCK")
@UsesLibraries(libraries = "funf.jar")
public class ProximitySensor extends ProbeBase{
	
	static final String PROXIMITY_SENSOR_UPDATE_ACTION = "PROXIMITY_SENSOR_UPDATE_ACTION";
	
	private final String TAG = "ProximitySensor";
	private ProximitySensorProbe probe;
	private final String PROXIMITYSENSOR_PROBE = "edu.mit.media.funf.probe.builtin.ProximitySensorProbe";

	private boolean enabled = true;
	private boolean enabledSchedule; // run periodically
	private boolean phoneIsCloseToObj = false;
	private float distanceBtwnSensorAndObject = 0f;

	private Activity mainUIThreadActivity;
	private Gson gson;
	
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 300; //read proximity sensor every 300 seconds (5 minutes)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	

	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri, JsonElement arg1) {
			// don't do anything here. This probe does not have onDataCompleted event
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {

			
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
			
			distanceBtwnSensorAndObject = data.get(ProximitySensorKeys.DISTANCE).getAsFloat();
			
			 Log.i(TAG, "received sensor info, distance = " + distanceBtwnSensorAndObject);
			
			// if the phone is close to an object, the distance will be zero
			if (distanceBtwnSensorAndObject == 0.0){
				phoneIsCloseToObj = true;
			}
			else {
				phoneIsCloseToObj = false;
			}
//			Log.i(TAG, " before call ProximityInfoReceived()");
			ProximityInfoReceived();
//			Log.i(TAG, " after call ProximityInfoReceived()");
			

		}

	};	
	
		
	public ProximitySensor(ComponentContainer container) {
		super(container.$form());
		
			mainUIThreadActivity = container.$context();
			gson = new GsonBuilder().registerTypeAdapterFactory(FunfManager.getProbeFactory(mainUIThreadActivity)).create();
			JsonObject  config = new JsonObject();
			 
			probe = gson.fromJson(config, ProximitySensorProbe.class);

			interval = SCHEDULE_INTERVAL;
			duration = SCHEDULE_DURATION;

	}
	  
	
	
	
	/**
	 * Indicates whether the user has specified that the sensor should listen
	 * for changes and raise the corresponding events.
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean Enabled() {
		return enabled;
	}
	/**
	 * Indicates whether the sensor should run once to listen for proximity
	 * changes and raise the corresponding events.
	 */

  @SimpleFunction(description = "Enable proximity sensor to run once")
  @Override
	public void Enabled(boolean enabled) {
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
	
	/**
	 * Indicates whether the user has specified that the sensor should listen
	 * for changes and raise the corresponding events.
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean EnabledSchedule() {
		return enabledSchedule;
	}

 
	
	/**
	 * Returns the latest reading of the device's proximity to another object.
	 */
	@SimpleProperty
	public boolean PhoneIsCloseToObject() {
		return phoneIsCloseToObj;
	}
	
	/**
	 * Indicates that the proximity sensor info has been received.
	 */
	@SimpleEvent
	public void ProximityInfoReceived(){
		if (enabled) {
		  mainUIThreadActivity.runOnUiThread(new Runnable() {
		    public void run(){
		    	EventDispatcher.dispatchEvent(ProximitySensor.this, "ProximityInfoReceived");
			}
		  });
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
		// TODO Auto-generated method stub
		Log.i(TAG, "Registering data requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, PROXIMITYSENSOR_PROBE);
		Log.i(TAG, "Data request: " + dataRequest.toString());
		mBoundFunfManager.requestData(listener, dataRequest);
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
