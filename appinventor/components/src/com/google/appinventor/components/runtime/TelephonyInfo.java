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
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.builtin.TelephonyProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.TelephonyKeys;



/**
 * This is a wrapper class for Android's telephonyManager. 
 * For more information about telephonyManager read its documentation at
 * http://developer.android.com/reference/android/telephony/TelephonyManager.html
 * 
 * @author fuming@mit.edu (Fuming Shih)  
 * 
 */
@DesignerComponent(version = YaVersion.TELEPHONY_COMPONENT_VERSION, 
    description = "<p>A wrapper funciton of Android TelephonyManager. The components gives " +
    		"information about the telephony services on the device. Applications can use the " +
    		"methods in this class to determine telephony services and states, as well as to " +
    		"access some types of subscriber information. <p> For more info. " +
    		"check http://developer.android.com/reference/android/telephony/TelephonyManager.html", 
    category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/info.png")
    
@SimpleObject
@UsesPermissions(permissionNames = "android.Manifest.permission.READ_PHONE_STATE")
@UsesLibraries(libraries = "funf.jar")
public class TelephonyInfo extends ProbeBase{
  
  private final String TAG = "Telephony";
  protected final String TELEPHONY_PROBE = "edu.mit.media.funf.probe.builtin.TelephonyProbe";
  
  
  //telephony fields
  
  private int callState;
  private String deviceId;
  private int deviceSoftwareVersion;
  private String lineNumber; //this is sensitive data, we return a string a hashed value
  private String networkOperator;
  private String networkOperatorName;
  private String simCountryIso; 
  private String simOperator;
  private String simOperatorName;
  private String simSerialNumber; 
  private String subscriberId;
  
  private final int SCHEDULE_INTERVAL = 604800; //read telephony information every 7 days
  private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
  
  private TelephonyProbe probe;
  
  
  public TelephonyInfo(ComponentContainer container) {
    super(container);
    // TODO Auto-generated constructor stub
    
    form.registerForOnDestroy(this);
    
    mainUIThreadActivity = container.$context();
    Log.i(TAG, "Before create probe");
    gson = new GsonBuilder().registerTypeAdapterFactory(
        FunfManager.getProbeFactory(mainUIThreadActivity)).create();

    interval = SCHEDULE_INTERVAL;
    duration = SCHEDULE_DURATION;
    
    JsonObject config = new JsonObject();
    probe = gson.fromJson(config, TelephonyProbe.class);

  }
  
  private DataListener listener = new DataListener() {
    @Override
    public void onDataCompleted(IJsonObject completeProbeUri,
        JsonElement arg1) {
      // do nothing, CallLogProbe does not generate onDataCompleted event
    }

    @Override
    public void onDataReceived(IJsonObject completeProbeUri,
        IJsonObject data) {
      Log.i(TAG, "receive data of telephony info");
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
      
      callState =  data.get(TelephonyKeys.CALL_STATE).getAsInt();
      deviceId = data.get(TelephonyKeys.DEVICE_ID).getAsString();
      deviceSoftwareVersion = data.get(TelephonyKeys.DEVICE_SOFTWARE_VERSION).getAsInt();
      lineNumber = data.get(TelephonyKeys.LINE_1_NUMBER).getAsString();
      networkOperator = data.get(TelephonyKeys.NETWORK_OPERATOR).getAsString();
      networkOperatorName = data.get(TelephonyKeys.NETWORK_OPERATOR_NAME).getAsString();
      simCountryIso = data.get(TelephonyKeys.SIM_COUNTRY_ISO).getAsString();
      simOperator = data.get(TelephonyKeys.SIM_OPERATOR).getAsString();
      simSerialNumber = data.get(TelephonyKeys.SIM_SERIAL_NUMBER).getAsString();
      subscriberId = data.get(TelephonyKeys.SUBSCRIBER_ID).getAsString();

      Log.i(TAG, " before call LocationInfoReceived();");
      TelephonyInfoReceived();
      Log.i(TAG, " after call LocationInfoReceived();");
      
    }
  };
    
    /**
     * Indicates that the Telephony info has been received.
     */

    @SimpleEvent  
    public void TelephonyInfoReceived() {
      // TODO Auto-generated method stub
      // TODO Auto-generated method stub
      if (enabled || enabledSchedule) {
        
        mainUIThreadActivity.runOnUiThread(new Runnable() {
          public void run() {
            Log.i(TAG, "TelephonyInfoReceived() is called");
            EventDispatcher.dispatchEvent(TelephonyInfo.this,
                "TelephonyInfoReceived");
          }
        });
        
      }
      }


  @SimpleFunction(description = "Enable telephonyInfo sensor to run once")
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

    
  }

  @Override
  public void registerDataRequest(int interval, int duration) {
    // TODO Auto-generated method stub
    
    Log.i(TAG, "Registering data requests.");
    JsonElement dataRequest = null;

    dataRequest = getDataRequest(interval, duration, TELEPHONY_PROBE);
    Log.i(TAG, "Data request: " + dataRequest.toString());

    mBoundFunfManager.requestData(listener, dataRequest);
    
  }
  
  /**
   * Returns the latest reading of the call state of telephony
   */
  @SimpleProperty(description = "The call state of the phone.")
  public int CallState() {
    Log.i(TAG, "returning SSID: " + callState);
    return callState;
  }
  
  /**
   * Returns unique device ID, the IMEI for GSM and the MEID or ESN for CDMA phones.
   */
  @SimpleProperty(description = "Unique device ID, the IMEI for GSM " +
  		"and the MEID or ESN for CDMA phones")
  public String DeviceId() {
    Log.i(TAG, "returning deviceId: " + deviceId);
    return deviceId;
  } 
  
  
  /**
   * Returns the software version number for the device, 
   * for example, the IMEI/SV for GSM phones
   */
  @SimpleProperty(description = "The software version number for the device")
  public int deviceSoftwareVersion() {
    Log.i(TAG, "returning device software version: " + deviceSoftwareVersion);
    return deviceSoftwareVersion;
  } 
  
  /**
   * Returns the line number for the device, 
   * Due to privacy issue, only return the hashed string value 
   * for the line number. 
   */
  @SimpleProperty(description = "The hashed line number for the device")
  public String LineNumber() {
    Log.i(TAG, "returning linenumber: " + lineNumber);
    return lineNumber;
  } 
  
  /**
   * Returns the The numeric name (MCC+MNC) of current registered operator.

   */
  @SimpleProperty(description = "The numeric name (MCC+MNC) of current registered operator.")
  public String NetworkOperator() {
    Log.i(TAG, "returning network operator: " + networkOperator);
    return networkOperator;
  } 
  
  /**
   * Returns the alphabetic name of current registered operator
   * @return
   */
  @SimpleProperty(description = "The alphabetic name of current registered operator.")
  public String NetworkOperatorName() {
    Log.i(TAG, "returning networkOperatorName: " + networkOperatorName);
    return networkOperatorName;
  }
  
  /**
   * Returns the ISO country code equivalent for the SIM provider's country code.
   * @return
   */
  @SimpleProperty(description = "The ISO country code equivalent for the SIM " +
  		"provider's country code.")
  public String SimCountryIso() {
    Log.i(TAG, "returning simCountryIso: " + simCountryIso);
    return simCountryIso;
  }
  
  /**
   * Returns the MCC+MNC (mobile country code + mobile network code) of the provider
   * of the SIM.
   * @return
   */
  @SimpleProperty(description = "The MCC+MNC (mobile country code + mobile network code)" +
  		" of the provider of the SIM.")
  public String SimOperator() {
    Log.i(TAG, "returning simOperator: " + simOperator);
    return simOperator;
  }
  
  /**
   * Returns the Service Provider Name (SPN).
   * @return
   */
  @SimpleProperty(description = "The Service Provider Name (SPN).")
  public String SimOperatorName() {
    Log.i(TAG, "returning simCountryIso: " + simOperatorName);
    return simOperatorName;
  }
  
  
  /**
   * Returns the serial number of the SIM, if applicable.
   * @return
   */
  @SimpleProperty(description = "The serial number of the SIM, if applicable.")
  public String simSerialNumber() {
    Log.i(TAG, "returning simSerialNumber: " + simSerialNumber);
    return simSerialNumber;
  }

  /**
   * Returns the serial number of the SIM, if applicable.
   * @return
   */
  @SimpleProperty(description = "Tnique subscriber ID, for example, the IMSI for a GSM phone")
  public String SubscriberId() {
    Log.i(TAG, "returning subscriberId: " + subscriberId);
    return subscriberId;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

}
