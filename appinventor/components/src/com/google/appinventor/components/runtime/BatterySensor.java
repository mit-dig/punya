// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.LightSensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;



@DesignerComponent(version = YaVersion.BATTERYSENSOR_COMPONENT_VERSION, 
		description = "Information about the type and current state of the battery in the device. " , 
		category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/batterySensor.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.BATTERY_STATS")
@UsesLibraries(libraries = "funf.jar")
public class BatterySensor extends ProbeBase{
	
	private final String TAG = "BatterySensor";
	private BatteryProbe probe;
	private final String BATTERYSENSOR_PROBE = "edu.mit.media.funf.probe.builtin.BatteryProbe";

	//fields for information about battery status
	
	private int scale;
	private int level;
	private long timestamp;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 900; //read battery status every 900 seconds (30 minutes)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime


	protected BatterySensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		
		
		form.registerForOnDestroy(this);
		 
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, BatteryProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;
	}
	
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			//do nothing. BatterySensor does not have onDataComplete event
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data of battery info");
			/* returned json format 
 			   
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
			
			 
			timestamp = data.get(ProbeKeys.BatteryKeys.TIMESTAMP).getAsLong();
			scale = data.get(ProbeKeys.BatteryKeys.SCALE).getAsInt();
			level = data.get(ProbeKeys.BatteryKeys.LEVEL).getAsInt();
	
			Log.i(TAG, " before call BatteryInfoReceived()");
			BatteryInfoReceived();
			Log.i(TAG, " after call BatteryInfoReceived()");

		}

	};	
	
	
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
	@SimpleProperty
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

		dataRequest = getDataRequest(interval, duration, BATTERYSENSOR_PROBE);
		Log.i(TAG, "Data request: " + dataRequest.toString());
		mBoundFunfManager.requestData(listener, dataRequest);
		
	}
	
	/**
	 * Indicates that the battery status info has been received.
	 */
	@SimpleEvent
	public void BatteryInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "BatteryInfoReceived() is called");
					EventDispatcher.dispatchEvent(BatterySensor.this,
							"BatteryInfoReceived");
				}
			});

		}

	}
	
	/**
	 * Returns the current battery level, from 0 to Scale()
	 */
	@SimpleProperty(description = "the current battery level of the device")
	public int Level() {
		Log.i(TAG, "returning level: " + level);
		return level;
	}
	
	/**
	 * Returns the timestamp of latest reading 
	 */
	@SimpleProperty(description = "The timestamp of this sensor event.")
	public float Timestamp() {
		Log.i(TAG, "returning timestamp: " + timestamp);
		return timestamp;
	}
	
	/**
	 * Returns the battery SCALE, integer containing the maximum battery level
	 */
	@SimpleProperty(description = "Battery SCALE, integer containing the maximum battery level")
	public int Scale() {
		Log.i(TAG, "returning bettery scale: " + scale);
		return scale;
	}
	
	
	

}
