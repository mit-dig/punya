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
import edu.mit.media.funf.probe.builtin.LightSensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.probe.builtin.RunningApplicationsProbe;


@DesignerComponent(version = YaVersion.LIGHTSENSOR_COMPONENT_VERSION, 
		description = "Return information of the current illuminances (lux) by using the light sensor " , 
		category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/lightsensorProbe.png")
@SimpleObject
@UsesLibraries(libraries = "funf.jar")
public class LightSensor extends ProbeBase {
	
	
	private final String TAG = "LightSensor";
	private LightSensorProbe probe;
	private final String LIGHTSENSOR_PROBE = "edu.mit.media.funf.probe.builtin.LightSensorProbe";

	
	private int lux;
	private long timestamp;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 1800; //read illuminances every 1800 seconds (30 minutes)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	
	public LightSensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		
		form.registerForOnDestroy(this);
		 
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, LightSensorProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;
		
		
		
	}
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			//do nothing. LightSensorProbe does not have onDataComplete event
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data of illuminances");
			/* returned json format 
 			 {"accuracy":0,"lux":100.0,"timestamp":946695643.291918}
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
			
			lux = data.get(ProbeKeys.LightSensorKeys.LUX).getAsInt();
			timestamp = data.get(ProbeKeys.LightSensorKeys.TIMESTAMP).getAsLong();
	
			Log.i(TAG, " before call LightInfoReceived()");
			LightInfoReceived();
			Log.i(TAG, " after call LightInfoReceived()");

		}

	};	
	
	
	/**
	 * Indicates whether the sensor should "run once" to read the current illuminance
	 */

  @SimpleFunction(description = "Enable light sensor probe to run once")
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
		// TODO Auto-generated method stub
		Log.i(TAG, "Registering data requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, LIGHTSENSOR_PROBE);
		Log.i(TAG, "Data request: " + dataRequest.toString());
		mBoundFunfManager.requestData(listener, dataRequest);
	}
	
	/**
	 * Indicates that the illuminance(light) info has been received.
	 */
	@SimpleEvent
	public void LightInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "LightInfoReceived() is called");
					EventDispatcher.dispatchEvent(LightSensor.this,
							"LightInfoReceived");
				}
			});

		}

	}
	
	/**
	 * Returns whether the screen is currently on
	 */
	@SimpleProperty(description = "The illuminance(lux) of light in the current environment.")
	public int Lux() {
		Log.i(TAG, "returning lux: " + lux);
		return lux;
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
