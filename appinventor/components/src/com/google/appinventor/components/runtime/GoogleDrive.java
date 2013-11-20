package com.google.appinventor.components.runtime;



import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import android.accounts.AccountManager;
import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import android.os.AsyncTask;

import android.os.Handler;
import android.os.IBinder;

import android.util.Log;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import com.google.api.services.drive.model.ParentReference;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;

import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.OAuth2Helper;

import com.google.appinventor.components.runtime.util.YailList;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Launcher;

import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.storage.UploadService;

//TODO: rename this component and the dropbox component to GoogleDriveUploader
@DesignerComponent(version = YaVersion.GOOGLE_DRIVE_COMPONENT_VERSION,
    description = "This component can upload file(s) to Google Drive.",
    category = ComponentCategory.CLOUDSTORAGE,
    nonVisible = true,
    iconName = "images/googledrive.png")
@UsesPermissions(permissionNames = "android.permission.GET_ACCOUNTS," +
    "android.permission.INTERNET," +
    "android.permission.WAKE_LOCK, " +
    "android.permission.WRITE_EXTERNAL_STORAGE, " +
    "android.permission.READ_LOGS, " + 
    "android.permission.ACCESS_NETWORK_STATE")
@UsesLibraries(libraries =
   "google-http-client-beta.jar," +
   "google-oauth-client-beta.jar," +
   "google-api-services-drive-v2.jar," +
   "google-api-client-beta.jar," +
   "google-api-client-android-beta-14.jar," +
   "google-http-client-android-beta-14.jar," +
   "google-http-client-gson-beta-14.jar, " +
   "google-play-services.jar," +
   "funf.jar")
  public class GoogleDrive extends AndroidNonvisibleComponent
      implements ActivityResultListener, Component, OnResumeListener, OnStopListener, OnDestroyListener{
  
  private static final String TAG = "GoogleDrive";

  protected boolean mIsBound = false;

  
  private final ComponentContainer container;
  private final Handler handler;
  private final SharedPreferences sharedPreferences;
  public static final String PREFS_GOOGLEDRIVE = "googledrive_pref";
  public static final String PREF_ACCOUNT_NAME = "gd_account";
  public static final String PREF_AUTH_TOKEN = "gd_authtoken";
  public final static String GD_FOLDER = "gd_folder";
  public static final String DEFAULT_GD_FOLDER = "gd_root";

  
  protected static Activity mainUIThreadActivity;
  private final int REQUEST_CHOOSE_ACCOUNT; 
  private final int REQUEST_AUTHORIZE;

  ////////////////
  private String gdFolder;
  private String gdFolder_id;
  
  //binding to FunfManager Service

  protected FunfManager mBoundFunfManager = null;

  
  private static final long SCHEDULE_UPLOAD_PERIOD = 7200; //default period for uploading task 



  private AccessToken accessTokenPair;
  

  private boolean wifiOnly = false;
  
  private static Drive service;
  private GoogleAccountCredential credential;


  private GoogleDrivePipeline mPipeline = null;
  private final String pipelineName = GoogleDrivePipeline.pipelineName;


  /*
   * Because we run the upload service on the background periodically, we need a way to understand if there's 
   * anything wrong with the background work (e.g. network problem, quota exceed,..etc) and decide to terminate
   * the and remove the schedule task or not. Using lastupload_status to report if successful or not, 
   * if not successful, the error message will placed in the lastupload_report
   */
  public static final String GOOGLEDRIVE_LASTUPLOAD_TARGET = "gd_lastupload_target";
  public static final String GOOGLEDRIVE_LASTUPLOAD_REPORT = "gd_lastupload_report";
  public static final String GOOGLEDRIVE_LASTUPLOAD_STATUS = "gd_lastupload_status";   
  public static final String GOOGLEDRIVE_LASTUPLOAD_TIME = "gd_lastupload_time";
  
  OnSharedPreferenceChangeListener bgServiceStatuslistener = new OnSharedPreferenceChangeListener() {

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      if(key.equals(GOOGLEDRIVE_LASTUPLOAD_STATUS)){
        boolean status = sharedPreferences.getBoolean(GOOGLEDRIVE_LASTUPLOAD_STATUS, true);
        String report = sharedPreferences.getString(GOOGLEDRIVE_LASTUPLOAD_REPORT, "");
        String uploadTarget = sharedPreferences.getString(GOOGLEDRIVE_LASTUPLOAD_TARGET, "");
        String time = sharedPreferences.getString(GOOGLEDRIVE_LASTUPLOAD_TIME, "");

        ServiceStatusChanged(uploadTarget, status, report, time);
      }
      
    }
    
  };

  // try local binding to FunfManager
  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {

      mBoundFunfManager = ((FunfManager.LocalBinder) service)
          .getManager();

      //after the component is bound to funfManager, we can get or create the GoogleDrivePipeline
      mPipeline = (GoogleDrivePipeline)getOrCreatePipeline();

      mIsBound = true;
      Log.i(TAG, "Bound to FunfManager");

    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundFunfManager = null;
      mIsBound = false;
      Log.i(TAG, "Unbind FunfManager");

    }
  };


  /*
 * create a pipleline by giving a json configuration to FunfManger,
 * and get back the handle
 */
  private Pipeline getOrCreatePipeline() {
    // try to get the pipeline, if not create a pipeline configuration here
    //<string name="mainPipelineConfig">{"@type":"edu.mit.dig.funftest.MainPipeline"}</string>
    // try

    Log.i(TAG, "Try to get pipeline from FunfMananger:" + mBoundFunfManager.toString());
    Pipeline pipeline = mBoundFunfManager.getRegisteredPipeline(pipelineName);


    if(pipeline == null){
      Log.i(TAG, "We don't have the pipeline name:" + pipelineName + " ,try to create a new one");
      String pipeConfigStr = "{\"@type\":\"com.google.appinventor.components.runtime.GoogleDrivePipeline\"}";

      // add to funfManager by calling this new function, it will create Pipeline and register to FunfManger
      // note that if you createPipeline, it will automatically be registered with the manager
      mBoundFunfManager.createPipeline(pipelineName, pipeConfigStr);
      Log.i(TAG, "just created pipeline with name:" + pipelineName);
      return mBoundFunfManager.getRegisteredPipeline(pipelineName);

    }

    return pipeline;

  }

  
  /*
   * GoogleDrive component, similar to the dropbox component, will be bound to two services 
   * 1. FunfManager service(for scheduling repeating tasks)
   * 2. GoogleUploadService (for uploading the data to Google Drive)
   */
  
  public GoogleDrive(ComponentContainer container) {

    super(container.$form());
    this.container = container;
    handler = new Handler();
    sharedPreferences = container.$context().getSharedPreferences(PREFS_GOOGLEDRIVE, Context.MODE_PRIVATE);
    // subscribe to preference changes, we use this to communicate for background service status changes
    // user can use serviceStatusChanged() event to obtain status of
    sharedPreferences.registerOnSharedPreferenceChangeListener(bgServiceStatuslistener);
    accessTokenPair = retrieveAccessToken();
    mainUIThreadActivity = container.$context();
    Log.i(TAG, "Package name:" + mainUIThreadActivity.getApplicationContext().getPackageName());

    if (!Launcher.isLaunched()) {
      Log.i(TAG, "firstTime launching funManger");
      Launcher.launch(mainUIThreadActivity);
    }


    doBindService();

    REQUEST_CHOOSE_ACCOUNT = form.registerForActivityResult(this);
    REQUEST_AUTHORIZE = form.registerForActivityResult(this);

    this.gdFolder = GoogleDrive.DEFAULT_GD_FOLDER;

    credential = GoogleAccountCredential.usingOAuth2(mainUIThreadActivity, DriveScopes.DRIVE);
    form.registerForOnStop(this);
    form.registerForOnDestroy(this);
    form.registerForOnResume(this);
    
    
  }
  void doBindService() {

    mainUIThreadActivity.bindService(new Intent(mainUIThreadActivity,
        FunfManager.class), mConnection, Context.BIND_AUTO_CREATE);
    Log.i(TAG,
        "FunfManager is bound, and now we could have register dataRequests");


  }
  
  void doUnbindService() {
    if (mIsBound) {

      // Detach our existing connection.
      mainUIThreadActivity.unbindService(mConnection);
    }
  }


  @Override
  public void onResume() {
    Log.i(TAG, "I got resumed, mIsBound:" + mIsBound);
    
  }
  
  @Override
  public void onStop(){
    Log.i(TAG, "My form: " + mainUIThreadActivity.toString() + " got stopped");

  }



  @Override
  public void onDestroy() {

    if (mIsBound) {
      doUnbindService();
    }

  }


  @Override
  public void resultReturned(int requestCode, int resultCode, Intent data) {
    // When the authentication from Google chooseAccount is back
    Log.i(TAG, "resultReturned.... " + resultCode);
    final Intent returnData = data;
    // if it's returning from choose account
    if (requestCode == REQUEST_CHOOSE_ACCOUNT) {
      if (resultCode == Activity.RESULT_OK && data != null
          && data.getExtras() != null) {
        
        setUpDriveService(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
               
      } else {
        // TODO: Should not happen
      }

    }// Below happens in setUpDriveService after UserRecoverableAuthException 
    if (requestCode == REQUEST_AUTHORIZE) {
      if (resultCode == Activity.RESULT_OK) {

        String accountName = data
            .getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        String accessToken = data.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        saveAccessToken(new AccessToken(accountName, accessToken));

      } else {
        mainUIThreadActivity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_CHOOSE_ACCOUNT);
      }

     
    }
  }
  
