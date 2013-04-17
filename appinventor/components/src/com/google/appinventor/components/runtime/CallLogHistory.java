// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.text.format.DateFormat;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.CallLogProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.TimeUnit;

@DesignerComponent(version = YaVersion.CALLLOGHISTORY_COMPONENT_VERSION, 
		description = "Return information of recent calls. A wrapper around Android.Calllog.Calls " +
				"(see http://developer.android.com/reference/android/provider/CallLog.Calls.html). " +
				"Could specifiy \"afterDate\" parameter to only read calllog information after that date. " +
				"Some of the returning fields (name, number, numberType, numberLabel) are hashed " +
				"for privacy reasons" , 
		category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/calllogProbe.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.READ_CONTACTS")
@UsesLibraries(libraries = "funf.jar")
public class CallLogHistory extends ProbeBase{
	
	private final String TAG = "CallLogProbe";
	private CallLogProbe probe;
	private final String CALLLOG_PROBE = "edu.mit.media.funf.probe.builtin.CallLogProbe";
	
	//fields in the returned json
	private String name; 		//name (one way hashed) associated with the phone number
	private String number; 		//The phone number (one way hashed) as the user entered it.
	private String numberLabel; //The cached number label, for a custom number type, associated with the phone number, if it exists.
	private String numberType;	//The cached number type (Home, Work, etc) associated with the phone number, if it exists.
	private long date; 			//The date the call occurred, in milliseconds since the epoch
	private int duration; 		//The duration of the call in seconds
	private String type; 		//The type of the call (incoming(1), outgoing(2) or missed(3)).
	
	//parameters for this probe 
	private long afterDate = 0; 	// Only read calllogs that are after this date (in seconds since epoch). Without this, the probe will return all calllogs
	

	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 86400; //read calllogs every 86400 seconds (every 24 hours)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//util
	JsonParser jsonParser = new JsonParser();
	Context context;
	
	
	private DataListener listener = new DataListener() {
		@Override
		public void onDataCompleted(IJsonObject completeProbeUri,
				JsonElement arg1) {
			// do nothing, CallLogProbe does not generate onDataCompleted event
		}

		@Override
		public void onDataReceived(IJsonObject completeProbeUri,
				IJsonObject data) {
			Log.i(TAG, "receive data of calllog");
			/*
			 * TODO: bud in the json format of the returning name/number
			 * returned json format 
			 * { "_id":975,"date":1338751506200,
			 *   "duration":2, 
			 *   "name": "{\"ONE_WAY_HASH\":\"80d2125f300a1ad8baafd5ef295a7f544bf25409\"}", 
			 *   "number":"{\"ONE_WAY_HASH\":\"1662c65b0f53de6ce19c1f3138b9b5edc3d27630\"}",
			 *   "numberlabel":"{\"ONE_WAY_HASH\":\"\"}", 
			 *   "numbertype": "{\"ONE_WAY_HASH\":\"da4b9237bacccdf19c0760cab7aec4a8359010b0\"}",
			 *   "timestamp":1338751506.2,
			 *   "type":1}
			 */
			// debug
			
			Log.i(TAG, "DATA: " + data.toString());
			
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
			
			
			String hashedName = data.get(ProbeKeys.CallLogKeys.NAME).getAsString();
			String hashedNumber = data.get(ProbeKeys.CallLogKeys.NUMBER).getAsString();
			String hashedNnumberType = data.get(ProbeKeys.CallLogKeys.NUMBER_TYPE).getAsString();
			String haseddNumberLabel = data.get(ProbeKeys.CallLogKeys.NUMBER_LABEL).getAsString();
			
			JsonObject nameJson = jsonParser.parse(hashedName).getAsJsonObject();
			JsonObject numberJson = jsonParser.parse(hashedNumber).getAsJsonObject();
			JsonObject numberTypeJson = jsonParser.parse(hashedNnumberType).getAsJsonObject();
			JsonObject numberLabelJson = jsonParser.parse(haseddNumberLabel).getAsJsonObject();
			
			
			name = nameJson.get("ONE_WAY_HASH").getAsString();
			number = numberJson.get("ONE_WAY_HASH").getAsString();
			numberType = numberTypeJson.get("ONE_WAY_HASH").getAsString();
			numberLabel = numberLabelJson.get("ONE_WAY_HASH").getAsString();
			
			date = data.get(ProbeKeys.CallLogKeys.DATE).getAsLong();
			duration = data.get(ProbeKeys.CallLogKeys.DURATION).getAsInt();
			
			// get the string representation of call type 
			type =  getTypeName(data.get(ProbeKeys.CallLogKeys.TYPE).getAsInt());
		
  
			CalllogsInfoReceived();
 

		}

	};	
	
	private String getTypeName(int typeConst) {
		String type = "";
		switch (typeConst) {
		case CallLog.Calls.INCOMING_TYPE:
			type = "INCOMING";
			break;
		case CallLog.Calls.OUTGOING_TYPE:
			type = "OUTGOING";
			break;
		case CallLog.Calls.MISSED_TYPE:
			type = "MISSED";

		}

		return type;
	}
	
	
	
	public CallLogHistory(ComponentContainer container) {
		super(container);
		// TODO Auto-generated constructor stub
		
		form.registerForOnDestroy(this);
		context =  container.$context();
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, CallLogProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;

		
	}
	
	/**
	 * Specify the date after which the callogs occurred
	 * The formate should be "YYYY-MM-DD HH:mm:ss" (see http://docs.oracle.com/javase /1.5.0/docs/api/java/text/SimpleDateFormat.html) 
	 * for example "2012-06-01 20:00:00" will calllogs after June 1, 8pm
	 * Empty value will return all calllogs
	 */
	@SimpleFunction(description = "Specify the date after which the callogs occurred. The formate should be \"YYYY-MM-DD HH:mm:ss\"")
	public void AfterDate(String datePoint) {
		
		Date date = null;
		
		if ("".equals(datePoint)) {
			; //do nothing
		} else {
			try {
				date = sdf.parse(datePoint);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				form.dispatchErrorOccurredEvent(CallLogHistory.this, "AfterDate",
		                  ErrorMessages.ERROR_DATE_FORMAT, e.getMessage());
			}
			long timeInMillisSinceEpoch = date.getTime();
			this.afterDate = TimeUnit.MILLISECONDS.toSeconds(timeInMillisSinceEpoch);
 
			Log.i(TAG, "afterDate: " + DateFormat.getDateFormat(context).format(afterDate * 1000));
			 
		}
		
		
	}
	
//	/**
//	 * Set the range of calllog history want to retrieve, starting from n days ago until now 
//	 * @param nDaysAgo
//	 *        Set to retrieve only callogs from N days ago  
//	 *  TODO: remove from this component, because it's confusing when set this value in a reoccurring event for probing. 
//	 */
//
//	@SimpleFunction(description = "Set to retrieve only callogs from N days ago with the specified N")
//	public void SetCallLogsRange(int nDaysAgo) {
//		long totalSeconds = nDaysAgo * 86400;
//		long currentSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
//		
//		this.afterDate = currentSeconds - totalSeconds;
//		//debug 
//		Log.i(TAG, "afterDate: " + DateFormat.getDateFormat(context).format(afterDate * 1000));
//
//	}
	

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
	@SimpleProperty
	public void Enabled(boolean enabled) {
		// TODO Auto-generated method stub
		JsonObject newConfig = null;
		if (this.enabled != enabled)
			this.enabled = enabled;

		if (enabled) {

			if (afterDate != 0) { // recreate json config
				newConfig = new JsonObject();

				newConfig.addProperty("afterDate", this.afterDate);
				probe = gson.fromJson(newConfig, CallLogProbe.class);
			}

			probe.registerListener(listener);

			Log.i(TAG, "run-once config:" + newConfig);
		} else {
			probe.unregisterListener(listener);

		}

	}

	@Override
	public void unregisterDataRequest() {
		Log.i(TAG, "Unregistering calllog data requests.");
		//mBoundFunfManager.stopForeground(true);
		mBoundFunfManager.unrequestAllData2(listener);
		
	}

	@Override
	public void registerDataRequest(int interval, int duration) {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "Registering calllogs requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, CALLLOG_PROBE);

		if (afterDate != 0)
			((JsonObject) dataRequest).addProperty("afterDate", afterDate);

		Log.i(TAG, "CallLog request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);
		
		
		
		
		
	}

	/**
	 * Indicates that the calllog info has been received.
	 */
	@SimpleEvent
	public void CalllogsInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "CalllogsInfoReceived() is called");
					EventDispatcher.dispatchEvent(CallLogHistory.this,
							"CalllogsInfoReceived");
				}
			});

		}

	}
	
	
	/**
	 * Returns hashedName associated with the phone number
	 */
	@SimpleProperty(description = "Name (hashed format) associated with the phone number")
	public String HashedName(){
		Log.i(TAG, "returning hashedName: " + name);
		return name;	
	}
	
	/**
	 * The phone number (hashed) as the user entered it
	 */
	@SimpleProperty(description = "The phone number (hashed format) as the user entered it.")
	public String HashedNumber(){
		Log.i(TAG, "returning hashedNumber: " + number);
		return number;	
	}
	
	/**
	 * The duration of the call in seconds
	 */
	@SimpleProperty(description = "The duration of the call in seconds")
	public int Duration(){
		Log.i(TAG, "returning duration " + duration);
		return duration;	
	}
	
	/**
	 * The date the call occured, in milliseconds since the epoch
	 */
	@SimpleProperty(description = "The date the call occured, in milliseconds since the epoch")
	public long	Date(){
		Log.i(TAG, "returning date " + date);
		return date;	
	}
	
	/**
	 * The cached number label, for a custom number type, 
	 * associated with the phone number, if it exists.
	 */
	@SimpleProperty(description = "The number label (hashed format), for a custom number type, " +
			"associated with the phone number, if it exists.")
	public String NumberLabel(){
		Log.i(TAG, "returning numberLabel: " + numberLabel);
		return numberLabel;	
	}
	
	/**
	 * The cached number type (Home, Work, etc) associated 
	 * with the phone number, if it exists.
	 */
	@SimpleProperty(description = "The number type (Home, Work, etc) associated with the phone number, " +
			"but in hashed format")
	public String NumberType(){
		Log.i(TAG, "returning numberLabel: " + numberType);
		return numberType;	
	}
	
	/**
	 * The type of the call (incoming = "1", outgoing = "2" or missed = "3").
	 */
	@SimpleProperty(description = "The type of the call " +
			"(incoming = \"1\", outgoing = \"2\" or missed = \"3\")")
	public String CallType(){
		Log.i(TAG, "returning callType: " + type);
		return type;	
	}
	
	/*
	 * Returns the default interval between each scan for this probe
	 */
	@SimpleProperty(description = "The default interval (in seconds) between each scan for this probe")
	public float DefaultInterval(){
		
		return SCHEDULE_INTERVAL;
	}
	
	
	
}
