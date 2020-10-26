// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.R;
import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.SensorDbUtil;

//import com.google.appinventor.server.flags.Flag;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.gson.JsonPrimitive;

 
import edu.mit.media.funf.FunfManager;

import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;

import edu.mit.media.funf.time.DecimalTimeUnit;


@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, "
  + "android.permission.ACCESS_NETWORK_STATE, "
  + "android.permission.WAKE_LOCK, "
  + "android.permission.READ_LOGS, "
  + "android.permission.VIBRATE, "
  + "android.permission.INTERNET")
@UsesLibraries(libraries = "funf.jar")
public abstract class ProbeBase extends AndroidNonvisibleComponent implements
//SensorComponent, OnDestroyListener, Pipeline{
SensorComponent, OnDestroyListener{
  protected boolean enabled = false; // run once
  protected boolean enabledSchedule = false; // run periodically
  boolean privacySafe = false; // by default sensitive values clear text

  private final String TAG = "ProbeBase";
  /*
   * Binding to FunfMananger service
   */

  protected FunfManager mBoundFunfManager = null;
  protected boolean mIsBound = false;
  //protected List<JsonElement> dataRequests = new ArrayList<JsonElement>();
  
  protected Activity mainUIThreadActivity;
  protected Gson gson;

  protected int interval;
  protected int duration;
  
  /*
   * Notification
   */
  private Notification notification;
  private PendingIntent mContentIntent;
  private NotificationManager mNM;
  private static final int PROBE_NOTIFICATION_MIN_ID = 8888;
  private static int probeCounter = 0;
  private final int PROBE_NOTIFICATION_ID;

  
  public static final String PASSWORD_KEY = "PASSWORD";
  
  // save to db
  protected boolean enabledSaveToDB = false; //local for each probe
  private String exportPath;
  private String exportFormat;
  
  
  
  private Calendar calendar = Calendar.getInstance(Locale.getDefault());
  private BigDecimal localOffsetSeconds = BigDecimal.valueOf(
      calendar.get(Calendar.ZONE_OFFSET)
          + calendar.get(Calendar.DST_OFFSET), DecimalTimeUnit.MILLI);
  
  
  // try local binding to FunfManager
  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      // This is called when the connection with the service has been
      // established, giving us the service object we can use to
      // interact with the service. Because we have bound to a explicit
      // service that we know is running in our own process, we can
      // cast its IBinder to a concrete class and directly access it.
      mBoundFunfManager = ((FunfManager.LocalBinder) service)
          .getManager();
      
//      registerSelfToFunfManager(); 

      Log.i(TAG, "Bound to FunfManager");

    }

    public void onServiceDisconnected(ComponentName className) {
      // This is called when the connection with the service has been
      // unexpectedly disconnected -- that is, its process crashed.
      // Because it is running in our same process, we should never
      // see this happen.
      mBoundFunfManager = null;

      Log.i(TAG, "Unbind FunfManager");

    }
  };
  
  private static int getNotificationID() {
    probeCounter += 1;
    return PROBE_NOTIFICATION_MIN_ID + probeCounter;

  }
  
  protected ProbeBase(ComponentContainer container) {
    super(container.$form());
    // TODO Auto-generated constructor stub


    // Set up listeners
    form.registerForOnDestroy(this);

    mainUIThreadActivity = container.$context();


    // start FunfManger
    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the
    // schedule)
    doBindService();


    // assign an unique notification id for each probe to use
    PROBE_NOTIFICATION_ID = getNotificationID();

    // get Notification Manager
    String ns = Context.NOTIFICATION_SERVICE;
    mNM = (NotificationManager) mainUIThreadActivity.getSystemService(ns);
    Log.i(TAG, "created notification manager");
    exportPath =  new File(Environment.getExternalStorageDirectory(), form.getPackageName()) + 
		File.separator + "export";
    exportFormat = NameValueDatabaseService.EXPORT_CSV; // set the exporting format as csv by default


  }
  
  
  void doBindService() {
    // Establish a connection with the service. We use an explicit
    // class name because we want a specific service implementation that
    // we know will be running in our own process (and thus won't be
    // supporting component replacement by other applications).
    mainUIThreadActivity.bindService(new Intent(mainUIThreadActivity,
        FunfManager.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
    Log.i(TAG,
        "FunfManager is bound, and now we could have register dataRequests");

  }

  void doUnbindService() {
    if (mIsBound) {
      // first unrequestData
      unregisterDataRequest();
      // then unregister Pipeline action 
//      unregisterPipelineActions();
      // Detach our existing connection.
      mainUIThreadActivity.unbindService(mConnection);
      mIsBound = false;
    }
  }


  
  protected JsonElement getDataRequest(int interval, int duration, String probeName) {
    // This will set the schedule to FunfManger for this probe
    //List<JsonElement> dataRequests = new ArrayList<JsonElement>();
    /*
     * Accepted format for probe configuration {"@type":
     * "edu.mit.media.funf.probe.builtin.BluetoothProbe", "maxScanTime": 40,
     * "@schedule": { "strict": true, "interval": 60, "duration": 30,
     * "opportunistic": true } }
     */
    
    JsonElement dataRequest = new JsonObject();
    ((JsonObject) dataRequest).addProperty("@type",
        probeName);
    //((JsonObject) dataRequest).addProperty("maxScanTime", 40);
    JsonElement scheduleObject = new JsonObject();
    ((JsonObject) scheduleObject).addProperty("strict", true);
    ((JsonObject) scheduleObject).addProperty("interval", interval);
    ((JsonObject) scheduleObject).addProperty("duration", duration);
    ((JsonObject) scheduleObject).addProperty("opportunistic", true);

    ((JsonObject) dataRequest).add("@schedule", scheduleObject);

    //dataRequests.add(dataRequest);

    return dataRequest;
  }


  /**
   * Set whether the returned values will be privacy safe (hashed) or not
   */
  
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
  @SimpleProperty(description = "If set to True, then sensitive values will be hashed. Note that for some" +
  		" application, only hashed values will be good enough. It's not necessary to read clear " +
  		"text of user information which can cause some privacy issues.")
  public void HideSensitiveData(boolean hideSensitive){
    
    this.privacySafe = hideSensitive;
  
  }
  
  /**
   * Indicates whether the returned values will be privacy safe (hashed) or not
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public boolean HideSensitiveData() {
    return privacySafe;
  }
  
  /**
   * Set the length of the interval for a re-occurring probe activity. 
   * Setting this value triggers the probe using the new schedule 
   * 
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "60")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void SetScheduleInterval(int newInterval) {
    
    this.interval = newInterval;

  }


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

  @SimpleFunction(description = "Enables the sensor periodically and raise the corresponding events")
  public void EnabledSchedule(boolean enabledSchedule) {

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
  
  /**
   * Indicates whether the user has specified that the sensor should listen
   * for changes and raise the corresponding events.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public boolean Enabled() {
    return enabled;
  }

  @Override
  public void onDestroy() {
    // remember to unbind
    doUnbindService();
  }
  
  
  // This is used as the database name for all the sensor components that 
  // extends probebase
  public static final String PROBE_BASE_NAME = SensorDbUtil.DB_NAME;  
  
  public static SharedPreferences getSystemPrefs(Context context) {
    // TODO Auto-generated method stub
    return context.getSharedPreferences(ProbeBase.class.getName()
        + "_system", android.content.Context.MODE_PRIVATE);
  }
  
  
    
  protected void saveToDB(IJsonObject completeProbeUri, IJsonObject data){
    
    Log.i(TAG, "Writing data: " + completeProbeUri + ": " + data.toString());
    final JsonObject dataObject = data.getAsJsonObject();
    dataObject.add("probe",
        completeProbeUri.get(RuntimeTypeAdapterFactory.TYPE));
    dataObject.add("timezoneOffset", new JsonPrimitive(localOffsetSeconds)); // nice
                                          // move
    final long timestamp = data.get(BaseProbeKeys.TIMESTAMP).getAsLong();
    final String probeName = completeProbeUri.get("@type").getAsString();
    
    
    Bundle b = new Bundle();
    b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, PROBE_BASE_NAME);
    b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
    b.putString(NameValueDatabaseService.NAME_KEY, probeName);
    b.putString(NameValueDatabaseService.VALUE_KEY, dataObject.toString());
    Intent i = new Intent(mBoundFunfManager, NameValueDatabaseService.class);
    i.setAction(DatabaseService.ACTION_RECORD);
    i.putExtras(b);
    mBoundFunfManager.startService(i);

  }
  
  
  /**
   * Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
   * csv file(s) or JSON file(s). Each type of sensor data in the database
   * will be export it as one file.
   * The export path is under SDcard/packageName/export/
   */
  @SimpleFunction(description = "Export all sensor data as CSV files")
  public void ExportSensorDB( ){
    Log.i(TAG, "Exporting DB as CSV files");
    Log.i(TAG, "exporting data...at: " + System.currentTimeMillis());

    Bundle b = new Bundle();
    b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, SensorDbUtil.DB_NAME);
    b.putString(NameValueDatabaseService.EXPORT_KEY, this.exportFormat);
    Intent i = new Intent(mBoundFunfManager, NameValueDatabaseService.class);
    i.setAction(DatabaseService.ACTION_EXPORT);
    i.putExtras(b);
    mBoundFunfManager.startService(i);
  }


  @SimpleFunction(description = "Get the path of the foler to which " +
  		"sensor db are exported")
  public String ExportFolderPath() {
	// the real export path is exportPath + "/" + exportformat
	Log.i(TAG, "exportpath:" + this.exportPath + File.separator
		+ this.exportFormat);
	return this.exportPath + File.separator + this.exportFormat;

  }

  
  
  /**
   * Return the name of the database in which the sensed data is stored 
   * (actually there's only one db for sensed data (SensorData)
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String LocalDBName(){
    return ProbeBase.PROBE_BASE_NAME;
    
  }
  
  
  
  /**
   * Set to indicate whether the returned sensor data should be saved to db automatically
   * 
   * @param enabledSaveToDB
   *            if true, save all data sensed from this sensor to DB automatically
   */

  @SimpleProperty(description = "Save all sensed data from Wifi sensor probe to db automatically")
  public void EnableSaveToDB(boolean enabledSaveToDB) {
    if (this.enabledSaveToDB != enabledSaveToDB) 
      this.enabledSaveToDB = enabledSaveToDB;

  }
  
  /**
   * Indicates whether the sensed data are saved to DB automatically 
   * 
   */
 
  public boolean EnabledSaveToDB() {
    return enabledSaveToDB;
  }
  

  /*
   * Add notification with some message and the app (actually it's app.Screen1) it wants to activate
   * @param title 
   * @param text
   * @param enabledSound
   * @param enabledVibrate
   * @param appName
   * @param extraKey 
   * @param extraVal 
   * 
   */
  
  @SimpleFunction(description = "Create a notication with message to wake up " +
      "another activity when tap on the notification")
  public void CreateNotification(String title, String text, boolean enabledSound, 
      boolean enabledVibrate, String packageName, String className, String extraKey, String extraVal) 
      throws ClassNotFoundException {

    
    Intent activityToLaunch = new Intent(Intent.ACTION_MAIN);

    Log.i(TAG, "packageName: " + packageName);
    Log.i(TAG, "className: " + className);

    // for local AI instance, all classes are under the package
    // "appinventor.ai_test"
    // but for those runs on Google AppSpot(AppEngine), the package name will be
    // "appinventor.ai_GoogleAccountUserName"
    // e.g. pakageName = appinventor.ai_HomerSimpson.HelloPurr
    // && className = appinventor.ai_HomerSimpson.HelloPurr.Screen1

    ComponentName component = new ComponentName(packageName, className);
    activityToLaunch.setComponent(component);
    activityToLaunch.putExtra(extraKey, extraVal);

    
    Log.i(TAG, "we found the class for intent to send into notificaiton");

    activityToLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

    mContentIntent = PendingIntent.getActivity(mainUIThreadActivity, 0, activityToLaunch, 0);

    Long currentTimeMillis = System.currentTimeMillis();
    notification = new Notification(R.drawable.stat_notify_chat,
        "Activate Notification!", currentTimeMillis);

    Log.i(TAG, "After creating notification");
    notification.contentIntent = mContentIntent;
    notification.flags = Notification.FLAG_AUTO_CANCEL;

    // reset the notification
    notification.defaults = 0;
    
    if(enabledSound)
      notification.defaults |= Notification.DEFAULT_SOUND;
    
    if(enabledVibrate)
      notification.defaults |= Notification.DEFAULT_VIBRATE;

    Log.i(TAG, "after updated notification contents");
    mNM.notify(PROBE_NOTIFICATION_ID, notification);
    Log.i(TAG, "notified");
  
  }
  
  
  public abstract void Enabled(boolean enabled); 
  public abstract void unregisterDataRequest();
  public abstract void registerDataRequest(int interval, int duration); 

}
