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
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.HttpsUploadService;
//import com.google.appinventor.server.flags.Flag;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

 
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.security.Base64Coder;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;
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
SensorComponent, OnDestroyListener, Pipeline{

  protected boolean enabled = false; // run once
  protected boolean enabledSchedule = false; // run periodically
  
  
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
  
  // for system operations: archive and upload
//  urls with 8002 and 8001 ports on air machine for testing previous version server are obsolete 
//  private static final String UPLOAD_URL = "http://air.csail.mit.edu:8002/connectors/set_funf_data";
//  private static final String SET_KEY_URL = "http://air.csail.mit.edu:8002/connectors/set_funf_key";
  
  //for testing version MITv0.3
// TODO: move all these configuration to runtime.components.commons
  
  private String base_url = "";
  private String port = "";
  private static final String UPLOAD_POSTFIX = "/connectors/funf/set_funf_data";
  private static final String SETKEY_POSTFIX = "/connectors/funf/set_funf_key";

  //The default server URL should be wherever the trustframework server is deployed, else it won't work
  private static final String UPLOAD_URL = "http://128.30.16.224:8031/connectors/funf/set_funf_data";
  private static final String SET_KEY_URL = "http://128.30.16.224:8031/connectors/funf/set_funf_key";
  
  
  private int archiveCounter = 0;
  private static final long UPLOAD_PERIOD = 7200;
  private static final long ARCHIVE_PERIOD = 3600;
  private static final String ACCESS_TOKEN_KEY = "accessToken";
  private static String accessToken;
  private final SharedPreferences sharedPreferences;
  private final Handler handler;
  protected boolean enabledUpload = false;
  protected boolean enabledArchive = false;
  protected long archive_period;
  protected long upload_period;
  
  protected static final String ACTION_ARCHIVE_DATA = "ARCHIVE_DATA";
  protected static final String ACTION_UPLOAD_DATA = "UPLOAD_DATA";
  private static final boolean DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY = true;
  
  // OAuth stuff 
  
  private volatile String clientID = null;
  private volatile String clientSecret = null;
  

//  final String TOKEN_URL = "http://air.csail.mit.edu:8001/oauth2/token/";
  private static final String TOKEN_POSTFIX = "/oauth2/token/";
  final String TOKEN_URL = "http://128.30.16.224:8031/oauth2/token/";
  
  
  final String SCOPE = "funf_write";
// The below keys/secrets only make sense for MIT internal testing on air 
  
//  Fuming's app client info (for Trustframework internal testing)
//  final String CLIENT_ID = "062044d6b51e39e6eb8066a4516a4a";
//  final String CLIENT_SECRET = "330c8fa32eb184dea65b6e69f39a16";
//  Jeff's app client info (for Trustframework internal testing)
//  final String CLIENT_ID = "a2e864bedc79a3018d1e7fb0bfaadb";
//  final String CLIENT_SECRET = "38682bffe775dd794404724bd880e4";
  
  //for testing purpose for trustframework v0.3

  final String CLIENT_ID = "4bf895513f6dab94f845156ae9fa23";
  final String CLIENT_SECRET = "293dd97aafda0d2531fb1c61438919";
  
  public static final String PASSWORD_KEY = "PASSWORD";
  
  // save to db
  protected boolean enabledSaveToDB = false; //local for each probe
  
  private boolean logToFile = true;
  
  
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
      
      registerSelfToFunfManager(); 

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

    // collectLog();

    // Set up listeners
    form.registerForOnDestroy(this);
    handler = new Handler();
    mainUIThreadActivity = container.$context();

    // set upload and archive periods
    archive_period = ARCHIVE_PERIOD;
    upload_period = UPLOAD_PERIOD;

    // start FunfManger
    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the
    // schedule)
    doBindService();

    // for storing probe config and system configurations
    sharedPreferences = getSystemPrefs(mainUIThreadActivity);

    accessToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, "");

    // assign an unique notification id for each probe to use
    PROBE_NOTIFICATION_ID = getNotificationID();

    // get Notification Manager
    String ns = Context.NOTIFICATION_SERVICE;
    mNM = (NotificationManager) mainUIThreadActivity.getSystemService(ns);
    Log.i(TAG, "created notification manager");

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
      unregisterPipelineActions();
      // Detach our existing connection.
      mainUIThreadActivity.unbindService(mConnection);
      mIsBound = false;
    }
  }

  private String getUpLoadURL(){
    
    String upLoadURL = (base_url == "")? UPLOAD_URL:base_url + ":" + port + UPLOAD_POSTFIX;
    
    return upLoadURL;
  }
  
  private String getSetKeyURL(){
    String setKeyURL = (base_url == "")? SET_KEY_URL:base_url + ":" + port + SETKEY_POSTFIX;
    
    return setKeyURL; 
    
  }
  
  private String getTokenURL(){
    
    String getTokenURL = (base_url == "")? TOKEN_URL:base_url + ":" + port + TOKEN_POSTFIX;
    return getTokenURL;
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
   * Set the length of the interval for a re-occurring probe activity. 
   * Setting this value triggers the probe using the new schedule 
   * 
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "60")
  @SimpleProperty
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
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
  @SimpleProperty
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
  
  // below are functions for saving to DB and httpArchive
  
  public void archiveData() {

    Intent i = new Intent(mainUIThreadActivity, NameValueDatabaseService.class);
    Log.i(TAG, "archiving data....archive count:" + archiveCounter++);
    i.setAction(DatabaseService.ACTION_ARCHIVE);
    i.putExtra(DatabaseService.DATABASE_NAME_KEY, PROBE_BASE_NAME);
    mainUIThreadActivity.startService(i);

  }
  
  
  public static final String PROBE_BASE_NAME = "SensorData";

  public Class<? extends UploadService> getUploadServiceClass() {
    return HttpsUploadService.class;
  }
  
  public void uploadData(boolean wifiOnly) {
    archiveData();
    String archiveName = PROBE_BASE_NAME;
//    String uploadUrl = UPLOAD_URL;
    String uploadUrl = getUpLoadURL();
    Intent i = new Intent(mainUIThreadActivity, getUploadServiceClass());
    i.putExtra(UploadService.ARCHIVE_ID, archiveName);
    i.putExtra(UploadService.REMOTE_ARCHIVE_ID, uploadUrl);
    i.putExtra(UploadService.NETWORK,
        (wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
            : UploadService.NETWORK_ANY);
    mainUIThreadActivity.startService(i);
  }
  
  public static SharedPreferences getSystemPrefs(Context context) {
    // TODO Auto-generated method stub
    return context.getSharedPreferences(ProbeBase.class.getName()
        + "_system", android.content.Context.MODE_PRIVATE);
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
        EventDispatcher.dispatchEvent(ProbeBase.this,
            "UploadIsAuthorized");
      }
    });

  }
  
