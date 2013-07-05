package com.google.appinventor.components.runtime;



import java.io.IOException;
import java.math.BigDecimal;

import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;


import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
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
import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Launcher;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.storage.UploadService;

//TODO: rename this component and the dropbox component to GoogleDriveUploader
@DesignerComponent(version = YaVersion.GOOGLE_DRIVE_COMPONENT_VERSION,
    description = "This component can upload file(s) to Google Drive.",
    category = ComponentCategory.FUNF,
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
implements ActivityResultListener, Component, Pipeline, OnResumeListener, OnStopListener, OnDestroyListener{
  
  private static final String TAG = "GoogleDrive";

  protected boolean mIsBound = false;
  public static final String GOOGLEDRIVE_PIPE_NAME = "googledrive";
  private final int pipeId;
  private static final AtomicInteger snextGoogleDriveID = new AtomicInteger(1);
  
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
  
  //binding to GoogleDriveUploadService and FunfManager Service
  
  protected GoogleDriveUploadService mBoundGDService= null;
  protected FunfManager mBoundFunfManager = null;
  protected static final String ACTION_UPLOAD_DATA = "UPLOAD_DATA";
  
  private static final long SCHEDULE_UPLOAD_PERIOD = 7200; //default period for uploading task 
  // for periodic upload
  private boolean enablePeriodicUploadFolder = false;
  private String uploadTarget = null;


  private AccessToken accessTokenPair;
  
  private long upload_period;
  private boolean wifiOnly = false;
  
  private static Drive service;
  private GoogleAccountCredential credential;
  
  /*
   * Because we run the upload service on the background periodically, we need a way to understand if there's 
   * anything wrong with the background work (e.g. network problem, quota exceed,..etc) and decide to terminate
   * the and remove the schedule task or not. Using lastupload_status to report if successful or not, 
   * if not successful, the error message will placed in the lastupload_report
   */
  public static final String GOOGLEDRIVE_LASTUPLOAD_REPORT = "gd_lastupload_report";
  public static final String GOOGLEDRIVE_LASTUPLOAD_STATUS = "gd_lastupload_status";   
  public static final String GOOGLEDRIVE_LASTUPLOAD_TIME = "gd_lastupload_time";
  
  OnSharedPreferenceChangeListener bgServiceStatuslistener = new OnSharedPreferenceChangeListener() {

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      // TODO Auto-generated method stub
      if(key.equals(GOOGLEDRIVE_LASTUPLOAD_STATUS)){
        boolean status = sharedPreferences.getBoolean(GOOGLEDRIVE_LASTUPLOAD_STATUS, true);
        String log = sharedPreferences.getString(GOOGLEDRIVE_LASTUPLOAD_REPORT, "");

        ServiceStatusChanged(status, log);
      }
      
    }
    
  };

  // try local binding to FunfManager
  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {

      mBoundFunfManager = ((FunfManager.LocalBinder) service)
          .getManager();
      
      registerSelfToFunfManager(); 
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
   * GoogleDrive component, similar to the dropbox component, will be bound to two services 
   * 1. FunfManager service(for scheduling repeating tasks)
   * 2. GoogleUploadService (for uploading the data to Google Drive)
   */
  
  public GoogleDrive(ComponentContainer container) {
    // TODO Auto-generated constructor stub
    super(container.$form());
    this.container = container;
    handler = new Handler();
    sharedPreferences = container.$context().getSharedPreferences(PREFS_GOOGLEDRIVE, Context.MODE_PRIVATE);
    accessTokenPair = retrieveAccessToken();
    mainUIThreadActivity = container.$context();
    Log.i(TAG, "Package name:" + mainUIThreadActivity.getApplicationContext().getPackageName());
//    // start a FunfManager Service
//    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
//    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the
    // schedule)
    if (!Launcher.isLaunched()) {
      Log.i(TAG, "firstTime launching funManger");
      Launcher.launch(mainUIThreadActivity);
    }

    pipeId = generatePipelineId(); // generate pipelineID for each new Google Drive instance.
    // it's possible that the user has two GoogleDrive instance in two different screen

    doBindService();
    this.upload_period = GoogleDrive.SCHEDULE_UPLOAD_PERIOD;
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
      // first unregister Pipeline action
      unregisterPipelineActions();
      // unregister self
      mBoundFunfManager.unregisterPipeline(GOOGLEDRIVE_PIPE_NAME + pipeId);
      // Detach our existing connection.
      mainUIThreadActivity.unbindService(mConnection);
    }
  }
  
  
  public void unregisterPipelineActions() {
    // TODO Auto-generated method stub
     mBoundFunfManager.unregisterPipelineAction(this, ACTION_UPLOAD_DATA);   
    
  }

  private static int generatePipelineId(){
    return snextGoogleDriveID.incrementAndGet();

  }


  @Override
  public void onResume() {
    // TODO Auto-generated method stub
    Log.i(TAG, "I got resumed, mIsBound:" + mIsBound);
    
  }
  
  @Override
  public void onStop(){
    Log.i(TAG, "My form: " + mainUIThreadActivity.toString() + " got stopped");

  }

  @Override
  public void onCreate(FunfManager manager) {
    // FunfManager manager
    // This function will run once whenever FunfManager.registerPipeline() is called
    //do nothing for now
    
    
  }

  @Override
  public void onDestroy() {
    // TODO Auto-generated method stub

    if (mIsBound) {
      doUnbindService();
    }

  }

  @Override
  public void onRun(String action, JsonElement config) {
    // TODO Auto-generated method stub
    if (ACTION_UPLOAD_DATA.equals(action)) {
      // Do something else
      if (uploadTarget != null){
        uploadService(uploadTarget);
      }
      SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
      Date date = new Date();
      String currentDatetime = dateFormat.format(date);
      Log.i(TAG, "Run pipe's action UPLOAD_DATA at:" + System.currentTimeMillis() + "," + currentDatetime);

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

     
    }/// testing code///
//    if(requestCode == REQUEST_CAPTURE){
//      if (resultCode == Activity.RESULT_OK){
//        saveFileToDrive();
//
//      }
//    }
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
  
  /*
   * After we bind to FunfManger, we have to register self to Funf as a Pipeline. 
   * This is for later to be wakened up and do previously registered actions 
   */
  private void registerSelfToFunfManager(){
    Log.i(TAG, "register this class as a Pipeline to FunfManger: "+ GOOGLEDRIVE_PIPE_NAME + pipeId);
    mBoundFunfManager.registerPipeline(GOOGLEDRIVE_PIPE_NAME + pipeId, this);
    
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
   * Upload a file to Google Drive, using AsyncTask
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
      // TODO Auto-generated method stub
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
        dialog.dismiss();
        UploadDone(resultSuccessful);
    } 
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

  /**
   * Indicates when the upload task has been successful done.
   */
  @SimpleEvent(description =
               "This event is raised after the program calls " +
               "<code>UploadData</code> if the upload task was done.")
  public void UploadDone(boolean successful) {
    Log.i(TAG, "uploadDone");
    EventDispatcher.dispatchEvent(this, "UploadDone", successful);
  }
  
  // this function will be used for schedule task
  private void uploadService(String archiveName){
    Log.i(TAG, "Start uploadService...");
    Intent i = new Intent(mainUIThreadActivity, getUploadServiceClass());
    i.putExtra(UploadService.ARCHIVE_ID, archiveName);
    i.putExtra(GoogleDrive.GD_FOLDER, this.gdFolder);
    i.putExtra(GoogleDriveUploadService.FILE_TYPE, GoogleDriveUploadService.REGULAR_FILE);
    i.putExtra(UploadService.REMOTE_ARCHIVE_ID, GoogleDriveArchive.GOOGLEDRIVE_ID);
    i.putExtra(UploadService.NETWORK,(this.wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
          : UploadService.NETWORK_ANY);
    mainUIThreadActivity.startService(i);

  }
  
  
  
  /*
   * Upload a file to Google Drive
   */
  @SimpleFunction(description = "Uploads the file(s)" +
      "(as specified with its filepath) to Google Drive folder. ")
      
  public void UploadData(String filepath) throws IOException {

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
    
    //try using AsyncTask
    
    new AsyncUploader(mainUIThreadActivity).execute(filePath);

  }
  
  
  /*
   * Start and set interval for a re-occurring upload activity for uploading
   * file in a folder to Google Drive
   */
  @SimpleFunction(description = "Enable upload scheduled task based on specified filepath "
      + "of a folder locally in parameter <code>folderPath</code>. " 
      + "One use case is to upload all the save photos"
      + "in some SD folder periodically. The parameter <code>period</code> is in second.")
  public void ScheduleUpload(String folderPath, long period) {

    // will throw an exception if try to startPeriodUpload when there's one
    // on-going schedule Task
 
      this.enablePeriodicUploadFolder = true;
      this.upload_period = period;
      this.uploadTarget = folderPath;

      Schedule uploadPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.upload_period), BigDecimal.ZERO, false, false);

      mBoundFunfManager.registerPipelineAction(this, ACTION_UPLOAD_DATA,
          uploadPeriod);

  }
  
  
  @SimpleProperty (description = "Indicates whether there exists any schedule upload task")
  public boolean ScheduleUploadEnabled(){
    return this.enablePeriodicUploadFolder;
  }
  
  
  /*
   * Stop the current running schedule task. 
   */
  @SimpleFunction(description = "Stop the schedule upload task")
  public void StopScheduleUpload(){
    this.enablePeriodicUploadFolder = false;

    mBoundFunfManager.unregisterPipelineAction(this, ACTION_UPLOAD_DATA);
     
  }
  
  /*
   * Give the component an event listener that gives the recent status and log of the background service
   */
  @SimpleEvent(description = "This event is raised when upload service status has changed." +
  		" If successful, <code>successful</code> will return true, else it returns false" +
  		" and <code>log</code> returns the error messages")

  public void ServiceStatusChanged(boolean successful, String log) {
    Log.i(TAG, "ServiceStatusChanged:" + successful + ", " + log);
    EventDispatcher.dispatchEvent(this, "ServiceStatusChanged", successful, log);
  }
  
  @SimpleFunction(description = "Get the status of the status of the most recent schedule upload task")
  public boolean GetScheduleTaskStatus() {
    return sharedPreferences.getBoolean(GoogleDrive.GOOGLEDRIVE_LASTUPLOAD_STATUS, true);
    
  }
  
  @SimpleFunction(description = "Get the message log of the most recent schedule upload task")
  public String GetScheduleTaskLog(){
    return sharedPreferences.getString(GoogleDrive.GOOGLEDRIVE_LASTUPLOAD_STATUS , "");
    
  }
  
  @SimpleFunction(description = "Get the finished datetime of the last uploading task")
  public String GetScheduleTaskTime(){
    return sharedPreferences.getString(GOOGLEDRIVE_LASTUPLOAD_TIME, "");
  }

  
}
