// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;

import android.content.Intent;

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
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.BluetoothReflection;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
 

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;

import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.SensorProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BluetoothKeys;

/**
 * Detects whether or not a Bluetooth-enabled device is close by or not.
 * 
 * @author fuming@mit.edu (Fuming Shih) TODO: let's do detecting one friend's
 *         device first, then try multiple devices specified in a list to detect
 *         about
 * 
 */

@DesignerComponent(version = YaVersion.SOCIALPROXIMITYSENSOR_COMPONENT_VERSION, description = "A component that detects whether the Bluetooth-enabled device owned by a friend is close by or not.", category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/socialProximitysensor.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WAKE_LOCK, "
		+ "android.permission.BLUETOOTH, "
		+ "android.permission.BLUETOOTH_ADMIN")
@UsesLibraries(libraries = "funf.jar")
public class SocialProximitySensor extends ProbeBase{
//	static final String SOCIAL_PROXIMITY_SENSOR_UPDATE_ACTION = "SOCIAL_PROXIMITY_SENSOR_UPDATE_ACTION";

	private boolean enabled; // run once
	private boolean enabledSchedule; // run periodically
	private BluetoothAdapter bluetoothAdapter; // bluetooth adapter (not all cellphone has Bluetooth)
	private static final int REQUEST_ENABLE_BT = 1001;

	private final String BLUETOOTH_PROBE = "edu.mit.media.funf.probe.builtin.BluetoothProbe";
	private final int PROBE_SCHEDULE_NOTIFICATION = 1337;
	
	private NotificationManager mNM;
	boolean isScanningForFirstTime = true;
	
	/*
	 * Note, do make a probe runs again, just register the listener again
	 */

	/*
	 * Bluetooth scan (information about a device)
	 */
	private String macAddress; // maps to android.bluetooth.device.extra.DEVICE
	private String deviceName; // maps android.bluetooth.device.extra.NAME
	private int rssi; // android.bluetooth.device.extra.RSSI
	private int mClass; // android.bluetooth.device.extra.CLASS
	private float timestamp;

	private BluetoothProbe probe;
	private final String TAG = "BluetoothSensor";
	private Activity mainUIThreadActivity;
	private Gson gson;


	private final int SCHEDULE_INTERVAL = 60;
	private final int SCHEDULE_DURATION = 30;
	private int interval;
	private int duration;



//	/**
//	 * Set the length of the interval for a re-occurring probe activity
//	 */
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "60")
//	@SimpleProperty
//	public void SetScheduleInterval(int newInterval) {
//		if (!enabledSchedule) {
//			// do nothing
//		} else {
//			interval = newInterval;
//			// register new interval for the schedule to FunfManger
//			registerDataRequest(newInterval, duration);
//		}
//
//	}

