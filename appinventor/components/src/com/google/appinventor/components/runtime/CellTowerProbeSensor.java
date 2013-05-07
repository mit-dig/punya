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
import edu.mit.media.funf.probe.builtin.CellTowerProbe;
import edu.mit.media.funf.probe.builtin.SensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.CellKeys;




@DesignerComponent(version = YaVersion.CELLTOWERPROBESENSOR_COMPONENT_VERSION, 
		description = "A component that detects information of the cell tower that the cellphone connects to", 
		category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/cellTowerProbe.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_COARSE_LOCATION")
@UsesLibraries(libraries = "funf.jar")
public class CellTowerProbeSensor extends ProbeBase{
	
	private final String TAG = "CellTowerProbe";
	private final String CELLTOWER_PROBE = "edu.mit.media.funf.probe.builtin.CellTowerProbe";
	
	private long cellid;
	private long locationAreaCode;
	private long timestamp;
	
	private CellTowerProbe probe;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 1800; //scan for celltower info  points every 30 minutes
	private final int SCHEDULE_DURATION = 30; //scan for 30 seconds everytime
	
	
	public CellTowerProbeSensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub

		form.registerForOnDestroy(this);
 
			mainUIThreadActivity = container.$context();
			Log.i(TAG, "Before create probe");
			gson = new GsonBuilder().registerTypeAdapterFactory(
					FunfManager.getProbeFactory(mainUIThreadActivity)).create();
			JsonObject config = new JsonObject();

			probe = gson.fromJson(config, CellTowerProbe.class);

			interval = SCHEDULE_INTERVAL;
			duration = SCHEDULE_DURATION;
 
		
	}
	
	final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			IJsonObject data = (IJsonObject) msg.obj;
			Log.i(TAG, "Update component's varibles.....");
			
			cellid =  data.get(CellKeys.CID).getAsLong();
			locationAreaCode = data.get(CellKeys.LAC).getAsLong();
	 
			timestamp = data.get(CellKeys.TIMESTAMP).getAsLong();


			Log.i(TAG, " before call CellInfoReceived()");
			CellInfoReceived();
			Log.i(TAG, " after call CellInfoReceived()");

		}

	};
	
	/**
	 * Indicates that the celltower sensor info has been received.
	 */
	@SimpleEvent
	public void CellInfoReceived(){
		if (enabled || enabledSchedule) {
			
			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "CellInfoReceived is called");
					EventDispatcher.dispatchEvent(CellTowerProbeSensor.this,
							"CellInfoReceived");
				}
			});
			
			
		}
		
	}
	
	/**
	 * Indicates that one round of scan has finish
	 */
	@SimpleEvent
	public void CellTowerScanComplete() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "CellTowerScanComplete() is called");
					EventDispatcher.dispatchEvent(CellTowerProbeSensor.this,
							"CellTowerScanComplete");
				}
			});
		}
	}
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			CellTowerScanComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive celltower data");
			/* returned json format 
			 * 
			 * {"@type":"edu.mit.media.funf.probe.builtin.CellTowerProbe"} 
			 * {"cid":184559962,"lac":64921,"psc":-1,"timestamp":1345076962.112,"type":1}	
			 *  
			*/
 
			
			Log.i(TAG, "cellid:" + data.get(CellKeys.CID).getAsLong());
			Log.i(TAG, "locationAreaCode:" + data.get(CellKeys.LAC).getAsLong() );
			Log.i(TAG, "timestamp:" + data.get(CellKeys.TIMESTAMP).getAsLong());
			
			//save data to DB is enabledSaveToDB is true
			if(enabledSaveToDB){
				
				saveToDB(completeProbeUri, data);
			}

			//CellInfoReceived();
			Message msg = myHandler.obtainMessage();

			msg.obj = data;	

			myHandler.sendMessage(msg);

 
		}

	};


	/**
	 * Indicates whether the sensor should "run once" to listen for celltower information  
	 */

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

		dataRequest = getDataRequest(interval, duration, CELLTOWER_PROBE);

		 
		Log.i(TAG, "Data request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);
 
		
	}
	
	/**
	 * Returns the latest reading of the cell id of the cell tower
	 */
	@SimpleProperty(description = "The cell id of the cell tower.")
	public long Cellid() {
		Log.i(TAG, "returning cell id: " + cellid);
		return cellid;
	}
	
	/**
	 * Returns the latest reading of the area code of the cell tower
	 */
	@SimpleProperty(description = "The location area code of the cell tower.")
	public long LocationAreaCode() {
		Log.i(TAG, "returning cell id: " + locationAreaCode);
		return locationAreaCode;
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
