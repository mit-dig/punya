// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

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
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInternal.TextBasedSmsColumns;
import edu.mit.media.funf.probe.builtin.SmsProbe;
import edu.mit.media.funf.time.TimeUnit;

@DesignerComponent(version = YaVersion.CALLLOGHISTORY_COMPONENT_VERSION, 
		description = "Messages sent and received by this device using SMS. Sensitive data is hashed for user privacy." +
				"Could specifiy \"afterDate\" parameter to only read sms information after that date." , 
		category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/smsProbe.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.READ_SMS")
@UsesLibraries(libraries = "funf.jar")
public class SmsHistory extends ProbeBase{
	//android.permission.READ_SMS

	private final String TAG = "SmsProbe";
	private SmsProbe probe;
	private final String SMS_PROBE = "edu.mit.media.funf.probe.builtin.SmsProbe";

	//fields in the returned json
	private String address; 		//phone number associated with message 
	private String body; 			 		
 	private long date; 				//The date that the message was sent or received
	private String type; 			// http://stackoverflow.com/questions/8447735/android-sms-type-constants
	private boolean read; 			//whether the sms message is read or not


	//default settings for schedule 
	private final int SCHEDULE_INTERVAL = 43200; //read calllogs every 43200 seconds (every 12 hours)
	private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//parameters for this probe 
	private long afterDate = 0; 	// Only read smsLog that are after this date (in seconds since epoch). Without this, the probe will return all calllogs

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
			 * {"address":"{\"ONE_WAY_HASH\":\"4ce667aa063d4ecf712bc97a7142c2cb0118d5c9\"}",
			 *   "body":"{\"ONE_WAY_HASH\":\"4a111bc11f23bd65316b257ea806d8ce0b51dbb2\"}",
			 *   "date":1335467311983,"locked":false,"person":"{\"ONE_WAY_HASH\":\"\"}",
			 *   "protocol":0,
			 *   "read":true,
			 *   "reply_path_present":false,
			 *   "status":-1,
			 *   "subject":"{\"ONE_WAY_HASH\":\"\"}",
			 *   "thread_id":25,
			 *   "timestamp":1335467311.983,
			 *   "type":2}
			 */

			 /*
			  * Some notes for SMS:type (http://stackoverflow.com/questions/8447735/android-sms-type-constants)
			  * MESSAGE_TYPE_ALL    = 0;
			  * MESSAGE_TYPE_INBOX  = 1;
			  * MESSAGE_TYPE_SENT   = 2;
			  * MESSAGE_TYPE_DRAFT  = 3;
			  * MESSAGE_TYPE_OUTBOX = 4;
			  * MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
			  * MESSAGE_TYPE_QUEUED = 6; // for messages to send later
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


			String hashedAddress = data.get(ProbeKeys.SmsKeys.ADDRESS).getAsString();
			String hashedBody = data.get(ProbeKeys.SmsKeys.BODY).getAsString();
 

			JsonObject addrJson = jsonParser.parse(hashedAddress).getAsJsonObject();
			JsonObject bodyson = jsonParser.parse(hashedBody).getAsJsonObject();



			address = addrJson.get("ONE_WAY_HASH").getAsString();
			body = bodyson.get("ONE_WAY_HASH").getAsString();
			date = data.get(ProbeKeys.SmsKeys.DATE).getAsLong();
			read = data.get(ProbeKeys.SmsKeys.READ).getAsBoolean();

			// get the string representation of sms type
			type = getTypeName(data.get(ProbeKeys.SmsKeys.TYPE).getAsInt());

			Log.i(TAG, "DEBUG TYPE: " + type);


			SmsInfoReceived();
 

		}

	};	


	private String getTypeName(int typeConst) {
		String type = "";
		switch (typeConst) {
		case TextBasedSmsColumns.MESSAGE_TYPE_INBOX:
			type = "INBOX";
			break;
		case TextBasedSmsColumns.MESSAGE_TYPE_DRAFT:
			type = "DRAFT";
			break;
		case TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX:
			type = "OUTBOX";
			break;
		case TextBasedSmsColumns.MESSAGE_TYPE_FAILED:
			type = "FAILED";
			break;
		case TextBasedSmsColumns.MESSAGE_TYPE_SENT:
			type = "SENT";
			break;
		case TextBasedSmsColumns.MESSAGE_TYPE_QUEUED:
			type = "QUEUED";
			break;

		}

		return type;
	}


	public SmsHistory(ComponentContainer container) {
		super(container);
		// init sms types hashmap
 
		form.registerForOnDestroy(this);
		context =  container.$context();
		mainUIThreadActivity = container.$context();
		Log.i(TAG, "Before create probe");
		gson = new GsonBuilder().registerTypeAdapterFactory(
				FunfManager.getProbeFactory(mainUIThreadActivity)).create();
		JsonObject config = new JsonObject();

		probe = gson.fromJson(config, SmsProbe.class);

		interval = SCHEDULE_INTERVAL;
		duration = SCHEDULE_DURATION;

	}

	/**
	 * Specify the date after which the sms message occurred
	 * The format should be "YYYY-MM-DD HH:mm:ss" (see http://docs.oracle.com/javase /1.5.0/docs/api/java/text/SimpleDateFormat.html) 
	 * for example "2012-06-01 20:00:00" will read all SMS after June 1, 8pm
	 * Empty value will return all SMS messages
	 */
	@SimpleFunction(description = "Specify the date after which the SMS messages occurred. The formate should be \"YYYY-MM-DD HH:mm:ss\"")
	@SimpleProperty
	public void AfterDate(String datePoint) {

		Date date = null;

		if ("".equals(datePoint)) {
			; //do nothing
		} else {
			try {
				date = sdf.parse(datePoint);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				form.dispatchErrorOccurredEvent(SmsHistory.this, "AfterDate",
		                  ErrorMessages.ERROR_DATE_FORMAT, e.getMessage());
			}
			long timeInMillisSinceEpoch = date.getTime();
			this.afterDate = TimeUnit.MILLISECONDS.toSeconds(timeInMillisSinceEpoch);
 
			Log.i(TAG, "afterDate: " + DateFormat.getDateFormat(context).format(afterDate * 1000) );

		}


	}


  @SimpleFunction(description = "Enable sms history sensor to run once")
  @Override
	public void Enabled(boolean enabled) {
		// TODO Auto-generated method stub
		JsonObject newConfig = null;
		if (this.enabled != enabled)
			this.enabled = enabled;

		if (enabled) {

			if (afterDate != 0) { // recreate json config
				newConfig = new JsonObject();

				newConfig.addProperty("afterDate", this.afterDate);
				probe = gson.fromJson(newConfig, SmsProbe.class);
			}

			probe.registerListener(listener);

			Log.i(TAG, "run-once config for SMS:" + newConfig);
		} else {
			probe.unregisterListener(listener);

		}

	}

	@Override
	public void unregisterDataRequest() {
		Log.i(TAG, "Unregistering sms data requests.");
		//mBoundFunfManager.stopForeground(true);
		mBoundFunfManager.unrequestAllData2(listener);

	}

	@Override
	public void registerDataRequest(int interval, int duration) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Registering sms requests.");
		JsonElement dataRequest = null;

		dataRequest = getDataRequest(interval, duration, SMS_PROBE);

		if (afterDate != 0)
			((JsonObject) dataRequest).addProperty("afterDate", afterDate);

		Log.i(TAG, "CallLog request: " + dataRequest.toString());

		mBoundFunfManager.requestData(listener, dataRequest);
	}

	/**
	 * Indicates that the calllog info has been received.
	 */
	@SimpleEvent
	public void SmsInfoReceived() {
		if (enabled || enabledSchedule) {

			mainUIThreadActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.i(TAG, "SmsInfoReceived() is called");
					EventDispatcher.dispatchEvent(SmsHistory.this,
							"SmsInfoReceived");
				}
			});

		}

	}

	/**
	 * The phone number associated with message. (hashed for privacy reason)
	 */
	@SimpleProperty(description = "The  address (phone number) associated with message, hashed for privacy reason. ")
	public String Address(){
		Log.i(TAG, "returning address of the message: " + address);
		return address;	
	}


	/**
	 * The body of the message. (hashed for privacy reason)
	 */
	@SimpleProperty(description = "The  body of the message, hashed for privacy reason. ")
	public String Body(){
		Log.i(TAG, "returning address of the message: " + address);
		return body;	
	}


	/**
	 * The date the sms was sent or received, in milliseconds since the epoch
	 */
	@SimpleProperty(description = "The date the sms was sent or received, in milliseconds since the epoch")
	public long	Date(){
		Log.i(TAG, "returning date " + date);
		return date;	
	}


	/**
	 * The type of the message 
	 */
	@SimpleProperty(description = "The type of the message " )
	public String MessageType(){
		Log.i(TAG, "returning message: " + type);
		return type;	
	}

	/**
	 * Indicate whether the message is read or not
	 */
	@SimpleProperty(description = "Indicate whether the message is read or not " )
	public boolean Read(){
		Log.i(TAG, "returning message: " + read);
		return read;	
	}

	/*
	 * Returns the default interval between each scan for this probe
	 */
	@SimpleProperty(description = "The default interval (in seconds) between each scan for this probe")
	public float DefaultInterval(){

		return SCHEDULE_INTERVAL;
	}


}
