// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
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
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.DropboxUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.DropboxUploadService;
import com.google.appinventor.components.runtime.DropboxArchive;

import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;



/**
 * Component for sync data on App Inventor to Dropbox.
 *
 * @author fuming@mit.edu (Fuming Shih)  
 */

@DesignerComponent(version = YaVersion.DROPBOX_COMPONENT_VERSION,
    description = "<p>A non-visible component that enables data reading from  " +
    "uploading to <a href=\"https://www.dropbox.com\" target=\"_blank\">Dropbox</a>. " +
    "<p>You must obtain a App Key and App Secret for Dropbox authorization " +
    " specific to your app from https://www.dropbox.com/developers/apps </p>. " +
    "The component have the capability to schedule a periodic upload event or directly upload data",
    category = ComponentCategory.FUNF,
    nonVisible = true,
    iconName = "images/dropbox.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET," +
		"android.permission.WAKE_LOCK, " +
		"android.permission.WRITE_EXTERNAL_STORAGE, " +
		"android.permission.READ_LOGS, " + 
    "android.permission.ACCESS_NETWORK_STATE")
 @UsesLibraries(libraries =
    "dropbox.jar," +
    "apache-httpcomponent-httpmime.jar," +
    "funf.jar," +
    "json-simple.jar")
public class Dropbox extends AndroidNonvisibleComponent
  implements ActivityResultListener, Component, Pipeline, OnResumeListener{
  public static final String TAG = "Dropbox";
  
  // Now we separate archiving and uploading data, dropbox component will only handle 
  // uploading part. The archiving part will be handle by NameValueDatabaseService
  // This means that if the user want to save their data to NameValueDB and later upload to 
  // dropbox, they need to use two components (NameValueDB component, Dropbox component)
  
  /*
   * Binding to FunfMananger service, for scheduling task to run periodically. 
   */

  protected FunfManager mBoundFunfManager = null;
  protected boolean mIsBound = false;
  public static final String DROPBOX_PIPE_NAME = "dropbox";
 
  private boolean enablePeriodicUploadDB = false;
  private boolean enablePeriodicUploadFolder = false;
  private String uploadTarget = null;
  private long upload_period;
  private static final long SCHEDULE_UPLOAD_PERIOD = 7200; //default period for uploading task 

  
  protected static final String ACTION_UPLOAD_DATA = "UPLOAD_DATA";
  private static final boolean DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY = true;
  public static final String PIPE_NAME = "Dropbox"; 
  private boolean wifiOnly = false;
  

  ///
  protected static Activity mainUIThreadActivity;
  
  private final int requestCode;
  private final ComponentContainer container;
  private final Handler handler;
  private final SharedPreferences sharedPreferences;
  //Auth stuff
  private AccessTokenPair accessTokenPair;
  private RequestTokenPair requestTokenPair;
  
  //binding to DropboxUploadService
  
  protected DropboxUploadService mBoundDropboxService= null;
 
  
  private volatile String appKey = "";
  private volatile String appSecret = "";
  private static DropboxAPI<WebAuthSession> dropboxApi;
  private static final String WEBVIEW_ACTIVITY_CLASS = WebViewActivity.class.getName();
  private static WebAuthSession session;
 
  private String dropboxFolder =""; 
  /*
   * Because we run the upload service on the background periodically, we need a way to understand if there's 
   * anything wrong with the background work (e.g. network problem, quota exceed,..etc) and decide to terminate
   * the and remove the schedule task or not. Using lastupload_status to report if successful or not, 
   * if not successful, the error message will placed in the lastupload_report
   */
  public static final String DROPBOX_LASTUPLOAD_REPORT = "dropbox_lastupload_report";
  public static final String DROPBOX_LASTUPLOAD_STATUS = "dropbox_lastupload_status";   
  public static final String DROPBOX_LASTUPLOAD_TIME = "dropbox_lastupload_time";

  // lock protects fields requestToken, accessToken. 
  // This follows the practice in Twitter.java
  private final Object lock = new Object();
  
  // dropboxLock synchronizes uses of any/all dropboxlock objects
  // in this class. These objects are not thread-safe
  private final Object dropboxLock = new Object();
  
  
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



  /*
   * After we bind to FunfManger, we have to register self to Funf as a Pipeline. 
   * This is for later to be wakened up and do pipe actions like archiving and uploading 
   */
  private void registerSelfToFunfManager(){
    Log.i(TAG, "register self(probeBase) as a Pipeline to FunfManger");
    mBoundFunfManager.registerPipeline(DROPBOX_PIPE_NAME, this);
    
  }
  
  void doBindService() {

    mainUIThreadActivity.bindService(new Intent(mainUIThreadActivity,
        FunfManager.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
    Log.i(TAG,
        "FunfManager is bound, and now we could have register dataRequests");

  }
  
  void doUnbindService() {
    if (mIsBound) {
      // unregister Pipeline action 
      unregisterPipelineActions();
      // Detach our existing connections.
      mainUIThreadActivity.unbindService(mConnection);
      mIsBound = false;
    }
  }
  
  public void unregisterPipelineActions() {
    // TODO Auto-generated method stub
     mBoundFunfManager.unregisterPipelineAction(this, ACTION_UPLOAD_DATA);   
    
  }
  
  OnSharedPreferenceChangeListener bgServiceStatuslistener = new OnSharedPreferenceChangeListener() {
    public void onSharedPreferenceChanged(SharedPreferences prefs,
        String key) {
      if(key.equals(Dropbox.DROPBOX_LASTUPLOAD_STATUS)){
       //do call event
        boolean status = sharedPreferences.getBoolean(Dropbox.DROPBOX_LASTUPLOAD_STATUS, true);
        String log = sharedPreferences.getString(Dropbox.DROPBOX_LASTUPLOAD_REPORT, "");

        ServiceStatusChanged(status, log);
        
      }
    }
  };
  
  public Dropbox(ComponentContainer container) {
    super(container.$form());
    this.container = container;
    handler = new Handler();
    sharedPreferences = container.$context().getSharedPreferences(DropboxUtil.PREFS_DROPBOX,
        Context.MODE_PRIVATE);
    //try 
    sharedPreferences.registerOnSharedPreferenceChangeListener(bgServiceStatuslistener);
    //
    accessTokenPair = retrieveAccessToken();
    mainUIThreadActivity = container.$context();
    
    // start a FunfManager Service
    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the
    // schedule)
    doBindService();

    requestCode = form.registerForActivityResult(this);
    upload_period = Dropbox.SCHEDULE_UPLOAD_PERIOD;

 }
  

  

 
  /**
   * Start WebAuthentication session by logging in to Dropbox with a username and password.
   */
  @SimpleFunction(
      description = "Redirects user to login to Dropbox via the Web browser using " +
                    "the OAuth protocol if we don't already have authorization.")
  public void Authorize() {
    if (appKey.length() == 0 || appSecret.length() == 0) {
      form.dispatchErrorOccurredEvent(this, "Authorize",
          ErrorMessages.ERROR_DROPBOX_BLANK_APP_KEY_OR_SECRET);
      return;
    }
    final String myAppKey = appKey;
    final String myAppSecret = appSecret;
    final String authURL;
    
    Log.i(TAG, "APP Key:" + appKey);
    Log.i(TAG, "APP Secret:" + appSecret);
    
    //save the app key to sharedPreference for later use
    saveAppToken(appKey, appSecret);
    
    AppKeyPair appKeys = new AppKeyPair(myAppKey, myAppSecret);
    session = new WebAuthSession(appKeys, AccessType.APP_FOLDER);
   
    
    try {
      Log.i(TAG, "before get AuthInfo");
      WebAuthInfo authInfo = session.getAuthInfo();
      Log.i(TAG, "before requestToken...");
      requestTokenPair = authInfo.requestTokenPair;
      Log.i(TAG, "after requestTokenPair....");
      authURL = authInfo.url;
      Log.i(TAG, "After authURL:" + authURL);
      
      // start browser for authentication
      AsynchUtil.runAsynchronously(new Runnable() {
        public void run() {
          if (CheckAuthorized()) {
            // if we have the access token already, just return
            return;
          }

          // potentially time-consuming calls

          // Redirect user to browser url
          Intent browserIntent = new Intent(Intent.ACTION_MAIN, Uri.parse(authURL));
          browserIntent.setClassName(container.$context(), WEBVIEW_ACTIVITY_CLASS);
          container.$context().startActivityForResult(browserIntent, requestCode);

        }
      });
      
      
    } catch (DropboxException e) {
      // TODO Auto-generated catch block
      Log.i("Dropbox", "Got exception: " + e.getMessage());
      e.printStackTrace();
      form.dispatchErrorOccurredEvent(Dropbox.this, "Authorize",
          ErrorMessages.ERROR_DROPBOX_EXCEPTION, e.getMessage());
      DeAuthorize(); // clean up
    }

  }
  
  /*
   * Get result from starting WebView activity to authorize access
   */
  @Override
  public void resultReturned(int requestCode, int resultCode, Intent data) {
    Log.i(TAG, "After authorized.... " + resultCode);

    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        AccessTokenPair myAccessTokenPair = null;

        try {

          // now retrieve the access token to this web session
          String userId = session.retrieveWebAccessToken(requestTokenPair);

          // we should get the access token by now
          myAccessTokenPair = session.getAccessTokenPair();

          handler.post(new Runnable() {
            @Override
            public void run() {
              IsAuthorized();
            }
          });

        } catch (DropboxException e) {
          // TODO Auto-generated catch block
          System.err.println("Could not retrieve WebAccessTokens. " + e);
          e.printStackTrace();
        }

        synchronized (lock) {
          
          accessTokenPair = myAccessTokenPair;
          saveAccessToken(accessTokenPair);
                    
        }
      }
    });
  }
  
  /**
   * Remove authentication for this app instance
   */
  @SimpleFunction(
      description = "Removes Dropbox authorization from this running app instance")
  public void DeAuthorize() {
    final DropboxAPI<WebAuthSession> OldDropboxApi;
    synchronized (lock) {
      
      OldDropboxApi = dropboxApi;
      dropboxApi = null;  // setting dropboxApi to null gives us a quick check
                       // that we don't have an authorized version around.
      accessTokenPair = null;
      saveAccessToken(accessTokenPair);
    }
  } 
  
  private void saveAppToken(String appKey, String appSecret){
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    
    sharedPrefsEditor.putString(DropboxUtil.PREF_DROPBOX_APP_KEY, appKey);
    sharedPrefsEditor.putString(DropboxUtil.PREF_DROPBOX_APP_SECRET, appSecret);
    sharedPrefsEditor.commit();
    
  }
  
  
  private void saveAccessToken(AccessTokenPair accessToken) {
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    if (accessToken == null) {
      sharedPrefsEditor.remove(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_KEY);
      sharedPrefsEditor.remove(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_SECRET);
    } else {
      sharedPrefsEditor.putString(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_KEY, accessToken.key);
      sharedPrefsEditor.putString(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_SECRET, accessToken.secret);
    }
    sharedPrefsEditor.commit();
  }
  
  /**
   * AppKey property getter method.
   */
  @SimpleProperty(
      category = PropertyCategory.BEHAVIOR)
  public String AppKey() {
    return appKey;
  }

  /**
   * AppKey property setter method: sets the AppKey to be used
   * when authorizing with Dropbox via OAuth.
   *
   * @param AppKey the key for use in Dropbox OAuth
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty
  public void AppKey(String appKey) {
    this.appKey = appKey;
  }

  /**
   * AppSecret property getter method.
   */
  @SimpleProperty(
      category = PropertyCategory.BEHAVIOR)
  public String appSecret() {
    return appSecret;
  }

  /**
   * AppSecret property setter method: sets the App Secret to be used
   * when authorizing with Dropbox via OAuth.
   *
   * @param AppSecret the secret for use in Dropbox OAuth
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty
  public void AppSecret(String appSecret) {
    this.appSecret= appSecret;
  }

  private AccessTokenPair retrieveAccessToken() {
    String token_key = sharedPreferences.getString(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_KEY, "");
    String secret = sharedPreferences.getString(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_SECRET, "");
    if (token_key.length() == 0 || secret.length() == 0) {
      return null;
    }
    return new AccessTokenPair(token_key, secret);
  }
  
  
  /**
   * Check whether we already have a Dropbox access token
   */
  @SimpleFunction(
      description = "Checks whether we already have access token already, " +
      		"if so, return True")
  public boolean CheckAuthorized() {
    String token_key = sharedPreferences.getString(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_KEY, "");
    String secret = sharedPreferences.getString(DropboxUtil.PREF_DROPBOX_ACCESSTOKEN_SECRET, "");
    if (token_key.length() == 0 || secret.length() == 0) {
      return false;
    }
    else
      return true;

  } 
  
  /**
   * Indicates when the authorization has been successful.
   */
  @SimpleEvent(description =
               "This event is raised after the program calls " +
               "<code>Authorize</code> if the authorization was successful.  " +
               "Only after this event has been raised or CheckAuthorize() returns True," +
               " any other method for this " +
               "component can be called.")
  public void IsAuthorized() {
    EventDispatcher.dispatchEvent(this, "IsAuthorized");
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

    if (ACTION_UPLOAD_DATA.equals(action)) {
      // Do something else //debug
      SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
      Date date = new Date();
      String currentDatetime = dateFormat.format(date);
      Log.i(TAG, "Run pipe's action UPLOAD_DATA at:" + System.currentTimeMillis() + "," + currentDatetime);
           
      upload();

    }

  }
  

  /*
   * Internal use for periodical uploading task
   */
  private void upload() {
    if (uploadTarget != null) {// either folder or db
      if (this.enablePeriodicUploadDB) {
        uploadDB(uploadTarget);

      } else {

        uploadFile(uploadTarget);

      }

    }
  }
 

  @Override
  public void onDestroy() {
    // TODO Auto-generated method stub
    doUnbindService(); //unbind the funfManager service
    StopScheduleUpload(); //stop the schedule task
    
    //write something to the shared preference (for debug purpose)
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    sharedPrefsEditor.putBoolean(DROPBOX_LASTUPLOAD_STATUS, false);
    sharedPrefsEditor.putString(DROPBOX_LASTUPLOAD_REPORT, "Activity Got Killed");
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    String currentDatetime = dateFormat.format(date);
    sharedPrefsEditor.putString(DROPBOX_LASTUPLOAD_TIME, currentDatetime);
    sharedPrefsEditor.commit();
    
    
  }
  
  public Class<? extends UploadService> getUploadServiceClass() {
    return DropboxUploadService.class;
  }
  
  /**
   * Indicates whether the user has specified that the DropBox could only 
   * use Wifi. If this value is set to True, the Dropbox uploader will use either Wifi or 3G/4G 
   * dataservice, whichever is available.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
  @SimpleProperty
  public void WifiOnly(boolean wifiOnly) {

    if (this.wifiOnly != wifiOnly)
      this.wifiOnly = wifiOnly;

  }
  
  /**
   * Indicates whether the user has specified that the DropBox could only 
   * use Wifi
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public boolean WifiOnly() {
    return wifiOnly;
  }
  
  /*
   * Upload a file to Dropbox
   */
  @SimpleFunction(description = "This function uploads the file " +
      "(as specified with its filepath) to dropbox folder. ")
      
  public void UploadData(String filepath) throws IOException {
    //TODO: use MediaUtil.java to know about the file type and how to deal with it
    // this will be the archive file name 
    //This method uploads the specified file directly to Dropbox 
    
    String filePath = "";
    if(filepath.startsWith("file:")){
      try { //convert URL string to URI and to real path : file:///sdcard --> /sdcard 
        filePath = new java.io.File(new URL(filepath).toURI()).getAbsolutePath();
      }catch (IllegalArgumentException e) {
        throw new IOException("Unable to determine file path of file url " + filepath);
      } catch (URISyntaxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    else {
      filePath = filepath;
    }
    
    new AsyncUploader(mainUIThreadActivity).execute(filePath);

  }
  

  /*
   * Set the full Dropbox path where to put the file, just the directories
   */
  
  @SimpleProperty(description = "This function specifies the path of the Dropbox" +
      "folder in which the uploaded file will be put. Default is \"\", the files will" +
      "be places under /Apps/App Inventor/Your-App-Name/data/ on each end user's dropbox." +
      "", category = PropertyCategory.BEHAVIOR)
  		public void DropboxUploadFolder(String dirPath){
    
    this.dropboxFolder = dirPath;
    
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    
    sharedPrefsEditor.putString(DropboxUtil.PREF_DROPBOX_UPLOAD_FOLDER, this.dropboxFolder);
 
    sharedPrefsEditor.commit();

  }
  
  /*
   * Set the Dropbox folder(directory) to where the uploaded file will be placed
   */

  @SimpleProperty(description = "Return the Dropbox folder(directory) to where the uploaded" +
  		"file will be placed", category = PropertyCategory.BEHAVIOR)
  public String DropboxUploadFolder(){
    return this.dropboxFolder;
    
  }
  
  /**
   * Indicates the interval for a re-occurring upload activity for uploading file to dropbox
   */
  @SimpleProperty(description = "Return the upload period of the current schedule uploading task")
  public float UploadPeriod() {

    return this.upload_period;

  }
  
  private void saveDBName(String dbName){
    //Save the DB name for DropboxUtil.java to use later 
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    
    sharedPrefsEditor.putString(DropboxUtil.PREF_DROPBOX_UPLOAD_DB_NAME, dbName);
 
    sharedPrefsEditor.commit();
    
  }
  //private method that will create an Upload Service, only use for schedule task 
  private void uploadFile(String fileName){
    
    String archiveName = uploadTarget;

    Intent i = new Intent(mainUIThreadActivity, getUploadServiceClass());
    i.putExtra(UploadService.ARCHIVE_ID, archiveName);
    i.putExtra(DropboxUploadService.FILE_TYPE, DropboxUploadService.REGULAR_FILE);
    i.putExtra(UploadService.REMOTE_ARCHIVE_ID, DropboxArchive.DROPBOX_ID);
    i.putExtra(UploadService.NETWORK,(this.wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
          : UploadService.NETWORK_ANY);
    mainUIThreadActivity.startService(i);
  }
  
  private void uploadDB(String dbName){
    // we need to savd DB name in the sharedPreference for DropboxUtil to pick it up
    saveDBName(dbName); 
    archiveData(dbName);
    Log.i(TAG, "start uploading process data....");
    Intent i = new Intent(mainUIThreadActivity, getUploadServiceClass());
    Log.i(TAG, "dbName:" + dbName);
    i.putExtra(UploadService.ARCHIVE_ID, dbName);
    i.putExtra(DropboxUploadService.FILE_TYPE, DropboxUploadService.DATABASE_FILE);
    i.putExtra(UploadService.REMOTE_ARCHIVE_ID, DropboxArchive.DROPBOX_ID);
    i.putExtra(UploadService.NETWORK,
        (wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
            : UploadService.NETWORK_ANY);
    Log.i(TAG, "before starting upload service:");
    mainUIThreadActivity.startService(i);

  }
  
  /*
   * 
   */
  @SimpleFunction(description = "Enable to upload the specified db file to remote " +
  		"storage place for backup. Will first archive the database. By default, if it's database name for" +
  		"sensor compoent, it should be \"SensorData\"")
  public void UploadDB(String dbName){
    uploadDB(dbName);
    
  }
  
  private void archiveData(String dbName) {

    Intent i = new Intent(mainUIThreadActivity, NameValueDatabaseService.class);
    Log.i(TAG, "archiving data....");
    i.setAction(DatabaseService.ACTION_ARCHIVE);
    i.putExtra(DatabaseService.DATABASE_NAME_KEY, dbName);
    mainUIThreadActivity.startService(i);
    Log.i(TAG, "after archiving data....");

  }
 
  /*
   * Start and set interval for a re-occurring upload activity for uploading file to dropbox
   * Current we can either do uploading folder or uploading archived db
   */
  @SimpleFunction(description = "Enable upload scheduled task based on specified filepath "
      + "of a folder locally in parameter <code>folderPath</code>. " 
      + "One use case is to upload all the save photos"
      + "in some SD folder periodically. The parameter <code>period</code> is in second.")
  public void ScheduleUpload(String folderPath, long period) {

    // will throw an exception if try to startPeriodUpload when there's one
    // on-going schedule Task

    if (this.enablePeriodicUploadFolder || this.enablePeriodicUploadDB) {
      form.dispatchErrorOccurredEvent(Dropbox.this, "StartScheduleUpload",
          ErrorMessages.ERROR_DROPBOX_NO_TWO_RUNNING_TASKS);
    } else {

      this.enablePeriodicUploadFolder = true;
      this.upload_period = period;
      uploadTarget = folderPath;

      Schedule uploadPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.upload_period), BigDecimal.ZERO, false, false);

      mBoundFunfManager.registerPipelineAction(this, ACTION_UPLOAD_DATA,
          uploadPeriod);
    }

  }

  
  /*
   * Start and set interval for a re-occurring upload activity for uploading db
   * to dropbox Current we can either do uploading folder or uploading archived
   * db
   */
  @SimpleFunction(description = "Enable upload scheduled task based on specified filepath "
      + "of a folder locally. One use case is to upload all the save photos"
      + "in some SD folder periodically")
  public void ScheduleUploadDB(String dbName, long period) {

    // Will throw an exception if try to startPeriodUpload when there's one
    // on-going schedule Task

    if (this.enablePeriodicUploadFolder || this.enablePeriodicUploadDB) {
      form.dispatchErrorOccurredEvent(Dropbox.this, "StartScheduleUpload",
          ErrorMessages.ERROR_DROPBOX_NO_TWO_RUNNING_TASKS);
    } else {
      this.enablePeriodicUploadDB = true;
      this.upload_period = period;
      uploadTarget = dbName;
      Schedule uploadPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(upload_period), BigDecimal.ZERO, false, false);

      mBoundFunfManager.registerPipelineAction(this, ACTION_UPLOAD_DATA,
          uploadPeriod);

    }
  }
  
  /*
   * Stop the current running schedule task. 
   */
  @SimpleFunction(description = "Stop the schedule uploading task")
  public void StopScheduleUpload(){
    this.enablePeriodicUploadFolder = false;
    this.enablePeriodicUploadDB = false;
    mBoundFunfManager.unregisterPipelineAction(this, ACTION_UPLOAD_DATA);//stop periodic schedule task
     
  }
  
  
  @SimpleProperty (description = "Indicates whether there exists any schedule upload task")
  public boolean ScheduleUploadEnabled(){
    return this.enablePeriodicUploadFolder || this.enablePeriodicUploadDB;
  }
  

  @Override
  public void onResume() {
    // TODO Auto-generated method stub
    
  }
  
  /**
   * Indicates when the authorization has been successful.
   */
  @SimpleEvent(description =
               "This event is raised after the program calls " +
               "<code>UploadData</code> if the upload task was done.")
  public void UploadDone(boolean successful) {
    Log.i(TAG, "uploadDone");
    EventDispatcher.dispatchEvent(this, "UploadDone", successful);
  }

  private void displayErrorMessage(Exception e){
    try {
      throw e;
    } catch (DropboxUnlinkedException exp) {
      // TODO Auto-generated catch block
      form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
          ErrorMessages.ERROR_TWITTER_EXCEPTION);
    } catch (DropboxFileSizeException exp){
      form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
          ErrorMessages.ERROR_DROPBOX_FILESIZE);
    } catch (DropboxPartialFileException exp){
      form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
          ErrorMessages.ERROR_DROPBOX_PARTIALFILE);
    } catch (DropboxServerException exp){
      if(exp.error == DropboxServerException._507_INSUFFICIENT_STORAGE){
        form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
            ErrorMessages.ERROR_DROPBOX_SERVER_INSUFFICIENT_STORAGE);
      }

    } catch (DropboxIOException exp){
      form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
          ErrorMessages.ERROR_DROPBOX_IO);
      
    } catch (FileNotFoundException exp){
      form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
          ErrorMessages.ERROR_DROPBOX_FILENOTFOUND);
      
    } catch (Exception exp) {
      //something bad just happened
      form.dispatchErrorOccurredEvent(Dropbox.this, "UploadData/UploadDB",
          ErrorMessages.ERROR_DROPBOX_EXCEPTION);
    }
        
  }
  
  
  private class AsyncUploader extends AsyncTask<String, Void, Boolean>{

    private static final String TAG = "DropBoxAysncUploader";
    private final Activity activity; // The main list activity
    private final ProgressDialog dialog;
    
    AsyncUploader(Activity activity) {
      this.activity = activity;
      dialog = new ProgressDialog(activity);
      
    }
    @Override
    protected void onPreExecute() {
      dialog.setMessage("Uploading file...");
      dialog.show();
    }
    
    @Override
    protected Boolean doInBackground(String... params) {
      // Called DropboxUtil to upload the file
      Log.i(TAG, "Starting doInBackground " + params[0]);
      String filepath = params[0];
      java.io.File uploadFile = new java.io.File(filepath);
      boolean uploadResult = false;
      try {
        uploadResult = DropboxUtil.uploadDataFile(mainUIThreadActivity, uploadFile);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        displayErrorMessage(e);

        return false;
      }
      
      return uploadResult;

    }
    
    /**
     * Fires the AppInventor uploadDone() method
     */
    @Override
    protected void onPostExecute(Boolean resultSuccessful) {
      // if no exception happened during the Google Drive uploading event, then
      // just call
      // uploadDone event, else throw exception
        dialog.dismiss();
        UploadDone(resultSuccessful);
    }  

       
  }
  
  @SimpleFunction(description = "Get the status of the status of the most recent schedule upload task")
  public boolean GetScheduleTaskStatus() {
    return sharedPreferences.getBoolean(Dropbox.DROPBOX_LASTUPLOAD_STATUS, true);
    
  }
  
  @SimpleFunction(description = "Get the message log of the most recent schedule upload task")
  public String GetScheduleTaskLog(){
    return sharedPreferences.getString(DROPBOX_LASTUPLOAD_REPORT, "");
    
  }
  
  @SimpleFunction(description = "Get the finshed datetime of last upload service task")
  public String GetScheduleTaskLogTime(){
    return sharedPreferences.getString(DROPBOX_LASTUPLOAD_TIME, "");
  }

  /*
   * Give the component an event listener that gives the recent status and log of the background service
   */
  @SimpleEvent(description = "This event is raised when upload service status has changed")

  public void ServiceStatusChanged(boolean successful, String log) {
    
    
    
    Log.i(TAG, "ServiceStatusChanged:" + successful + ", " + log);
    EventDispatcher.dispatchEvent(this, "ServiceStatusChanged", successful, log);
  }
  
 
}
