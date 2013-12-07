// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
import edu.mit.media.funf.probe.builtin.PedometerProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

import edu.mit.media.funf.probe.builtin.ProbeKeys.PedometerKeys;

@DesignerComponent(version = YaVersion.PEDOMETERSENSOR_COMPONENT_VERSION, 
		description = "Detect people's walking activity. The sensor will output a stream of \"step\" events when " +
				"a person starts walking. To continously detect whether a person is walking and keep track of how many" + 
				"steps he walks, one could set \"interval\"= duration + 1. There are 9 different sensitivities for " +
				"detecting walking step, including \"extra high\", \"very high\", \"high\", \"higher\", " +
				"\"medium\", \"lower\", \"low\", \"very low\", \"extra low\". ", 
		category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/pedometerSensor.png")
@SimpleObject
@UsesLibraries(libraries = "funf.jar")
public class PedometerSensor extends ProbeBase{


	private final String TAG = "PedometerSensor";
	private final String PEDOMETER_PROBE = "edu.mit.media.funf.probe.builtin.PedometerProbe";
	private PedometerProbe probe;
	
	private String sensitivityLevel;
	// [extra high, very high, high, higher, medium, lower, low(default), very low, extra low]  
	
//	private float rawVal; # rawVal is only for debugging purpose, should not return to AI user
	private long timestamp;
	
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 120; //scan for walking activity every 120 seconds
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	private final String SENSITIVITY_LEVEL = ProbeKeys.PedometerKeys.SENSITIVITY_LEVEL_LOW;
	
	
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			PedometerScanComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive pedometer data");
			/* returned json format 
			 *  
			 */
			Log.i(TAG, "timestamp:" + data.get(PedometerKeys.TIMESTAMP).getAsLong());
			Log.i(TAG, "rawVal:" + data.get(PedometerKeys.RAW_VALUE).getAsFloat());
			
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
	 
			timestamp = data.get(PedometerKeys.TIMESTAMP).getAsLong();

			Log.i(TAG, " before call PedometerInfoReceived()");
			PedometerInfoReceived(timestamp);
			Log.i(TAG, " after call PedometerInfoReceived()");

		}

	};
	
	public PedometerSensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		// TODO Auto-generated constructor stub
		form.registerForOnDestroy(this);
		 
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, PedometerProbe.class);

		//init all the defaults 
		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;
		sensitivityLevel = SENSITIVITY_LEVEL;
		
		
	}
	
	
	/**
	 * Indicates that the pedometer(one walking step) info has been received.
	 */
	@SimpleEvent
	public void PedometerInfoReceived(final long timestamp){
		if (enabled || enabledSchedule) {
			
			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "PedometerInfoReceived() is called");
					EventDispatcher.dispatchEvent(PedometerSensor.this,
							"PedometerInfoReceived", timestamp);
				}
			});

		}
	}
	
	/**
	 * Indicates that one round of scan has finish
	 */
	@SimpleEvent
	public void PedometerScanComplete() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "PedometerScanComplete() is called");
					EventDispatcher.dispatchEvent(PedometerSensor.this,
							"PedometerScanComplete");
				}
			});
		}
	}


  @SimpleFunction(description = "Enable pedometer sensor to run once")
  @Override
	public void Enabled(boolean enabled) {
		// TODO Auto-generated method stub
		if (this.enabled != enabled)
			this.enabled = enabled; // this will only run once for DURATION seconds

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
		// TODO Auto-generated method stub
		
		Log.i(TAG, "Registering data requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, PEDOMETER_PROBE);


		((JsonObject)dataRequest).addProperty(PedometerKeys.SENSITIVITY_LEVEL, this.sensitivityLevel);
		Log.i(TAG, "Data request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);

	}
	
	//*************  returning fields, fields setting *************
	
	
	/**
	 * Returns the default sensitivity level of walking detector
	 */
	
	
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public String SensitivityLevel() {
		
		return this.sensitivityLevel;
	}
	
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, 
			defaultValue = PedometerKeys.SENSITIVITY_LEVEL_LOW)
	@SimpleProperty(description = "The sensitivity level used to detect walking. " +
			"9 different options for this value: \"extra high\", \"very high\", \"high\", \"higher\", " +
				"\"medium\", \"lower\", \"low\", \"very low\", \"extra low\"")
	public void SensitivityLevel(String newSensitivityLevel ) {
		
		this.sensitivityLevel = newSensitivityLevel;
	}
	
	
//	/**
//	 * Returns the timestamp of latest reading
//	 */
//	@SimpleProperty(description = "The timestamp of this sensor event.")
//	public float Timestamp() {
//		Log.i(TAG, "returning timestamp: " + timestamp);
//		return timestamp;
//	}
	
	
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