/*
 * This should only be done once in the main UI. Once we have authorized the app, the 
 * GoogleAccountCredential will automatically refresh token with Google Play service if it expires
 */
  private void setUpDriveService(String accountName) {

    final String mAccountName = accountName;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        String token = "";
        credential.setSelectedAccountName(mAccountName);
        try {
          Log.i(TAG, "before getToken()... ");
          token = credential.getToken();
        } catch (UserRecoverableAuthException e) {
          // if the user has not yet authorized
          Log.i(TAG, "in userRecoverableAuthExp... ");
          // this means that the user has never grant permission to this app before
          UserRecoverableAuthException exception = (UserRecoverableAuthException) e;
          Intent authorizationIntent = exception.getIntent();
          mainUIThreadActivity.startActivityForResult(authorizationIntent,
              REQUEST_AUTHORIZE);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (GoogleAuthException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        Log.i(TAG, "before build drive service... ");
        saveAccessToken(new AccessToken(mAccountName, token));
        service = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
            new GsonFactory(), credential).build();
        Log.i(TAG, "after drive service... ");
        // tell the mainUI that we are done
        handler.post(new Runnable() {
          @Override
          public void run() {
            IsAuthorized();
          }
        });
      }
    });
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
    Log.i(TAG, "call isAuthorized");
    EventDispatcher.dispatchEvent(this, "IsAuthorized");
  }


  /**
   * Indicates whether the user has specified that the component could only 
   * use Wifi to upload file(s). If this value is set to False, the GoogleDrive
   * uploader will use either Wifi or 3G/4G dataservice, whichever is available.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
  @SimpleProperty(description = "If this value is set to False, the GoogleDrive " +
  		"uploader will use either Wifi or 3G/4G dataservice, whichever is available")
  public void WifiOnly(boolean wifiOnly) {

    if (this.wifiOnly != wifiOnly)
      this.wifiOnly = wifiOnly;

    if(mPipeline!=null){

      mPipeline.setWifiOnly(this.wifiOnly);
    }
  }
  
  /**
   * Indicates whether the user has specified that the GoogleDrive component could only 
   * use Wifi to upload data
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "uploader will" +
  		" use either Wifi or 3G/4G dataservice, whichever is available")
  public boolean WifiOnly() {
    return wifiOnly;
  }
    
  /**
   * Copy the public master app scripts
   */
  @SimpleFunction(description = "Copy the master app scripts to your Google Drive root. It" +
  		"will return true if successful, otherwise false.")
  public void CopyFile(String googleDocId) {
	final String id = googleDocId;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
	    File copiedFile = new File();
	    copiedFile.setMimeType("application/vnd.google-apps.folder");
	    copiedFile.setParents(
	            Arrays.asList(new ParentReference().setId(gdFolder_id)));
	    try {
	      service.files().copy(id, copiedFile).execute();
	    } catch (IOException e) {
	      System.out.println("An error occurred: " + e);
	    }
      }
    });
  }
  
  /**
   * Copy the public master app scripts
   */
  @SimpleFunction(description = "Create a folder witht the given name.")
  public void CreateFolder(String folderName) {
	final String FolderName = folderName;
    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
	    File body = new File();
	    body.setTitle(FolderName);
	    body.setMimeType("application/vnd.google-apps.folder");
	    try {
	    	File folder = service.files().insert(body).execute();
	    	gdFolder_id = folder.getId();
	    } catch (IOException e) {
	      System.out.println("An error occurred: " + e);
	    }
      }
    });
  }
  
  /**
   * Start OAuth2 Authentication to ask for user's permission for using Google Drive
   */
  @SimpleFunction(
      description = "Start the Authorization process to ask the user for permission to access" +
      		"his or her Google Drive. Need to do it at least once, before using the " +
      		"Google Drive APIs")
  public void Authorize() {
    // we will not use OAuth2Helper here, the newest version of Google-api has taken care the flow for us
    // This will help us getting the main Google Account name, and auth token for the first time
    // we will persist these two in sharedPreference. 
    Log.i(TAG, "Start Authorization");

  	// check if we have choose the account already
  	String accountName = sharedPreferences.getString(PREF_ACCOUNT_NAME, "");
  	if(accountName.isEmpty()){
  	  mainUIThreadActivity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_CHOOSE_ACCOUNT);
  	}
  	else{
  	  setUpDriveService(accountName);
  	}
    
  }
  
  /**
   * Check whether we already have already the Google Drive access token
   */
  @SimpleFunction(
      description = "Checks whether we already have access token already, " +
      		"if so, return True")
  public boolean CheckAuthorized() {
    String accountName =  accessTokenPair.accountName;
    String token =  accessTokenPair.accessToken;
    if (accountName.isEmpty() || token.isEmpty()) {
      return false;
    }
    else
      return true;

  } 
  
  /**
   * Remove authentication for this app instance
   */
  @SimpleFunction(
      description = "Removes Google Drive authorization from this running app instance")
  public void DeAuthorize() {
    accessTokenPair = null;
    saveAccessToken(accessTokenPair);
  } 
  
  private void saveAccessToken(AccessToken accessToken) {
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    if (accessToken == null) {
      sharedPrefsEditor.remove(PREF_ACCOUNT_NAME);
      sharedPrefsEditor.remove(PREF_AUTH_TOKEN);
    } else {
      sharedPrefsEditor.putString(PREF_ACCOUNT_NAME, accessToken.accountName);
      sharedPrefsEditor.putString(PREF_AUTH_TOKEN, accessToken.accessToken);
      Log.i(TAG, "Save Google Access Token and Account" + accessToken.accountName + ", " + accessToken.accessToken);
    }
    sharedPrefsEditor.commit();
    this.accessTokenPair = accessToken; //update reference to local accessTokenPair
  }
  
  
  public class AccessToken  {
    public final String accessToken;
    public final String accountName;

    public AccessToken(String accountName, String accessToken) {
        this.accountName= accountName;
        this.accessToken = accessToken;
    }
}
  private AccessToken retrieveAccessToken() {
    String accountName = sharedPreferences.getString(OAuth2Helper.PREF_ACCOUNT_NAME, "");
    String accessToken = sharedPreferences.getString(OAuth2Helper.PREF_AUTH_TOKEN, "");
    if (accountName.length() == 0 || accessToken.length() == 0) {
      return new AccessToken("",""); // returning an accessToken with both params empty
    }
    return new AccessToken(accountName, accessToken);
  }
  
  /*
   * Set the Google Drive folder to which the file(s) will be uploaded 
   */
  
  @SimpleProperty(description = "Set up the Google Drive" +
      "folder in which the uploaded file(s) will be placed. If not set, " +
      "default will be the root of Google Drive" +
      "", category = PropertyCategory.BEHAVIOR )
    public void GoogleDriveFolder(String folderName){
    
    this.gdFolder = folderName;    
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();   
    sharedPrefsEditor.putString(GoogleDrive.GD_FOLDER , this.gdFolder);
    sharedPrefsEditor.commit();
  }
  
  /*
   * Indicate the folder(directory) to where the uploaded file(s) will be placed
   */

  @SimpleProperty(description = "Return the Google Drive folder(directory) to where the uploaded" +
      "file(s) will be placed", category = PropertyCategory.BEHAVIOR )
  public String GoogleDriveFolder(){
    return this.gdFolder;
    
  }

  public Class<? extends UploadService> getUploadServiceClass() {
    return GoogleDriveUploadService.class;
  }
  
  
  /*
   * Upload a file to Google Drive once, using AsyncTask
   */
  
