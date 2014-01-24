package com.google.appinventor.components.runtime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.storage.UploadService;
import org.json.JSONException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A class that let GoogleDrive component to delegate task for scheduling uploads
// xxxPipleline.java is the type of class that tailors for scheduling tasks in Funf architecture
// Notes: The pipeline will allow a GoogleDrive component to schedule multiple tasks
// for example, some app might have two uploading schedules to backup their photos

public class GoogleDrivePipeline implements Pipeline{

  private SharedPreferences sharedPreferences;
  // only one pipeline for all the GoogleDrive components.
  public static final String pipelineName = "GoogleDrivePipeline";

  protected static final String ACTION_UPLOAD_DATA = "UPLOAD_DATA";
  private static final String TAG = "GoogleDrivePipline";

  // stores ongoing uploading tasks. Changes will be saved to sharedPreferences as well.
  private Map<String, Integer> activeUploadingTasks = new HashMap<String, Integer>();
  // stores the mapping between upload tasks and the targeted file(folder)
  private Map<String, String> uploadTargets = new HashMap<String, String>();
  //store mapping between upload tasks and the destination Google Drive folders
  private Map<String, String> uploadGDFolders = new HashMap<String, String>();
  protected static final String ACTIVE_TASKS = "active.tasks";
  protected static final String TASK_TARGETS = "task_target";
  protected static final String TASK_GDFOLDERS = "task_gdFolder";

  // by default, all the uploading task for a pipeline will be wifiOnly
  private boolean wifiOnly = false;




//	private boolean scheduledUploadEnabled;
//	private int uploadPeriod;

  private FunfManager funfManager;

	@Override
	public void onCreate(FunfManager manager) {
		// TODO Auto-generated method stub
    funfManager = manager;
    Log.i(TAG, "Created GoogleDrivePipeline from funfManager:" + manager.toString() + ",at: " + System.currentTimeMillis());

    sharedPreferences = manager.getSharedPreferences("GoogleDrivePipeline", Context.MODE_PRIVATE);

    // if funf recreate the pipeline, we reset all the upload tasks according to what are saved in the prefs
    initialization();


	}