//	/**
//	 * Indicates the duration of the interval for a re-occurring probe activity
//	 */
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "30")
//	@SimpleProperty
//	public void SetScheduleDuration(int newDuration) {
//		if (!enabledSchedule) {
//			// do nothing
//		} else {
//			duration = newDuration;
//			// register new interval for the schedule to FunfManger
//			registerDataRequest(interval, newDuration);
//		}
//
//	}

	/**
	 * Indicates whether the user has specified that the sensor should listen
	 * for changes and raise the corresponding events.
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean EnabledSchedule() {
		return enabledSchedule;
	}

	/**
	 * Indicates whether the sensor should listen for proximity periodically and
	 * raise the corresponding events.
	 */
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
//	@SimpleProperty
  @SimpleFunction(description = "Enable proximity sensor periodically")
	public void EnabledSchedule(boolean enabledSchedule) {
		
		//check if Bluetooth is available
		checkBluetoothAvailabiblity();
		
		if (this.enabledSchedule != enabledSchedule)
			this.enabledSchedule = enabledSchedule;

		if (enabledSchedule) {

			// register default dataRequest with bound FunfManger
			registerDataRequest(interval, duration); // Everytime we reset the
														// schedule to default
		} else {
			// unregister the dataRequest
			unregisterDataRequest();
		}
	}
	
	private void checkBluetoothAvailabiblity(){
		//check if Bluetooth is available, if not it will raise an exception
		this.bluetoothAdapter = (BluetoothAdapter) BluetoothReflection.getBluetoothAdapter();
		
	    if (bluetoothAdapter == null) {
	        form.dispatchErrorOccurredEvent(this, "checkBluetoothAvailabiblity",
	            ErrorMessages.ERROR_BLUETOOTH_NOT_AVAILABLE);
	      }
	}

	/**
	 * Enabling discoverability
	 */
	
	@SimpleFunction(description = "Enabling discoverability. " +
			"If set, the device will be discoverable for a period of time " +
			"as specified in the variable \"seconds\". Note that if set seconds to be 0, " +
			"the device will be discoverable forever.")
	public void SetDiscoverable (int seconds) {
		// first we check whether Bluetooth is avaiable in this device
		checkBluetoothAvailabiblity(); // 
		 
		if (!this.bluetoothAdapter.enable()) {
			// for some reason the Bluetooth is closed
			Log.i(TAG, "Bluetooth is not enabled, request to turn on Bluetooth");
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			this.form.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
		this.form.startActivity(discoverableIntent);
		Log.i(TAG, "Set discoverability to:" + seconds + "seconds");

	}

	final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			IJsonObject data = (IJsonObject) msg.obj;
			Log.i(TAG, "Update component's varibles.....");
			
			macAddress = ((IJsonObject) data.get(BluetoothKeys.DEVICE)).get(
					"mAddress").getAsString();
			mClass = ((IJsonObject) data.get(BluetoothKeys.CLASS))
					.get("mClass").getAsInt();
			deviceName = data.get(BluetoothKeys.NAME).getAsString();
			rssi = data.get(BluetoothKeys.RSSI).getAsInt();
			timestamp = data.get(BluetoothKeys.TIMESTAMP).getAsLong();


			Log.i(TAG, " before call SocialProximityInfoReceived()");
			SocialProximityInfoReceived();
			Log.i(TAG, " after call SocialProximityInfoReceived()");

		}

	};

	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			Log.i(TAG, " before call SocialProximityScanComplete()");
			SocialProximityScanComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {

			/*
			 * The returned JsonElements
			 * {"android.bluetooth.device.extra.CLASS":{"mClass":3670284},
			 * "android.bluetooth.device.extra.DEVICE"
			 * :{"mAddress":"00:23:39:A4:66:21"},
			 * "android.bluetooth.device.extra.NAME":"someone's mackbook",
			 * "android.bluetooth.device.extra.RSSI"
			 * :-35,"timestamp":1339360144.799}
			 */
//			Log.i(TAG, "received Bluetooth sensor info");
//			Log.i(TAG, "TIMESTAMP: " + data.get(BluetoothKeys.TIMESTAMP));
//			Log.i(TAG, "RSSI: " + data.get(BluetoothKeys.RSSI));
//			Log.i(TAG, "NAME: " + data.get(BluetoothKeys.NAME));
//			Log.i(TAG, "CLASS: " + data.get(BluetoothKeys.CLASS));
//			Log.i(TAG, "MAC_ADDRESS: " + data.get(BluetoothKeys.DEVICE));

			Message msg = myHandler.obtainMessage();

			msg.obj = data;	

			myHandler.sendMessage(msg);

		}

	};

	private JsonElement getDataRequest(int interval, int duration) {
		// This will set the schedule to FunfManger for this probe
		JsonElement dataRequest = new JsonObject();
		/*
		 * Accepted format for probe configuration {"@type":
		 * "edu.mit.media.funf.probe.builtin.BluetoothProbe", "maxScanTime": 40,
		 * "@schedule": { "strict": true, "interval": 60, "duration": 30,
		 * "opportunistic": true } }
		 */

		((JsonObject) dataRequest).addProperty("@type",
				"edu.mit.media.funf.probe.builtin.BluetoothProbe");
		((JsonObject) dataRequest).addProperty("maxScanTime", 40);
		JsonElement scheduleObject = new JsonObject();
		((JsonObject) scheduleObject).addProperty("strict", true);
		((JsonObject) scheduleObject).addProperty("interval", interval);
		((JsonObject) scheduleObject).addProperty("duration", duration);
		((JsonObject) scheduleObject).addProperty("opportunistic", true);

		((JsonObject) dataRequest).add("@schedule", scheduleObject);


		return dataRequest;
	}

	
	public SocialProximitySensor(ComponentContainer container) {
		super(container);
		
		// Set up listeners
		form.registerForOnDestroy(this);

		mainUIThreadActivity = container.$context();

		// Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();
		config.addProperty("sensorDelay", SensorProbe.SENSOR_DELAY_NORMAL);

		probe = gson.fromJson(config, BluetoothProbe.class);
		Log.i(TAG, "Probe created");


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

  @SimpleFunction(description = "Enable social proximity sensor to run once")
  @Override
	public void Enabled(boolean enabled) {
		//check if Bluetooth is even available on this device
		checkBluetoothAvailabiblity();
	    
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
	
	
	/*
	 * (experimental) Get the app's activity name (xxx.yyy.zzz.Screen1); use this to solve the problem that
	 * some people dont' know their activity name. Who knows how to use adb cat?
	 */
	
	@SimpleFunction(description = "Get app's main activity name")
	public String GetAppName(){
		String appName = mainUIThreadActivity.getClass().getName();
		return appName;
	}
	
	
	/**
	 * Returns the latest reading of the mac address about another device in
	 * proximity
	 */
	@SimpleProperty
	public String DeviceMacAddress() {
		Log.i(TAG, "returning macAddress: " + macAddress);
		return macAddress;
	}

	/**
	 * Returns the latest reading of the name about another device in proximity
	 */
	@SimpleProperty
	public String DeviceName() {
		Log.i(TAG, "returning device name: " + deviceName);
		return deviceName;
	}

	/**
	 * Returns the latest reading of the rssi about another device in proximity
	 */
	@SimpleProperty(description = "Bluetooth Received Signal Strength Indication (RSSI) could indicate how close is the device")
	public int DeviceRSSI() {
		Log.i(TAG, "returning RSSI: " + rssi);
		return rssi;
	}

	/**
	 * Returns the latest reading of the mClass about another device in
	 * proximity
	 */
	@SimpleProperty(description = "Bluetooth Class is useful as a hint to roughly describe a device. http://developer.android.com/reference/android/bluetooth/BluetoothClass.html")
	public int DeviceClass() {
		Log.i(TAG, "returning mClass: " + mClass);
		return mClass;
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
	 * Indicates that the proximity sensor info has been received.
	 */
	@SimpleEvent
	public void SocialProximityInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "SocialProximityInfoReceived() is called");
					EventDispatcher.dispatchEvent(SocialProximitySensor.this,
							"SocialProximityInfoReceived");
				}
			});

		}
	}

	/**
	 * Indicates that one round of scan has finish
	 */
	@SimpleEvent
	public void SocialProximityScanComplete() {
		if (enabled || enabledSchedule) {
			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "SocialProximityScanComplete() is called");
					EventDispatcher.dispatchEvent(SocialProximitySensor.this,
							"SocialProximityScanComplete");
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

		dataRequest = getDataRequest(interval, duration);
		
 
		Log.i(TAG, "Data request: " + dataRequest.toString());
 
		mBoundFunfManager.requestData(listener, dataRequest);
			// Funf will overwrite with the new dataRequest configuration for
			// the same listener

	}
	
	/*
	 * This function overwrite the Bluetooth device name
	 */
	@SimpleFunction(description = "Overwrite your own device name (Bluetooth)")
	public void setOwnDeviceName(String name){
		checkBluetoothAvailabiblity();
		Log.i(TAG, "localdevicename before : "+bluetoothAdapter.getName()+" localdeviceAddress : "+bluetoothAdapter.getAddress());
        bluetoothAdapter.setName(name);
        Log.i(TAG, "localdevicename after: "+bluetoothAdapter.getName()+" localdeviceAddress : "+bluetoothAdapter.getAddress());
    }
		
		
	/*
	 * This function returns the Bluetooth device name of this device
	 */
	
	@SimpleFunction(description = "Get your own device name (Bluetooth)")
	public String getOwnDeviceName(){
		
		checkBluetoothAvailabiblity();
		return this.bluetoothAdapter.getName();
		
	}


}
