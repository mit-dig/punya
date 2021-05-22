// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.util.ArrayList;
import java.util.List;

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
import edu.mit.media.funf.probe.builtin.LocationProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.probe.builtin.SensorProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;





/**
 * Record GPS location periodically
 *
 * @author fuming@mit.edu (Fuming Shih)  
 * 
 */

@DesignerComponent(version = YaVersion.LOCATIONPROBESENSOR_COMPONENT_VERSION, 
	description = "<p>Similar compoment to LocationSensor that provides location information, " +
    "including longitude, latitude, altitude (if supported by the device).  </p>"  +
	"The \"probe\" can be scheduled to periodically receive location information. " +
	"In addition, LocationProbeSensor filters the verbose location set for the most accurate " +
	"location within a max wait time (default 2 mins), ending early if it finds a location that has at most the goodEnoughAccuracy." +
	"The default max_wait_time is 2 mins and the default goodEnoughAccuracy is 80. " +
	"Useful for sparse polling of location to limit battery usage.", 
	category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/locationProbe.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_COARSE_LOCATION, "
		+ "android.permission.ACCESS_FINE_LOCATION")
@UsesLibraries(libraries = "funf.jar")
public class LocationProbeSensor extends ProbeBase{
	
	private final String TAG = "LocationProbeSensor";
	protected final String SIMPLE_LOCATION_PROBE = "edu.mit.media.funf.probe.builtin.SimpleLocationProbe";
	public static final int UNKNOWN_VALUE = 0;
	
	// Location info
	
	private double mLatitude = UNKNOWN_VALUE;
	private double mLongitude = UNKNOWN_VALUE;
	private float mAccuracy = UNKNOWN_VALUE;
	private long timestamp;
	private String mProvider = "";
	
	private SimpleLocationProbe probe;
	
	 
	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 180; //read location information every 3 minutes
	private final int SCHEDULE_DURATION = 10; //scan for 10 seconds everytime
	private final int GOOD_ENOUGHT_ACCURACY = 80;
	private boolean useGPS = true;
	private boolean useNetwork = true;
	private boolean useCache = false;
	
	private int goodEnoughAccurary;
	 

	public LocationProbeSensor(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		
		// Set up listeners
		form.registerForOnDestroy(this);
 
			mainUIThreadActivity = container.$context();
			Log.i(TAG, "Before create probe");
			gson = new GsonBuilder().registerTypeAdapterFactory(
					FunfManager.getProbeFactory(mainUIThreadActivity)).create();
//			JsonObject config = new JsonObject();
//
//			probe = gson.fromJson(config, SimpleLocationProbe.class);

			interval = SCHEDULE_INTERVAL;
			duration = SCHEDULE_DURATION;
			goodEnoughAccurary = GOOD_ENOUGHT_ACCURACY;
 
	}

	final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			IJsonObject data = (IJsonObject) msg.obj;
			Log.i(TAG, "Update component's varibles.....");
			
			mLatitude =  data.get(LocationKeys.LATITUDE).getAsDouble();
			mLongitude = data.get(LocationKeys.LONGITUDE).getAsDouble();
			mAccuracy = data.get(LocationKeys.ACCURACY).getAsFloat();
			timestamp = data.get(LocationKeys.TIMESTAMP).getAsLong();
			mProvider = data.get("mProvider").getAsString();

