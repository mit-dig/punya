// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.DBTrustUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.HttpsUploadService;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.security.Base64Coder;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.time.DecimalTimeUnit;

/*
 * DBTrust acts like a Funf pipe
 */
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, "
	+ "android.permission.ACCESS_NETWORK_STATE, "
	+ "android.permission.WAKE_LOCK, "
	+ "android.permission.READ_LOGS, "
	+ "android.permission.INTERNET")
public class DBTrust extends AndroidNonvisibleComponent implements
OnDestroyListener, Pipeline{
  
  /*
   * Binding to FunfMananger service
   */

  protected FunfManager mBoundFunfManager = null;
  protected boolean mIsBound = false;
  
  private final String TAG = "DBTrust";
  public static final String PIPE_NAME = "DBTrust"; // this is used as the dtatbase name
  
  protected static Activity mainUIThreadActivity;
 
 
  
  private static final long UPLOAD_PERIOD = 7200;
  private static final long ARCHIVE_PERIOD = 3600;
  private final String ACCESS_TOKEN_KEY = "accessToken";
  private static String accessToken;
  private final SharedPreferences sharedPreferences;
  private final Handler handler;
  protected boolean enabledUpload = false;
  protected boolean enabledArchive = false;
  protected long archive_period;
  protected long upload_period;
  private int archiveCounter = 0;
  
  protected static final String ACTION_ARCHIVE_DATA = "ARCHIVE_DATA";
  protected static final String ACTION_UPLOAD_DATA = "UPLOAD_DATA";
  private static final boolean DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY = true;
  
 
  public static final String PASSWORD_KEY = "PASSWORD";

  public DBTrust(ComponentContainer container) {
    super(container.$form());
    // TODO Auto-generated constructor stub
    // Set up listeners
    form.registerForOnDestroy(this);
    handler = new Handler();
    mainUIThreadActivity = container.$context();
    
    //set upload and archive periods
    archive_period = ARCHIVE_PERIOD;
    upload_period = UPLOAD_PERIOD;
    
    // start FunfManger
    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the schedule)
    doBindService();
    
    // for storing probe config and system configurations
    sharedPreferences = getSystemPrefs(mainUIThreadActivity);
    
    accessToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, "");

  }
  
  /*
   *  This function is for saving name/value(json object) to remote DB. The remote DB current is a 
   *  mongo (non-sql) database. 
   *  Note: Don't know yet how to write a nice description of this method. 
   *        reference to JsonUitl$getListFromJsonObject() and JsonUtilTest.testGetListFromJsonObject()
   */
  @SimpleFunction(description = "Save data (name and value) to the remote database. " +
      "The value itself is a json representation in List. The list contains one two item list per key in the json object it is representing." +
      "Each two item list has the key String as its first element, the second element is the representation of a json object in list recursively." +
      "Later the name will be used to retrieve the value")
  public void SaveToTrustDB(String name, YailList value){
     // The name will be used to retrieve the value in the remote db later (either through HTTP request e.g. someurl&name
     // all key/value pair will be coupled with TIMESTAMP when save to remote DB
     
     final long timestamp = System.currentTimeMillis()/1000;
     
     Bundle b = new Bundle();
      b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, PIPE_NAME);
      b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
      b.putString(NameValueDatabaseService.NAME_KEY, name);
      
      String jsonVal = value.toJSONString(); // 
      b.putString(NameValueDatabaseService.VALUE_KEY, jsonVal);
      Intent i = new Intent(mBoundFunfManager, NameValueDatabaseService.class);
      i.setAction(DatabaseService.ACTION_RECORD);
      i.putExtras(b);
      form.startService(i);
   }
  
  /*
   * This function is used to save directly the json values to remoteDB
   * 
   */
  @SimpleFunction(description = "Save Json object to the remote database")
  public void SaveJsonToTrustDB(String name, Object value){
    
     final long timestamp = System.currentTimeMillis()/1000;
     
     Bundle b = new Bundle();
      b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, PIPE_NAME);
      b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
      b.putString(NameValueDatabaseService.NAME_KEY, name);
      
      b.putString(NameValueDatabaseService.VALUE_KEY, value.toString());
      Intent i = new Intent(mBoundFunfManager, NameValueDatabaseService.class);
      i.setAction(DatabaseService.ACTION_RECORD);
      i.putExtras(b);
      form.startService(i);
    
  }

  
  public static SharedPreferences getSystemPrefs(Context context) {
    // TODO Auto-generated method stub
    return context.getSharedPreferences(DBTrust.class.getName()
        + "_system", android.content.Context.MODE_PRIVATE);
  }
  
 
  
  
  public Class<? extends UploadService> getUploadServiceClass() {
    return HttpsUploadService.class;
  }
  

  
  /*
   * After we bind to FunfManger, we have to register self to Funf as a Pipeline. This is for later relevant 
   */
  private void registerSelfToFunfManager(){
    Log.i(TAG, "register self(probeBase) as a Pipeline to FunfManger");
    mBoundFunfManager.registerPipeline(PIPE_NAME, this);
    
  }
  
  // try local binding to FunfManager
  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mBoundFunfManager = ((FunfManager.LocalBinder) service)
          .getManager();
      
      registerSelfToFunfManager(); 

      Log.i(TAG, "Bound to FunfManager");

    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundFunfManager = null;

      Log.i(TAG, "Unbind FunfManager");

    }
  };
  
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
      // then unregister Pipeline action 
      unregisterPipelineActions();
      // Detach our existing connection.
      mainUIThreadActivity.unbindService(mConnection);
      mIsBound = false;
    }
  }

  private void unregisterPipelineActions() {
    // TODO Auto-generated method stub
    mBoundFunfManager.unregisterPipelineAction(this, ACTION_ARCHIVE_DATA);
    mBoundFunfManager.unregisterPipelineAction(this, ACTION_UPLOAD_DATA);
  }

  @Override
  public void onCreate(FunfManager manager) {
    // TODO Auto-generated method stub
    // This function will run once whenever FunfManager.registerPipeline() is called
    
    //do nothing for now
  }

  @Override
  public void onRun(String action, JsonElement config) {
    // TODO Auto-generated method stub
    if (ACTION_ARCHIVE_DATA.equals(action)) {
      Log.i(TAG, "Run pipe's action archive data");
      DBTrustUtil.archiveData(this.mainUIThreadActivity);

    }
    if (ACTION_UPLOAD_DATA.equals(action)) {
      // Do something else
      Log.i(TAG, "Run pipe's action UPLOAD_DATA");
      DBTrustUtil.uploadData(this.mainUIThreadActivity, DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY);

    }   

  }

  @Override
  public void onDestroy() {
    // TODO Auto-generated method stub
    doUnbindService();
    
  }
  
  
    /**
     * Authenticate to TrustFramework using OAuth
     */
    @SimpleFunction(
        description = "Redirects user to login to Trust Framework via the Http and " +
                      "obtain the accessToken if we don't already have authorization.")
  public void Authorize(String username, String password) {
      DBTrustUtil.authorize(form, this, username, password);
  }
    
  
  /**
   * Indicates whether the app has the access token or not
   *  
   */
   @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public boolean HasAuthorizedToken() {

      String token = getSystemPrefs(mainUIThreadActivity).getString(ACCESS_TOKEN_KEY, "");
      return (token=="")? false:true;
       
    }
  
  /**
   * Indicates when the login for uploading data has been successful.
   */
  @SimpleEvent(description =
               "This event is raised after the program calls " +
               "<code>Authorize</code> if the authorization was successful.  " +
               "After this event has been raised, enableUpload() can be called. " +
               "Another way to check if upload action is authorized, is to check " +
               "whether the token exists with <code>HasAuthorizedToken()</code>")
  public void UploadIsAuthorized() {
    Log.i(TAG, "UploadIsAuthorized");
    mainUIThreadActivity.runOnUiThread(new Runnable() {
      public void run() {
        Log.i(TAG, "UploadIsAuthorized() is called");
        EventDispatcher.dispatchEvent(DBTrust.this,
            "UploadIsAuthorized");
      }
    });
  } 
  
  /**
   * Indicates the interval for a re-occurring upload activity upload archived db to remote server
   */
  @SimpleFunction(description = "Set the schedule for Archiving service")
  public void SetScheduleUpload(int newUploadPeriod) {

    this.upload_period = newUploadPeriod;

  }
  
  /**
   * Indicates the interval for a re-occurring archive activity for DB
   */
  @SimpleFunction(description = "Set the schedule for Archiving service")
  public void SetScheduleArchive(int newArchivePeriod) {

    this.archive_period = newArchivePeriod;

  }
  
  
  /*
   * Set the URLs and Ports for Remote PDS servers and Registry server 
   *  (personal data store is the place to store uloaded data; registry server is the one that issues OAuth2 token)
   */
  
  @SimpleFunction(description = "Set the url and port for PDS (personal data store)")
  public void SetPDS(String pdsURL, String pdsPort, String registryURL, String registryPort){
    DBTrustUtil.SetDBTrust(pdsURL, pdsPort, registryURL, registryPort);
    
  }

  
}
