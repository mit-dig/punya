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
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.probe.builtin.RunningApplicationsProbe;


@DesignerComponent(version = YaVersion.RUNNINGAPPLICATIONS_COMPONENT_VERSION, 
		description = "The current running stack of applications. Tells you what application that the user is currently using" , 
		category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/runningAppsProbe.png")
@UsesPermissions(permissionNames = "android.permission.GET_TASKS")
@SimpleObject
@UsesLibraries(libraries = "funf.jar")
public class RunningApplications extends ProbeBase{
	
	private final String TAG = "RunningApplicationsProbe";
	private final String RUNNINGAPPLICATIONS_PROBE = "edu.mit.media.funf.probe.builtin.RunningApplicationsProbe";
	private RunningApplicationsProbe probe;
	
	private String runningPackage = null;
	private String runningClass = null;
	private long timestamp;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 3600; //scan for running apps 1800 seconds (30 minutes)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			// do nothing, RunningApplicationsProbe does not generate onDataCompleted event
			//AppsScanComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data of running applications");
			/* returned json format 
				{"duration":2.010,
				"taskInfo":{
				            "baseIntent":{
				                 "mAction":"android.intent.action.MAIN",
				                "mCategories":["android.intent.category.LAUNCHER"],
				                "mComponent":{
				                     "mClass":"com.dictionary.Splash","mPackage":"com.dictionary"},
				                "mFlags":270532608,
				                "mSourceBounds":{
				                     "bottom":482,"left":125,"right":235,"top":344}},
				          "id":16},
				"timestamp":1345091907.068}

			 *  
			 */
			//debug
			JsonObject taskInfo = data.get("taskInfo").getAsJsonObject();
			JsonObject baseIntent = taskInfo.get("baseIntent").getAsJsonObject();
			JsonObject mComponent = baseIntent.get("mComponent").getAsJsonObject();
			
			String mClass = mComponent.get("mClass").getAsString();
			String mPackage = mComponent.get("mPackage").getAsString();
			//debug
 
			Log.i(TAG, "DATA: " + data.toString());
			Log.i(TAG, "mPackage:" + mPackage); // tells which application is running
			Log.i(TAG, "mClass:" + mClass); // tells which class of this application is currently running
			
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
			
			JsonObject taskInfo = data.get("taskInfo").getAsJsonObject();
			JsonObject baseIntent = taskInfo.get("baseIntent").getAsJsonObject();
			JsonObject mComponent = baseIntent.get("mComponent").getAsJsonObject();
			
			runningClass = mComponent.get("mClass").getAsString();
			runningPackage = mComponent.get("mPackage").getAsString();
			timestamp = data.get(ProbeKeys.ActivityKeys.TIMESTAMP).getAsLong();
			
			
			Log.i(TAG, " before call ApplicationsInfoReceived()");
			AppsInfoReceived();
			Log.i(TAG, " after call ApplicationsInfoReceived()");

		}

	};	
	
 
 
	
	public RunningApplications(ComponentContainer container) {
		
		super(container);
		// TODO Auto-generated constructor stub
		form.registerForOnDestroy(this);
		 
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, RunningApplicationsProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;
		
 
	}

	/**
	 * Indicates whether the sensor should "run once" to probe the current running applications
	 */

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
	@SimpleProperty
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

		dataRequest = getDataRequest(interval, duration, RUNNINGAPPLICATIONS_PROBE);
		Log.i(TAG, "Data request: " + dataRequest.toString());
		mBoundFunfManager.requestData(listener, dataRequest);
		
	}

	
	/**
	 * Indicates that the running applications info has been received.
	 */
	@SimpleEvent
	public void AppsInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "AppsInfoReceived() is called");
					EventDispatcher.dispatchEvent(RunningApplications.this,
							"AppsInfoReceived");
				}
			});

		}

	}
	
	/**
	 * Returns the latest reading of package name of the running app
	 */
	@SimpleProperty(description = "The activity level of the interval.")
	public String PackageName() {
		Log.i(TAG, "returning packageName: " + runningPackage);
		return runningPackage;
	}
	
	
	/**
	 * Returns the latest reading of the executing (java)class name of the running app
	 */
	@SimpleProperty(description = "The activity level of the interval.")
	public String ClassName() {
		Log.i(TAG, "returning className: " + runningClass);
		return runningClass;
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