			Log.i(TAG, " before call LocationInfoReceived();");
			LocationInfoReceived(timestamp, mLatitude, mLongitude, mAccuracy, mProvider);
			Log.i(TAG, " after call LocationInfoReceived();");

		}


	};
	
	
	
	
	/**
	* Indicates that the Location info has been received.
	*/
	@SimpleEvent	
	public void LocationInfoReceived(final long timestamp, final double mLatitude,
                                     final double mLongitude, final float mAccuracy,
                                     final String mProvider) {

		if (enabled || enabledSchedule) {
			
			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "LocationInfoReceived() is called");
					EventDispatcher.dispatchEvent(LocationProbeSensor.this,
							"LocationInfoReceived", timestamp, mLatitude, mLongitude,
                            mAccuracy, mProvider);
				}
			});

		}	

	}
	
	
	/**
	* Indicates that the updating Location info has completed.
	*/
	@SimpleEvent	
	public void LocationUpdateComplete(){
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "LocationUpdateComplete() is called");
					EventDispatcher.dispatchEvent(LocationProbeSensor.this,
							"LocationUpdateComplete");
				}
			});
		}	
		
		
	}
	
	
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			LocationUpdateComplete();
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data");
			/* returned json format 
			 * 
			*/
			Log.i(TAG, "mLatitude:" + data.get(LocationKeys.LATITUDE).getAsDouble());
			Log.i(TAG, "mLongitude:" + data.get(LocationKeys.LONGITUDE).getAsDouble());
			Log.i(TAG, "mAccuracy:" + data.get(LocationKeys.ACCURACY).getAsFloat());
			Log.i(TAG, "mProvider:" + data.get("mProvider").getAsString());
			Log.i(TAG, "timestamp:" + data.get(LocationKeys.TIMESTAMP).getAsLong());
			
			//save data to DB is enabledSaveToDB is true
			if(enabledSaveToDB){
				
				saveToDB(completeProbeUri, data);
			}

			Message msg = myHandler.obtainMessage();

			msg.obj = data;

			myHandler.sendMessage(msg);

 
		}

	};
	
	
	

	/**
	 * Indicates whether the sensor should "run once" to listen for location information
	 * and raise the corresponding events.
	 */

  @SimpleFunction(description = "Enable location probe to run once and receive location")
	@Override
	public void Enabled(boolean enabled) {
		// TODO Auto-generated method stub
		if (this.enabled != enabled)
			this.enabled = enabled;

		if (enabled) {
			//recreate json config 
			JsonObject config = createNewConfig(this.useGPS, this.useNetwork, this.goodEnoughAccurary);
			probe = gson.fromJson(config, SimpleLocationProbe.class); 
			probe.registerListener(listener);
			Log.i(TAG, "register location listener for run-once");
			Log.i(TAG, "run-once config:" + config);
		} else {
			probe.unregisterListener(listener);
			Log.i(TAG, "unregister location run-once listener");
		}

	}

	/*
	 * recreate json config 
	 */
	private JsonObject createNewConfig(boolean useGPS, boolean useNetwork, int goodEnoughAccurary){
		JsonObject config = new JsonObject();
		
		config.addProperty("goodEnoughAccuracy", goodEnoughAccurary);
		config.addProperty("useGPS", useGPS);
		config.addProperty("useNetwork", useNetwork);
		config.addProperty("useCache", useCache);

		return config;
	}
	
	/**
	 * Set whether the location info will use the last known location without acquiring new location from 
	 * GPS or Network fix
	 * @param newVal
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
	@SimpleProperty(description = "Set whether the location info will use the last known location without" +
			" acquring a new location either through GPC or Network fix")
	public void UseCache(boolean newVal){
	  if(useCache != newVal) {
	    this.useCache = newVal;
	  }
	}
	
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean UseCache(){
	  return useCache;
	}
	
	
	/**
	 * Set the good-enough accuracy for location sensor
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "80")
	@SimpleProperty
	public void GoodEnoughAccuracy(int newVal) {
		if(goodEnoughAccurary != newVal){
			this.goodEnoughAccurary = newVal;
			 
		}

	}
	
	/**
	 *  The goodEnoughAccuracy of the location data that the sensor listens to
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public int GoodEnoughAccuracy() {
		return goodEnoughAccurary;
	}
	
	/**
	 * Set whether to use GPS or not for the location provider
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
	@SimpleProperty
	public void UseGPS(boolean newVal) {
		if(useGPS != newVal){
			this.useGPS = newVal;
			 
		}

	}
	
	
	/**
	 *  Indicate whether the locationProbe uses GPS or not  
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean UseGPS() {
		return useGPS;
	}
	
	
	/**
	 * Set whether to use Network or not for the location provider
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
	@SimpleProperty
	public void UseNetwork(boolean newVal) {
		if(useNetwork != newVal){
			this.useNetwork = newVal;
			 
		}

	}
	
	
	/**
	 *  Indicate whether the locationProbe uses GPS or not  
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean UseNetwork() {
		return useNetwork;
	}
	

	
	@Override
	public void registerDataRequest(int interval, int duration) {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "Registering location data requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, SIMPLE_LOCATION_PROBE);
		((JsonObject)dataRequest).addProperty("goodEnoughAccurary", goodEnoughAccurary);
		((JsonObject)dataRequest).addProperty("useGPS", useGPS);
		((JsonObject)dataRequest).addProperty("useNetwork", useNetwork);
		((JsonObject)dataRequest).addProperty("useCache", useCache);
		 
		Log.i(TAG, "Location Data request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);
		
	}
	
	
	
	@Override
	public void unregisterDataRequest() {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "Unregistering location data requests.");
		//mBoundFunfManager.stopForeground(true);
		mBoundFunfManager.unrequestAllData2(listener);

		Log.i(TAG, "After Unregistering location data requests.");

		
	}
	
//	  /**
//	   * The most recent available accuracy value.  If no value is available,
//	   * 0 will be returned.
//	   */
//	  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
//	  public double Accuracy() {
//	    return mAccuracy;
//	  }
//
//	  /**
//	   * The most recent available longitude value.  If no value is available,
//	   * 0 will be returned.
//	   */
//	  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
//	  public double Longitude() {
//	    return mLongitude;
//	  }
//
//	  /**
//	   * The most recently available latitude value.  If no value is available,
//	   * 0 will be returned.
//	   */
//	  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
//	  public double Latitude() {
//	      return mLatitude;
//	  }
//
//	  /**
//	   * The type of the provider.  If no value is available,
//	   * "" will be returned.
//	   */
//	  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
//	  public String Provider() {
//	      return mProvider;
//	  }
//
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
	public int DefaultInterval(){return interval;}

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "180")
	@SimpleProperty
	public void DefaultInterval(int defaultInterval) { this.interval = interval; }

	
	/*
	 * Returns the default duration of each scan for this probe
	 */
	@SimpleProperty(description = "The default duration (in seconds) of each scan for this probe")
	public int DefaultDuration(){ return duration;}

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT)
	@SimpleProperty
	public void DefaultDuration(int defaultDuration) { this.duration = duration; }
	
}