//  new QueryProcessorV1(activity).execute(query);
  
  /*
   * Use AsyncTask to do the uploading task (using GoogleDriveArchive class)
   * (this will not support doing schedule task in the background)
   * Before performing the upload task, the program needs to get authorized by the author
   */
  private class AsyncUploader extends AsyncTask<String, Void, Boolean>{
    private static final String TAG = "AysncUploader";
    private final Activity activity; // The main list activity
    
    AsyncUploader(Activity activity) {
      this.activity = activity;
      
    }

    @Override
    protected Boolean doInBackground(String... params) {
      Log.i(TAG, "Starting doInBackground " + params[0]);
      String filepath = params[0];
      boolean uploadResult = false;
      Log.i(TAG, "upload filepath:" + filepath);
      java.io.File uploadFile = new java.io.File(filepath);

      GoogleDriveArchive gdArchive = new GoogleDriveArchive(activity, gdFolder);
      try {
        uploadResult = gdArchive.uploadDataFile(uploadFile);
        Log.i(TAG, "upload success or failed?" + uploadResult);
      } catch (Exception e) {

        //TODO Read out types of the exception and call UI method to display
        //Could be IOException, GoogleAuthException.... 
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

        UploadDone(resultSuccessful);
    } 
  }

  /**
   * Indicates when the upload task has been successful done.
   */
  @SimpleEvent(description =
      "This event is raised after the program calls " +
          "<code>UploadData</code> if the upload task was done. Use this indicator" +
          "to tell the user that the upload task is finished")
  public void UploadDone(boolean successful) {
    Log.i(TAG, "uploadDone");
    EventDispatcher.dispatchEvent(this, "UploadDone", successful);
  }
  
  // this is the helper function called within the Uploader AsyncTask
  private void displayErrorMessage(Exception e){
   
    try{
      throw e;
    } catch (GoogleJsonResponseException e1) {
      GoogleJsonError error = e1.getDetails();

      Log.e(TAG, "Error code: " + error.getCode());
      Log.e(TAG, "Error message: " + error.getMessage());
      if(error.getCode() == 401){
        form.dispatchErrorOccurredEvent(GoogleDrive.this, "UploadData",
            ErrorMessages.ERROR_GOOGLEDRIVE_INVALID_CREDENTIALS);
        
      }
      if (error.getCode() == 403 && (error.getErrors().get(0).getReason().equals("appAccess"))){
        form.dispatchErrorOccurredEvent(GoogleDrive.this, "UploadData",
            ErrorMessages.ERROR_GOOGLEDRIVE_NOT_GRANT_PERMISSION);
      }
      
      if (error.getCode() == 403 && (error.getErrors().get(0).getReason().equals("appNotConfigured"))){
        form.dispatchErrorOccurredEvent(GoogleDrive.this, "UploadData",
            ErrorMessages.ERROR_GOOGLEDRIVE_APP_CONFIG_ERROR);
        
      }
      if (error.getCode() == 403 && (error.getErrors().get(0).getReason().equals("appBlacklisted"))){
        form.dispatchErrorOccurredEvent(GoogleDrive.this, "UploadData",
            ErrorMessages.ERROR_GOOGLEDRIVE_APP_BLACKLIST);        
      }

      // More error information can be retrieved with error.getErrors().
    }
      catch (IOException exception){
      form.dispatchErrorOccurredEvent(GoogleDrive.this, "UploadData",
          ErrorMessages.ERROR_GOOGLEDRIVE_IO_EXCEPTION);

    } catch (Exception exceptfinal) {
      // TODO Auto-generated catch block
      form.dispatchErrorOccurredEvent(GoogleDrive.this, "UploadData",
          ErrorMessages.ERROR_GOOGLEDRIVE_EXCEPTION);
    }

  }



  /*
   * Upload a file to Google Drive once
   */
  @SimpleFunction(description = "Upload the file(s)" +
      "as specified <code>target</code> (can be the file path of a single file or a folder. " +
      "Specify the destination folder in Google Drive in variable <code>GoogleDriveFolder</code>")
  public void UploadData(String target, String GoogleDriveFolder) throws IOException {
	
	//TODO: show error message to the user if no authorized
	boolean isAuthorized = CheckAuthorized();
	if(!isAuthorized){
      form.dispatchErrorOccurredEvent(this, "UploadData",
          ErrorMessages.ERROR_GOOGLEDRIVE_NEEDLOGIN);
      return;
	}
	
	
    //overwrite gdFolder and save to preference
    this.gdFolder = GoogleDriveFolder;
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    sharedPrefsEditor.putString(GoogleDrive.GD_FOLDER , this.gdFolder);
    sharedPrefsEditor.commit();
    ///////////

    String filePath = "";
    if(target.startsWith("file:")){
      try { //convert URL string to URI and to real path : file:///sdcard --> /sdcard
        filePath = new java.io.File(new URL(target).toURI()).getAbsolutePath();
      }catch (IllegalArgumentException e) {
    	Log.i(TAG, "IllegalArgument : " + e.getStackTrace());
        throw new IOException("Unable to determine file path of file url " + target);
      } catch (URISyntaxException e) {
    	Log.i(TAG, "RISyntaxException error : " + e.getStackTrace());
        e.printStackTrace();
      }
    }
    else {
      Log.i(TAG, "target : " + target);
      filePath = target;
    }

    //try using AsyncTask

    new AsyncUploader(mainUIThreadActivity).execute(filePath);

  }

  @SimpleFunction(description = "Add a background task for uploading file(s) to Google Drive. " +
      "The task will upload the file(s) that is specified in <code>target</code> to " +
      "the Google drive folder specified in <code>driveFolder</code> with every <code>" +
      "period</code> interval. Save <code>taskName</code> for later reference for removing the task")
  public void AddScheduledTask(String taskName, String target,
                                     String driveFolder, int period){
	
	//TODO: show error message to the user if no authorized
	boolean isAuthorized = CheckAuthorized();
	if(!isAuthorized){
      form.dispatchErrorOccurredEvent(this, "UploadData",
          ErrorMessages.ERROR_GOOGLEDRIVE_NEEDLOGIN);
      return;
	}
	
    if (mPipeline != null) {

      mPipeline.addUploadTask(taskName, target, driveFolder, period);

      // make the service on the foreground
      if (!Launcher.isForeground()) {
        Log.i(TAG, "make funfManager in the foreground....");
        Launcher.startForeground(mainUIThreadActivity);

      }
    } else {
      Log.d(TAG,
          "AddScheduledTask, pipeline is null, funf is killed by the system.");

    }

  }


  @SimpleFunction(description = "Remove a background task for uploading files(s).")
  public void RemoveScheduledTask(String taskName){

    if(mPipeline != null){
      mPipeline.removeUploadTask(taskName);
      // if all uploading tasks are removed.

      if (!mBoundFunfManager.hasRegisteredJobs()){
        Log.i(TAG, "make funfManager stop being foreground");
        Launcher.stopForeground(mainUIThreadActivity);
      }
    } else{
      Log.d(TAG,
          "RemoveScheduledTask, pipeline is null, funf is killed by the system.");

    }

  }

  
  /*
   * Give the component an event listener that gives the recent status and log of the background service
   */
  @SimpleEvent(description = "This event is raised when background upload tasks status for a <target>" +
      " has changed. If successful, <code>successful</code> will return true, else it returns false" +
  		" and <code>log</code> returns the error messages")

  public void ServiceStatusChanged(String target, boolean successful, String log, String time) {
    Log.i(TAG, "ServiceStatusChanged, target:" + target + ", " + successful + ", " + log);
    EventDispatcher.dispatchEvent(this, "ServiceStatusChanged", target, successful, log, time);
  }


  /*
   * Getters for task info using taskName
   */
  @SimpleFunction(description = "Obtain the information for a upload task that's running in the background." +
      "Will return a list containing the target, google drive folder, and period of the upload task. If the specified task " +
      "does not exist then the return list will be empty")
  public YailList GetUploadTaskInfo(String taskName){
    List<Object> arrlist = new ArrayList<Object>();

    if(mPipeline != null){
      String targetFile = mPipeline.getUploadTarget(taskName);
      if(targetFile.isEmpty())
        return  YailList.makeList(arrlist);
      String gdFolder = mPipeline.getUploadGoogleDriveFolder(taskName);
      int period = mPipeline.getUploadPeriod(taskName);
      arrlist.add(targetFile);
      arrlist.add(gdFolder);
      arrlist.add(period);

    }
    return YailList.makeList(arrlist); //if mPipeline is not ready then return an empty list

  }
}