  // a small helper function to iterate through the list
  private String getTargetFromPrefs(String taskName){

    String jsonArrayTargets = (String)getPreference(TASK_TARGETS);


    if (!jsonArrayTargets.isEmpty()){
      try {
        List<Object> targets = (List<Object>)JsonUtil.getObjectFromJson(jsonArrayTargets);
        // will get a list of lists (key, value)
        // it could be {"task_target" : []}
        for (int i = 0; i > targets.size(); i++){
          List keyVal = (List)targets.get(i);
          String task = (String) keyVal.get(0);
          String target = (String) keyVal.get(1);
          Log.i(TAG, "key(taskName):" + task);
          Log.i(TAG, "value(period):" + target);
          if(task.equals(taskName)) {

            return target;
          }

        }
        return ""; //shouldn't happen because we save all three variables together
      } catch (JSONException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }
    return "";  //shouldn't happen

  }

  // a helper function to get Gdfolder mapping to each upload task
  private String getGDFolderFromPrefs(String taskName){
    String jsonArrayGDFolders = (String)getPreference(TASK_GDFOLDERS);

    if (!jsonArrayGDFolders.isEmpty()){
      try {
        List<Object> gdfolders = (List<Object>)JsonUtil.getObjectFromJson(jsonArrayGDFolders);
        // will get a list of lists (key, value)
        // it could be {"task_target" : []}
        for (int i = 0; i > gdfolders.size(); i++){
          List keyVal = (List)gdfolders.get(i);
          String task = (String) keyVal.get(0);
          String gdfolder = (String) keyVal.get(1);
          Log.i(TAG, "key(taskName):" + task);
          Log.i(TAG, "value(gdFolder):" + gdfolder);
          if(task.equals(taskName)) {
            return gdfolder;
          }

        }
        return ""; //shouldn't happen because we save all three variables together
      } catch (JSONException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }
    return "";  //shouldn't happen

  }


  private void initialization(){
    // initialize tasks
    String jsonArrayStr = (String)getPreference(ACTIVE_TASKS);
    // if the pipeline is being created for the first time, then there's no active tasks for this pipeline
    Log.i(TAG, "ACTIVE upload tasks:" + jsonArrayStr);
    if (!jsonArrayStr.isEmpty()){
      try {
        List<Object> periods = (List<Object>)JsonUtil.getObjectFromJson(jsonArrayStr);
        // will get a list of lists (key, value)
        // it could be {"active.sensors" : []}
        for (int i = 0; i > periods.size(); i++){
          List keyVal = (List)periods.get(i);
          String taskName = (String) keyVal.get(0);
          Integer period = (Integer) keyVal.get(1);
          Log.i(TAG, "key(taskName):" + taskName);
          Log.i(TAG, "value(period):" + period);
          //obtain the target file and google drive folder for this taskname
          String target = getTargetFromPrefs(taskName);
          String gdFolder = getGDFolderFromPrefs(taskName);

          addUploadTask(taskName, target, gdFolder, period);
        }
      } catch (JSONException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }

  }


	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRun(String action, JsonElement config) {
		// TODO Auto-generated method stub
    //get the taskName

    String taskName = action.substring(ACTION_UPLOAD_DATA.length());
    String uploadTarget = uploadTargets.get(taskName);
    String gdFolder = uploadGDFolders.get(taskName);
    if(uploadTarget != null && gdFolder != null){
      uploadService(uploadTarget, gdFolder);

    }
    //some logging for debugging
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
    Date date = new Date();
    String currentDatetime = dateFormat.format(date);
    Log.i(TAG, "Run pipe's action UPLOAD_DATA at:" + System.currentTimeMillis() + "," + currentDatetime);

	}

  private void uploadService(String archiveName, String gdFolder){
    // archiveName is the file that will be remotely archived in Funf, which means that it will be uploaded to the remote site
    Log.i(TAG, "Start uploadService. Target:" + archiveName + ", to Google Drive folder: " + gdFolder);
    Intent i = new Intent(funfManager, getUploadServiceClass());
    i.putExtra(UploadService.ARCHIVE_ID, archiveName);
    i.putExtra(GoogleDrive.GD_FOLDER, gdFolder);
    i.putExtra(GoogleDriveUploadService.FILE_TYPE, GoogleDriveUploadService.REGULAR_FILE);
    i.putExtra(UploadService.REMOTE_ARCHIVE_ID, GoogleDriveArchive.GOOGLEDRIVE_ID);
    i.putExtra(UploadService.NETWORK,(this.wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
        : UploadService.NETWORK_ANY);
    funfManager.startService(i);

  }

  public Class<? extends UploadService> getUploadServiceClass() {
    return GoogleDriveUploadService.class;
  }


  public java.util.Set<String> getActiveTasks() {
    return activeUploadingTasks.keySet();
  }

  public int getUploadPeriod(String taskName) {
    Integer period = activeUploadingTasks.get(taskName);

    return (period == null)? 0:period.intValue();
  }

  public String getUploadTarget(String taskName){
     String target = uploadTargets.get(taskName);
    return (target == null)? "" : target;
  }

  public String getUploadGoogleDriveFolder(String taskName){
    String gdFolder = uploadGDFolders.get(taskName);
    return (gdFolder == null)? "" : gdFolder;

  }

  public boolean getWifiOnly(){
    return this.wifiOnly;

  }

  public void setWifiOnly(boolean wifiOnly){
    this.wifiOnly = wifiOnly;

  }


  /*
   *  reuse codes from TinyDB
   */
  private void savePreference(String tag, Object valueToStore){
    //every time we add /update/remove sensor, we will also write to the sharedPreference as well.
    SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    try {
      sharedPrefsEditor.putString(tag, JsonUtil.getJsonRepresentation(valueToStore));
      Log.i(TAG, "What we save:" + JsonUtil.getJsonRepresentation(valueToStore));
      sharedPrefsEditor.commit();
    } catch (JSONException e) {
      throw new YailRuntimeError("Value failed to convert to JSON.", "JSON Creation Error.");
    }


  }

  private Object getPreference(String tag){
    try {
      String value = sharedPreferences.getString(tag, "");
      // If there's no entry with tag as a key then return the empty string.
      return (value.length() == 0) ? "" : JsonUtil.getObjectFromJson(value);
    } catch (JSONException e) {
      throw new YailRuntimeError("Value failed to convert from JSON.", "JSON Creation Error.");
    }
  }

 /*
  * add a new upload task to FunfManager. Once it's added, it will be enabled right away which means that
  * the upload task will be executed once immediately.
  */
  public void addUploadTask(String taskName, String targetFile, String googleDriveFolder, int period) {
    // if we already have this sensor, then do nothing.
    if (activeUploadingTasks.containsKey(taskName)) {
      ;// do nothing
    } else {
      Log.i(TAG, "Registering new upload tasks.");

      activeUploadingTasks.put(taskName, new Integer(period));
      uploadTargets.put(taskName, targetFile);
      uploadGDFolders.put(taskName, googleDriveFolder);

      Schedule uploadPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(period), BigDecimal.ZERO, false, false);

      funfManager.registerPipelineAction(this, ACTION_UPLOAD_DATA + taskName, uploadPeriod);
      //save to preference in case FunfManager is killed and recreated. Need to recreate the pipeline
      savePreference(ACTIVE_TASKS, activeUploadingTasks);
      savePreference(TASK_TARGETS, uploadTargets);
      savePreference(TASK_GDFOLDERS, uploadGDFolders);
    }

  }

  public void removeUploadTask(String taskName){

    if (activeUploadingTasks.containsKey(taskName)) {
      Log.i(TAG, "Un-Registering upload tasks.");

      funfManager.unregisterPipelineAction(this, ACTION_UPLOAD_DATA + taskName);

      activeUploadingTasks.remove(taskName);
      uploadTargets.remove(taskName);
      uploadGDFolders.remove(taskName);

      savePreference(ACTIVE_TASKS, activeUploadingTasks);
      savePreference(TASK_TARGETS, uploadTargets);
      savePreference(TASK_GDFOLDERS, uploadGDFolders);

    } else {
      Log.i(TAG, taskName + " is not active");
    }


  }




}
