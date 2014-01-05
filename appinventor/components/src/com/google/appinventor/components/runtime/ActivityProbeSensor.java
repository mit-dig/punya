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
import edu.mit.media.funf.probe.builtin.ActivityProbe;
import edu.mit.media.funf.probe.builtin.CellTowerProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ActivityKeys;
import edu.mit.media.funf.probe.builtin.ProbeKeys.CellKeys;
 


@DesignerComponent(version = YaVersion.ACTIVITYPROBESENSOR_COMPONENT_VERSION, 
		description = "Records how active the person is. Uses the AccelerometerProbe data to calculate" +
				"each interval the variance of a device's acceleration and assign labels (high/medium/low) based on " +
				"whether the variance is above a certain threshold. " +
				"Intervals are 1 seconds long." , 
		category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/activityProbe.png")
@SimpleObject
@UsesLibraries(libraries = "funf.jar")
public class ActivityProbeSensor extends ProbeBase{
	
	private final String TAG = "ActivityProbe";
	private final String ACTIVITY_PROBE = "edu.mit.media.funf.probe.builtin.ActivityProbe";
	private ActivityProbe probe;
	
	private String activityLevel;
	private long timestamp;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 120; //scan for activity level every 120 seconds
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			ActivityScanComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive activitytower data");
			/* returned json format 
			 * {"activityLevel":"none","timestamp":1345090214.061}
			 * {"activityLevel":"low","timestamp":1345090214.061}
			 * {"activityLevel":"high","timestamp":1345090214.061
			 *  
			*/
			Log.i(TAG, "activity_level:" + data.get(ActivityKeys.ACTIVITY_LEVEL).getAsString());
			Log.i(TAG, "timestamp:" + data.get(CellKeys.TIMESTAMP).getAsLong());
			
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
			
			activityLevel =  data.get(ActivityKeys.ACTIVITY_LEVEL).getAsString();
	 
			timestamp = data.get(CellKeys.TIMESTAMP).getAsLong();


			Log.i(TAG, " before call ActivityInfoReceived()");
			ActivityInfoReceived(timestamp, activityLevel);
			Log.i(TAG, " after call ActivityInfoReceived()");

		}

	};

	public ActivityProbeSensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		form.registerForOnDestroy(this);
		 
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, ActivityProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;
	}
	
	/**
	 * Indicates whether the sensor should "run once" to listen for activity level information
	 */

  @SimpleFunction(description = "Enable activity probe sensor to run once")
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

		dataRequest = getDataRequest(interval, duration, ACTIVITY_PROBE);

		 
		Log.i(TAG, "Data request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);
 
		
	}
	
	
	/**
	 * Indicates that the activity info has been received.
	 */
	@SimpleEvent
	public void ActivityInfoReceived(final long timestamp, final String activityLevel){
		if (enabled || enabledSchedule) {
			
			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "ActivityInfoReceived() is called");
					EventDispatcher.dispatchEvent(ActivityProbeSensor.this,
							"ActivityInfoReceived", timestamp, activityLevel);
				}
			});
			
			
		}
		
	}
	
	/**
	 * Indicates that one round of scan has finish
	 */
	@SimpleEvent
	public void ActivityScanComplete() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "ActivityScanComplete() is called");
					EventDispatcher.dispatchEvent(ActivityProbeSensor.this,
							"ActivityScanComplete");
				}
			});
		}
	}
	
	/**
	 * Returns the latest reading of the activity level of one interval
	 */
//	@SimpleProperty(description = "The activity level of the interval.")
//	public String ActivityLevel() {
//		Log.i(TAG, "returning activityLevel: " + activityLevel);
//		return activityLevel;
//	}
	
	/**
	 * Returns the timestamp of latest reading 
	 */
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
