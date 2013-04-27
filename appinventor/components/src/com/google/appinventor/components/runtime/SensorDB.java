package com.google.appinventor.components.runtime;

import java.io.File;
import java.math.BigDecimal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.DBTrustUtil;
import com.google.gson.JsonElement;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;

/* 
 * This is equivalent to the class "DatabaseService" in Funf library excepts we don't 
 * provide recording data explicitly in the App Inventor interface. (Because it should never be done
 * in the UI level to record sensor stream data which will cause performance issues)
 * The component will only provide archive, export, clear backup functions with scheduling. 
 * 1) archive: copy the sqlite db file and moved it to sd card, under /packageName/dbName/archive/ 
 *    (note: this function will delete the sqlite db as well)
 * 2) export: will export the sqlite db with specified format to sd card, under /packageName/export/
 * 3) clear backup: every time when we execute archive function, a copy will be put into /packageName/dbName/backup
 *    as well for backup purpose. A user (developer) may want to clear the backup files after a while.  
 */
@DesignerComponent(version = YaVersion.WIFISENSOR_COMPONENT_VERSION, description = "Non-visible component that provides method      s to backup and upload "
		+ "sensor db on the phone.", category = ComponentCategory.BASIC, nonVisible = true, iconName = "images/tinyDB.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, "
		+ "android.permission.ACCESS_NETWORK_STATE, "
		+ "android.permission.WAKE_LOCK, "
		+ "android.permission.READ_LOGS, "
		+ "android.permission.INTERNET")
