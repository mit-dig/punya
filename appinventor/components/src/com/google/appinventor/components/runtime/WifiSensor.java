// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import kawa.standard.thisRef;

import android.R;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

 
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.SensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BluetoothKeys;
import edu.mit.media.funf.probe.builtin.ProbeKeys.WifiKeys;
import edu.mit.media.funf.probe.builtin.WifiProbe;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.time.DecimalTimeUnit;

@DesignerComponent(version = YaVersion.WIFISENSOR_COMPONENT_VERSION, description = "A component that detects information of the nearby Wireless Access Point", category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/wifiProbe.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_WIFI_STATE, "
	+ "android.permission.CHANGE_WIFI_STATE")
public class WifiSensor extends ProbeBase{

	
	private final String TAG = "wifiSensor";
	private final String WIFI_PROBE = "edu.mit.media.funf.probe.builtin.WifiProbe";
	
	
	// wifi sensor return info
	
	private String bssid;
	private String ssid;
	private String capabilities;
	private int frequency;
	private int level;
	private long timestamp;
	
	private WifiProbe probe;
	
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 300; //scan for wifi access points every 5 minutes
	private final int SCHEDULE_DURATION = 30; //scan for 30 seconds everytime
	
	public WifiSensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub

		// Set up listeners
		form.registerForOnDestroy(this);

		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();
		config.addProperty("sensorDelay", SensorProbe.SENSOR_DELAY_NORMAL);

		probe = gson.fromJson(config, WifiProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;

	}
	
	final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			IJsonObject data = (IJsonObject) msg.obj;
			Log.i(TAG, "Update component's varibles.....");
			
			bssid =  data.get(WifiKeys.BSSID).getAsString();
			ssid = data.get(WifiKeys.SSID).getAsString();
			capabilities = data.get(WifiKeys.CAPABILITIES).getAsString();
			frequency = data.get(WifiKeys.FREQUENCY).getAsInt();
			level = data.get(WifiKeys.LEVEL).getAsInt();
			timestamp = data.get(WifiKeys.TIMESTAMP).getAsLong();


			Log.i(TAG, " before call WifiInfoReceived()");
			WifiInfoReceived();
			Log.i(TAG, " after call WifiInfoReceived()");

		}

	};
	
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			WifiScanComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data");
			/* returned json format 
			 * 
			 * {"BSSID":"00:1f:5b:88:97:e3",
			 *  "SSID":"scbackup",
			 *  "capabilities":"[WPA2-PSK-CCMP]",
			 *  "frequency":2462,
			 *  "level":-60,
			 *  "timestamp":1344437246.516}
			 *  
			 *  http://developer.android.com/reference/android/net/wifi/ScanResult.html
			*/
			
			Log.i(TAG, "DATA: " + data.toString());
			
			//save data to DB is enabledSaveToDB is true
			if(enabledSaveToDB){
				
				saveToDB(completeProbeUri, data);
			}

			//WifiInfoReceived();
			Message msg = myHandler.obtainMessage();

			msg.obj = data;	

			myHandler.sendMessage(msg);

 
		}

	};
	
	/**
	 * Returns the latest reading of the BSSID of a wireless AP
	 */
	@SimpleProperty(description = "The address of the access point.")
	public String BSSID() {
		Log.i(TAG, "returning BSSID: " + bssid);
		return bssid;
	}
	
	/**
	 * Returns the latest reading of the SSID of a wireless AP
	 */
	@SimpleProperty(description = "The network nam of the access point.")
	public String SSID() {
		Log.i(TAG, "returning SSID: " + ssid);
		return ssid;
	}
	
	/**
	 * Returns the latest reading of the capabilities of a wireless AP
	 */
	@SimpleProperty(description = "Describes the authentication, key management, and encryption schemes supported by the access point.")
	public String Capabilities() {
		Log.i(TAG, "returning capbilities: " + capabilities);
		return capabilities;
	}
	
	/**
	 * Returns the latest reading of the signal level of a wireless AP
	 */
	@SimpleProperty(description = "The detected signal level in dBm.")
	public int Level() {
		Log.i(TAG, "returning level: " + level);
		return level;
	}
	
	/**
	 * Returns the timestamp of the latest reading 
	 */
	@SimpleProperty(description = "The timestamp of this sensor event.")
	public float Timestamp() {
		Log.i(TAG, "returning timestamp: " + timestamp);
		return timestamp;
	}
	
	
	
	/**
	 * Returns the latest reading of the frequency of a wireless AP
	 */
	@SimpleProperty(description = "The frequency in MHz of the channel over which the client is communicating with the access point.")
	public int Frequency() {
		Log.i(TAG, "returning frequency: " + frequency);
		return frequency;
	}
	
	
	/**
	 * Indicates that the Wifi sensor info has been received.
	 */
	@SimpleEvent
	public void WifiInfoReceived() {
		if (enabled || enabledSchedule) {
			
			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "WifiInfoReceived() is called");
					EventDispatcher.dispatchEvent(WifiSensor.this,
							"WifiInfoReceived");
				}
			});
			
			
		}
	}
	
	
	/**
	 * Indicates that one round of scan has finish
	 */
	@SimpleEvent
	public void WifiScanComplete() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "WifiScanComplete() is called");
					EventDispatcher.dispatchEvent(WifiSensor.this,
							"WifiScanComplete");
				}
			});
		}
	}

	/**
	 * Indicates whether the sensor should "run once" to listen for wifi access points information
	 * and raise the corresponding events.
	 */

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
	@SimpleProperty
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
	public void registerDataRequest(int interval, int duration) {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "Registering data requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, WIFI_PROBE);

		 
		Log.i(TAG, "Data request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);
 
	
	}
	@Override
	public void unregisterDataRequest() {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "Unregistering data requests.");
		//mBoundFunfManager.stopForeground(true);
		mBoundFunfManager.unrequestAllData2(listener);

		Log.i(TAG, "After Unregistering data requests.");

		
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
