package com.google.appinventor.components.runtime.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

public class SensorDbUtil {
 /*
  * edu.mit.media.funf.probe.builtin.ActivityProbe (ActivitySensor)
  * edu.mit.media.funf.probe.builtin.BatteryProbe (BatterySensor)
  * edu.mit.media.funf.probe.builtin.CallLogProbe (CallLogHistory)
  * edu.mit.media.funf.probe.builtin.CellTowerProbe (CellTowerProbeSensor)
  * edu.mit.media.funf.probe.builtin.LightSensorProbe (LightSensor)
  * edu.mit.media.funf.probe.builtin.SimpleLocationProbe (LocationProbeSensor)
  * edu.mit.media.funf.probe.builtin.PedometerProbe (PedometerSensor)
  * edu.mit.media.funf.probe.builtin.ProximitySensorProbe (ProximitySensor)
  * edu.mit.media.funf.probe.builtin.RunningApplicationsProbe (RunningApplications)
  * edu.mit.media.funf.probe.builtin.ScreenProbe (ScreenStatus)
  * edu.mit.media.funf.probe.builtin.SmsProbe (SmsHistory)
  * edu.mit.media.funf.probe.builtin.BluetoothProbe (SocialProximitySensor)
  * edu.mit.media.funf.probe.builtin.TelephonyProbe (TelephonyInfo)
  * edu.mit.media.funf.probe.builtin.WifiProbe (WifiSensor)
  */
  //store a mapping between Funf sensors and App Inventor sensor 
  public static final Map<String, String> sensorMap;
  static {
    Map<String, String> aMap = new HashMap<String, String>();
    aMap.put("ActivitySensor", "edu.mit.media.funf.probe.builtin.ActivityProbe");
    aMap.put("BatterySensor", "edu.mit.media.funf.probe.builtin.BatteryProbe");
    aMap.put("CallLogHistory", "edu.mit.media.funf.probe.builtin.CallLogProbe");
    aMap.put("CellTowerProbeSensor", "edu.mit.media.funf.probe.builtin.CellTowerProbe");
    aMap.put("LightSensor", "edu.mit.media.funf.probe.builtin.LightSensorProbe");
    aMap.put("LocationProbeSensor", "edu.mit.media.funf.probe.builtin.SimpleLocationProbe");
    aMap.put("PedometerSensor", "edu.mit.media.funf.probe.builtin.PedometerProbe");
    aMap.put("ProximitySensor", "edu.mit.media.funf.probe.builtin.ProximitySensorProbe");
    aMap.put("RunningApplications", "edu.mit.media.funf.probe.builtin.RunningApplicationsProbe"); 
    aMap.put("ScreenStatus", "edu.mit.media.funf.probe.builtin.ScreenProbe");
    aMap.put("SmsHistory", "edu.mit.media.funf.probe.builtin.SmsProbe");
    aMap.put("SocialProximitySensor", "edu.mit.media.funf.probe.builtin.BluetoothProbe");
    aMap.put("TelephonyInfo", "edu.mit.media.funf.probe.builtin.TelephonyProbe");
    aMap.put("WifiSensor", "edu.mit.media.funf.probe.builtin.WifiProbe");
    sensorMap = Collections.unmodifiableMap(aMap);    
}
  //This helps that each package (Appinventor application to have a unique name and pass into Pipeline and FunManager) 
  public static String getPipelineName(Context context){
    return context.getPackageName() + ".__SENSOR_DB__";
  }
  
  public static String DB_NAME = "__SENSOR_DB__";

}