@UsesLibraries(libraries = "funf.jar")
public class SensorDB extends AndroidNonvisibleComponent implements
OnDestroyListener, Pipeline{
	/*
	 * Binding to FunfMananger service
	 */

  protected FunfManager mBoundFunfManager = null;
  protected boolean mIsBound = false;

	private final String TAG = "SensorDB";
	public static final String PIPE_NAME = "SensorDB"; // this is used as the
	
														// dtatbase name

	protected static Activity mainUIThreadActivity;
	private String exportPath;
	protected static final String ACTION_ARCHIVE_DATA = "ARCHIVE_DATA";
  protected static final String ACTION_EXPORT_DATA = "EXPORT_DATA";
  protected static final String ACTION_CLEAR_BACKUP= "CLEAR_BACKUP";
  private static final long ARCHIVE_PERIOD = 3600; // archive database 60*60*24 (archive every 24 hour)
  private static final long EXPORT_PERIOD = 7200;//
  
  private boolean scheduleArchiveEnabled;
  private boolean scheduleExportEnabled;

  // note if use both archive and export, the period of export needs to be shorter than archive, because when 
  // archive is called, it will delete the original sqlite db file. 
  // ** Archive should be used together with the upload db function provided by other uploading component, such as 
  //    Dropbox.UploadDB(); 
  // ** Export should be used together with upload folder function provided by other uploading component, such as in 
  //    Dropbox.Upload();
  // We will provide archive folder path after archive
  
  
  private long archive_period;
  private long export_period;
	private String exportFormat;
  

	public SensorDB(ComponentContainer container) {
		super(container.$form());
		// TODO Auto-generated constructor stub
		// Set up listeners
		form.registerForOnDestroy(this);
		mainUIThreadActivity = container.$context();
		
    //set upload and archive periods
    archive_period = ARCHIVE_PERIOD;
    export_period = EXPORT_PERIOD;
    scheduleArchiveEnabled = false;
    scheduleExportEnabled = false;
    
    
    exportPath =  new File(Environment.getExternalStorageDirectory(), form.getPackageName()) + 
    		File.separator + "export";
    exportFormat = NameValueDatabaseService.EXPORT_CSV; // set the exporting format as csv by default
    // start local bind service FunfManger
    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the schedule)
    doBindService();
		
	}
	
  /**
   * Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
   * csv file(s) or JSON file(s). Each type of sensor data in the database will be export it as one file. 
   */
  @SimpleFunction(description = "Export Sensor Database as CSV files or JSON files. " +
  		"Input \"csv\" or \"json\" for exporting format")	
	public void Export(String format){
		Log.i(TAG, "Exporting DB as CSV files");
		this.exportFormat = format;

		
	}
  
  private void export(String format){

		Bundle b = new Bundle();
		b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, "SensorData");
		b.putString(NameValueDatabaseService.EXPORT_KEY, format);
		Intent i = new Intent(form, NameValueDatabaseService.class);
		i.setAction(DatabaseService.ACTION_EXPORT);
		i.putExtras(b);
		form.startService(i);
  }
  
  
  @SimpleFunction(description = "Archive Sensor Database to archive folder")	
	public void Archive(){
		
		Intent i = new Intent(form, NameValueDatabaseService.class);
		Log.i(TAG, "archiving data......");
		i.setAction(DatabaseService.ACTION_ARCHIVE);
		i.putExtra(DatabaseService.DATABASE_NAME_KEY, PIPE_NAME);
		form.startService(i);

	}
	
	 @SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public String FolderPath(){
		return this.exportPath;
		
		
	}
	 
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public String DBName(){
		return PIPE_NAME; 
		
	}
	
	
	@SimpleFunction(description = "Enable archive schedule task with specified period")
	public void ScheduleArchive(long period){
		this.scheduleArchiveEnabled = true;
		this.archive_period = period;

      // register pipeline action with bound FunfManger
    	
      Schedule archivePeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.archive_period), BigDecimal.ZERO, false, false);

    	mBoundFunfManager.registerPipelineAction(this, ACTION_ARCHIVE_DATA, archivePeriod);
	}
	
	@SimpleFunction(description = "Discable archive scheduled task")
	public void StopScheduleArchive(){
		
		this.scheduleArchiveEnabled = false;
		mBoundFunfManager.unregisterPipelineAction(this, ACTION_ARCHIVE_DATA);
		
	}
	
	@SimpleProperty(description = "Indicates whether the schedule archive task is current" +
			"enabled.", category = PropertyCategory.BEHAVIOR)
	public boolean ScheduleArchiveEnabled(){
		return this.scheduleArchiveEnabled; 
		
	}
	
	// schedule export
	
	@SimpleFunction(description = "Enable export db schedule task with specified period")
	public void ScheduleExport(long period){
		this.scheduleExportEnabled = true;
		this.export_period = period;
 

      // register pipeline action with bound FunfManger
    	
      Schedule exportPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.export_period), BigDecimal.ZERO, false, false);

    	mBoundFunfManager.registerPipelineAction(this, ACTION_EXPORT_DATA, exportPeriod);
	}
	
	@SimpleFunction(description = "Discable export scheduled task")
	public void StopExportArchive(){
		
		this.scheduleExportEnabled = false;
		mBoundFunfManager.unregisterPipelineAction(this, ACTION_EXPORT_DATA);
		
	}
	
	@SimpleProperty(description = "Indicates whether the scheduled export task is current" +
			"enabled.", category = PropertyCategory.BEHAVIOR)
	public boolean ScheduleExportEnabled(){
		return this.scheduleExportEnabled; 
		
	}
	
	
	
	
	
	/*
	 * After we bind to FunfManger, we have to register self to Funf as a
	 * Pipeline. This is for later relevant
	 */
	private void registerSelfToFunfManager() {
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
	

  
  /**
   * Indicates the interval for a re-occurring archive activity for DB
   */
  @SimpleFunction(description = "Set the schedule for Archiving service")
  public void SetScheduleArchive(int newArchivePeriod) {

    this.archive_period = newArchivePeriod;

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
    mBoundFunfManager.unregisterPipelineAction(this, ACTION_EXPORT_DATA);
    mBoundFunfManager.unregisterPipelineAction(this, ACTION_CLEAR_BACKUP);
  }


	@Override
	public void onCreate(FunfManager arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onRun(String action, JsonElement config) {

    if (ACTION_ARCHIVE_DATA.equals(action)) {
      Log.i(TAG, "Run action archive data");
  		Intent i = new Intent(form, NameValueDatabaseService.class);
  		Log.i(TAG, "archiving data.");
  		i.setAction(DatabaseService.ACTION_ARCHIVE);
  		i.putExtra(DatabaseService.DATABASE_NAME_KEY, PIPE_NAME);
  		form.startService(i);

//      DBTrustUtil.archiveData(this.mainUIThreadActivity);

    }
    if (ACTION_EXPORT_DATA.equals(action)) {
      // Do something else
      Log.i(TAG, "Run action export_DATA");
//      DBTrustUtil.uploadData(this.mainUIThreadActivity, DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY);

    }if (ACTION_CLEAR_BACKUP.equals(action)){
    	
    }
    
    
	}



	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
    doUnbindService();
	}

}