//  
//    /**
//     * Authenticate to TrustFramework using OAuth
//     */
//    @SimpleFunction(
//        description = "Redirects user to login to Trust Framework via the Http and " +
//                      "obtain the accessToken if we don't already have authorization.")
//  public void Authorize(String username, String password) {
//    // if success, write access_token = prefs.getString("accessToken", "");
//    // as true into SharedPreference,
//      final String myUsername = username;
//      final String myPassword = password;
//      
//      final String _clientId = (clientID == null)? CLIENT_ID:clientID;
//      final String _clientSecret = (clientSecret == null)? CLIENT_SECRET:clientSecret;
//      
//      final String authHeader = Base64Coder.encodeString(_clientId + ":" + _clientSecret);
//      final String _uploadURL = this.getUpLoadURL();
//      
//      Log.i(TAG, "_clientId:" + _clientId);
//      Log.i(TAG, "_clientSecret:" + _clientSecret);
//      Log.i(TAG, "authHeader:" + authHeader);
//      
//    AsynchUtil.runAsynchronously(new Runnable() {
//      public void run() {
//
//        try {
//          HttpClient httpclient = new DefaultHttpClient();
//          HttpPost httppost = new HttpPost(getTokenURL());
//          List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
//              2);
//
//          nameValuePairs.add(new BasicNameValuePair("grant_type",
//              "password"));
//
//          nameValuePairs.add(new BasicNameValuePair("scope", SCOPE));
//          nameValuePairs.add(new BasicNameValuePair("username",
//              myUsername));
//          nameValuePairs.add(new BasicNameValuePair("password",
//              myPassword));
//          
//          nameValuePairs.add(new BasicNameValuePair("client_id",
//              _clientId));
//          nameValuePairs.add(new BasicNameValuePair("client_secret",
//              _clientSecret));
//          httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//          
//          httppost.addHeader(
//              "AUTHORIZATION",
//              "basic "
//                  + authHeader);
//
//          
//          Log.i(TAG, "URI:" + httppost.getURI());
//          Log.i(TAG, "Before sending http request for accessToken");
//          
//          HttpResponse response = httpclient.execute(httppost);
//          
//          if (response == null) {
//            throw new Exception("Http respone is null");
//
//          }
//          JsonObject json = getResponseJSON(response)
//              .getAsJsonObject();
//          Log.i(TAG, "return json:" + json);
//
//          //String error = json.get("error").getAsString();
// 
//          JsonElement error = json.get("error");
//          Log.i(TAG, "Never here1, something broke before");
//          String access_token = json.get("access_token")
//                .getAsString();
//          Log.i(TAG, "Never here2, something broke before");
//          String refresh_token = json.get("refresh_token")
//                .getAsString();
//          Log.i(TAG, "Never here3, something broke before");
//          Long expire_in = json.get("expires_in").getAsLong();
// 
//          Log.i(TAG, "Never here4, something broke before");
//          
//          if (json == null) {
//            Log.e(TAG, "Could not parse http response into json");
//            throw new Exception(
//                "Http rsponse could not be parsed into json");
//          }
//          // else if(json.containsKey("error"))
//          else if (error != null) {
//            Log.e(TAG,
//                "Http rsponse to token request contained an error");
//            throw new Exception(
//                "Http rsponse to token request contained an error");
//          } else if (access_token != null & refresh_token != null
//              & expire_in != null) {
//            // validate the oauth response with included access and
//            // refresh
//            // token
//            Log.i(TAG, "Obtained Token" + access_token);
//            Editor editor = sharedPreferences.edit();
//            editor.putString(ACCESS_TOKEN_KEY, access_token);
//            editor.putString("refreshToken", refresh_token);
//            editor.putString("expiresIn", String.valueOf(expire_in));
//            editor.putString("uploadURL", _uploadURL);
//            editor.commit();
//
//          } else// the token was invalid
//          {
//            Log.e(TAG, "Unexpected error in getting token response");
//            throw new Exception(
//                "Unexpected error in getting token response");
//          }
//
//          if (true) {
//            if (!sharedPreferences.contains("PASSWORD")) {
//              try {
//                getDataPassword(sharedPreferences);
//                List<NameValuePair> funfnameValuePairs = new ArrayList<NameValuePair>(1);
//
//                funfnameValuePairs.add(new BasicNameValuePair(
//                    "funf_key", sharedPreferences
//                        .getString("PASSWORD", "")));
//                funfnameValuePairs.add(new BasicNameValuePair("overwrite", "true"));
//
//                sendAuthorizedGetRequest(funfnameValuePairs, getSetKeyURL(), 
//                    sharedPreferences);
//                 
//                //tell App Inventor now everything is all set!
//                handler.post(new Runnable() {
//                  @Override
//                  public void run() {
//                    UploadIsAuthorized();
//                  }
//                });
//
//              } catch (Exception e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//              }
//
//            }
//
//          } else {
//
//            Log.e(TAG, "Server returned an invlid access token.");
//            throw new Exception(
//                "Server returned an invlid access token.");
//          }
//
//        } catch (NetworkErrorException e) {
//          //need to have more different type of exception here. Look at Twitter component
//          Log.e(TAG, "exception:", e);
//        }
//        catch (Exception e){
//                form.dispatchErrorOccurredEvent(ProbeBase.this, "Authorize",
//                        ErrorMessages.ERROR_WEB_UNABLE_TO_GET, e.getMessage());
//        }
//      }
//    });
//
//  }
//    
//  /**
//   * Indicates whether the app has the access token or not
//   *  
//   */
//   @SimpleProperty(category = PropertyCategory.BEHAVIOR)
//    public boolean HasAuthorizedToken() {
//      String token = getSystemPrefs(mainUIThreadActivity).getString(this.ACCESS_TOKEN_KEY, "");
//      return (token=="")? false:true;
//    }
//  
// 
//    
//    public JsonElement getResponseJSON(HttpResponse response) throws Exception {
//        try {
//          HttpEntity ht = response.getEntity();
//          BufferedHttpEntity buf = new BufferedHttpEntity(ht);
//          InputStream is = buf.getContent();
//          BufferedReader r = new BufferedReader(new InputStreamReader(is));
//
//          StringBuilder total = new StringBuilder();
//          String line;
//          while ((line = r.readLine()) != null) {
//            total.append(line);
//          }
//          JsonParser jsonParser = new JsonParser();
//          //JSONParser parser = new JSONParser();
//          JsonElement json = jsonParser.parse(total.toString());
//            if(json == null)
//            {
//              Log.e(TAG, "Could not parse http response into json");
//              throw new Exception("Http rsponse could not be parsed into json");
//            }
//
//          return json;
//        } catch(IOException ex) {
//          Log.e("GetAccessToken", "IOException " +ex.getMessage());
//          return null;
//        } catch(JsonParseException ex) {
//          Log.e("GetAccessToken", "ParseException " +ex.getMessage());
//          return null;
//        }
//      }
//
//    
//  public String getDataPassword(SharedPreferences prefs) {
//
//    if (prefs.contains(PASSWORD_KEY))
//      return prefs.getString(PASSWORD_KEY, null);
//    String password = prefs.getString(PASSWORD_KEY, null);
//    if (password == null) {
//      // Set the password in the pipeline the first time it is created
//      // password = randomPassword().substring(0, 8);//our service only
//      // supports 8 chars...there might be a better way to do this.
//      password = "changeme"; 
//      prefs.edit().putString(PASSWORD_KEY, password).commit();
//
//      // TODO: queue UUID, password to be sent to server
//    }
//    // password = "changeme";
//    Log.i(TAG, "Password set to: " + password);
//    return password;
//  }   
  
//  
//    public static HttpResponse sendAuthorizedGetRequest(List<NameValuePair> nameValuePairs, String url, SharedPreferences prefs) throws Exception {
//      HttpResponse response = null;
//      try {
//        HttpClient httpclient = new DefaultHttpClient();
//        String access = prefs.getString("accessToken", "");
//        BasicNameValuePair pair = new BasicNameValuePair("bearer_token", access);
//        if (nameValuePairs == null) {
//          nameValuePairs = new ArrayList<NameValuePair>(1);
//        }
//        nameValuePairs.add(pair);
//      String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
//        url += "?" + paramString; 
//        HttpGet httpget = new HttpGet(url);
//        httpget.setHeader("Authorization", "Bearer "+prefs.getString("accessToken", null));
//        response = httpclient.execute(httpget);
//      } 
//      catch (IOException ex) {
//      
//
//
//      }
//      return response;
//
//    }
  
    
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
   * Indicates whether the phone will periodically upload all sensed data in the DB to the 
   * remote server (trust framework server)
   * 
   * @param enabledUpload
   *            if true, will upload data from DB periodically according to
   *            upload_period
   */

  @SimpleFunction(description = "Upload all sensed data in DB to remote server")
  public void EnabledUpload(boolean enabledUpload) {

    if (this.enabledUpload != enabledUpload)
      this.enabledUpload = enabledUpload;
    
    
    Schedule uploadPeriod = new Schedule.BasicSchedule(
        BigDecimal.valueOf(upload_period), BigDecimal.ZERO, false,
        false);

    if (enabledUpload) {

      mBoundFunfManager.registerPipelineAction(this,
          ProbeBase.ACTION_UPLOAD_DATA, uploadPeriod);
    } else {
      
      mBoundFunfManager.unregisterPipelineAction(this, ProbeBase.ACTION_UPLOAD_DATA);
    } 

  }
  
  
  /**
   * Return the name of the database in which the sensed data is stored 
   * (actually there's only one db for sensed data (SensorData)
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String LocalDBName(){
    return ProbeBase.PROBE_BASE_NAME;
    
  }
  
//  
//    /**
//     * OAuth2 ClientID property getter method.
//     */
//    @SimpleProperty(
//        category = PropertyCategory.BEHAVIOR)
//    public String ClientID() {
//      return clientID;
//    }
//  
//    /**
//     * OAuth2 ClientID property setter method: sets the consumer key to be used
//     * when authorizing with TrustFramework via OAuth.
//     *
//     * @param consumerKey the key for use in Twitter OAuth
//     */
//    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
//        defaultValue = "")
//    @SimpleProperty
//    public void ClientID(String clientID) {
//      this.clientID = clientID;
//    }  
//    
//    /**
//     * OAuth2 ClientSecret property getter method.
//     */
//    @SimpleProperty(
//        category = PropertyCategory.BEHAVIOR)
//    public String ClientSecret() {
//      return clientSecret;
//    }
//    
//
//    /**
//     * OAuth2 ClientSecret setter method: sets the client secret to be used
//     * when authorizing with Trustframework via OAuth2.
//     *
//     * @param consumerSecret the secret for use in Twitter OAuth
//     */
//    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
//        defaultValue = "")
//    @SimpleProperty
//    public void ClientSecret(String clientSecret) {
//      this.clientSecret = clientSecret;
//    }
//    
//    
//    /**
//     * Return storage server URL.
//     */
//    @SimpleProperty(
//        category = PropertyCategory.BEHAVIOR)
//    public String StorageServerURL() {
//      return this.base_url;
//    }
//  
//    /**
//     * Set the URL of the remote data storage server
//     *
//     * @param consumerKey the key for use in Twitter OAuth
//     */
//    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
//        defaultValue = "http://some.server.addr")
//    @SimpleProperty
//    public void StorageServerURL(String serverURL) {
//      this.base_url = serverURL;
//    }  
//    
//    /**
//     * Return storage server port.
//     */
//    @SimpleProperty(
//        category = PropertyCategory.BEHAVIOR)
//    public String StorageServerPort() {
//      return this.port;
//    }
//  
//    /**
//     * Set the network connection port for the remote data storage server
//     *
//     * @param consumerKey the key for use in Twitter OAuth
//     */
//    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
//        defaultValue = "8031")
//    @SimpleProperty
//    public void StorageServerPort(String port) {
//      this.port = port;
//    }  
//    

  
  /**
   * Indicates whether the phone will periodically upload all sensed data in the 
   * DB to the remote server (trust framework server)
   * 
   * @param enabledArchive
   *            if true, will archive DB periodically according to archive_period
   *             
   */
  
  
  @SimpleFunction(description = "Upload all sensed data in DB to remote server")
  public void EnabledArchive(boolean enabledArchive) {

    if (this.enabledArchive != enabledArchive)
      this.enabledArchive = enabledArchive;
    
    
    Schedule archivePeriod = new Schedule.BasicSchedule(
        BigDecimal.valueOf(archive_period), BigDecimal.ZERO, false,
        false);

    if (enabledArchive) {
      Log.i(TAG, "enable archive! :" + archivePeriod);
      mBoundFunfManager.registerPipelineAction(this,
          ProbeBase.ACTION_ARCHIVE_DATA, archivePeriod);
    } else {
      Log.i(TAG, "disable archive! :" + archivePeriod);
      mBoundFunfManager.unregisterPipelineAction(this, ProbeBase.ACTION_ARCHIVE_DATA);
    } 

  }
  
  /**
   * Indicates the duration of the interval for a re-occurring archive activity for DB
   */
  @SimpleFunction(description = "Set the schedule for Archiving service")
  public void SetScheduleArchive(int newArchivePeriod) {

    this.archive_period = newArchivePeriod;

  }
  
  /**
   * Indicates the duration of the interval for a re-occurring upload activity for DB to remote server
   */
  @SimpleFunction(description = "Set the schedule for Archiving service")
  public void SetScheduleUpload(int newUploadPeriod) {

    this.upload_period = newUploadPeriod;

  }
  
  
  
  @Override
  public void onCreate(FunfManager manager) {
    // This function will run once whenever FunfManager.registerPipeline() is called
    
    //do nothing for now
  }
  @Override
  public void onRun(String action, JsonElement config){
    if (ACTION_ARCHIVE_DATA.equals(action)) {
      Log.i(TAG, "Run pipe's action archive data");
      archiveData();

    }
    if (ACTION_UPLOAD_DATA.equals(action)) {
      // Do something else
      Log.i(TAG, "Run pipe's action UPLOAD_DATA");
      uploadData(DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY);

    }   
    
  }
  
  /*
   * After we bind to FunfManger, we have to register self to Funf as a Pipeline. 
   * This is for later to be wakened up and do pipe actions like archiving and uploading 
   */
  private void registerSelfToFunfManager(){
    Log.i(TAG, "register self(probeBase) as a Pipeline to FunfManger");
    mBoundFunfManager.registerPipeline(PROBE_BASE_NAME, this);
    
  }
  

  public void unregisterPipelineActions() {
    // TODO Auto-generated method stub
     mBoundFunfManager.unregisterPipelineAction(this, ProbeBase.ACTION_ARCHIVE_DATA);
     mBoundFunfManager.unregisterPipelineAction(this, ProbeBase.ACTION_UPLOAD_DATA);
      
    
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
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
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
    
    notification.setLatestEventInfo(mainUIThreadActivity, (CharSequence)title, 
                      (CharSequence)text, mContentIntent);
    Log.i(TAG, "after updated notification contents");
    mNM.notify(PROBE_NOTIFICATION_ID, notification);
    Log.i(TAG, "notified");
  
  }
  
  
  public abstract void Enabled(boolean enabled); 
  public abstract void unregisterDataRequest();
  public abstract void registerDataRequest(int interval, int duration); 

}
