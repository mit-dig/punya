package com.google.appinventor.components.runtime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.SensorDbUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.time.DecimalTimeUnit;
import org.json.JSONException;

/*
 * This class implements Funf's Pipeline interface. Pipeline is an interface that define functions
 * for periodic task, which means that the class can register itself with FunfManager with specified
 * waking-up period using registerPipelineAction(this, ACTION_TAG, schedule)
 *
 * It supports configuration of its action through SensorDB.java class's methods.
 *
 * This class lives in a long-running service (FunfManager), so it will not directly
 * communicate with the AI component. It will write out results to SharedPreference and the AI component can
 * learn about the result of the task by:
 * 1) if the component wants to check the result when it is active it can use onTaskResult event
 *    (implement by onSharePreferenceChanged)
 * 2) or it can check SharedPreference with keywords like (SensorDB.ARCHIVE_RESULT_KEY), and get
 *    Yail pair values (result and time stamp)
 *
 *
 * TODO: fix the documentation below
 * 1) archive: copy the sqlite db file and moved it to sd card, under /packageName/dbName/archive/
 *    (note: this function will delete the sqlite db as well)
 * 2) export: will export the sqlite db with specified format to sd card, under /packageName/export/
 * 3) clear backup: every time when we execute archive function, a copy will be put into /packageName/dbName/backup
 *    as well for backup purpose. A user (developer) may want to clear the backup files after a while.
 */

public class SensorDBPipeline implements Pipeline, DataListener{

  protected static final String ACTION_ARCHIVE_DATA = "ARCHIVE_DATA";
  protected static final String ACTION_EXPORT_DATA = "EXPORT_DATA";
  protected static final String ACTION_CLEAR_BACKUP= "CLEAR_BACKUP";
  protected static final String GLOBAL_ONOFF = "global_onoff";
  private static final int ARCHIVE_PERIOD = 86400; // archive database 60*60*24 (archive every 24 hour)
  private static final int EXPORT_PERIOD = 86400;//
  private static final int CLEAR_BACKUP = 86400;
  private static final String TAG = "SensorDBPipeline";
  protected static final String ACTIVE_SENSORS = "active.sensors";
  private Map<String, Integer> activeSensors = new HashMap<String, Integer>();
  private Map<String, String> sensorMapping = SensorDbUtil.sensorMap;

  private boolean scheduleArchiveEnabled;
  private boolean scheduleExportEnabled;
  private boolean scheduleClearBackupEnabled;

  private int archive_period;
  private int export_period;
  private int clear_period;
  private boolean hideSensitiveData;
  private SharedPreferences sharedPreferences;

  private Calendar calendar = Calendar.getInstance(Locale.getDefault());
  private BigDecimal localOffsetSeconds = BigDecimal.valueOf(
      calendar.get(Calendar.ZONE_OFFSET)
          + calendar.get(Calendar.DST_OFFSET), DecimalTimeUnit.MILLI);

  private String format = "csv";

    private FunfManager funfManager;
    @Override
    public void onCreate(FunfManager manager) {

      funfManager = manager;
      Log.i(TAG, "Created main pipeline from funfManager:" + manager.toString() + ",at: " + System.currentTimeMillis());

      archive_period = ARCHIVE_PERIOD;
      export_period = EXPORT_PERIOD;
      clear_period = CLEAR_BACKUP;
      scheduleArchiveEnabled = false;
      scheduleExportEnabled = false;
      scheduleClearBackupEnabled = false;
      hideSensitiveData = false;
      sharedPreferences = manager.getSharedPreferences("sensorDBPipeline", Context.MODE_PRIVATE);

      initActions();

    }

  private void initActions() {
    // whenever the pipeline is created or recreated, we will look into the sharedPreference
    // for re-init its actions and register probes listeners if needed

    Object archivePref = getPreference(ACTION_ARCHIVE_DATA);

    if (!archivePref.equals("")) {// we have previously set value in here
      Log.i(TAG, "recreate archive schedule: " + archivePref);
      Integer archivePeriod = (Integer) archivePref;
      if (archivePeriod != 0) {
        Schedule archive_p = new Schedule.BasicSchedule(
            BigDecimal.valueOf(archivePeriod), BigDecimal.ZERO, false, false);
        funfManager.registerPipelineAction(this, ACTION_ARCHIVE_DATA, archive_p);
        scheduleArchiveEnabled = true;
        archive_period = archivePeriod;
      }
    }

    Object clearPref = getPreference(ACTION_CLEAR_BACKUP);

    if (!clearPref.equals("")) {
      Integer clearPeriod = (Integer) clearPref;
      Log.i(TAG, "recreate clear schedule:" + clearPeriod);
      if (clearPeriod != 0) {
        Schedule clear_p = new Schedule.BasicSchedule(
            BigDecimal.valueOf(clearPeriod), BigDecimal.ZERO, false, false);
        funfManager.registerPipelineAction(this, ACTION_CLEAR_BACKUP, clear_p);
        scheduleClearBackupEnabled = true;
        clear_period = clearPeriod;
      }
    }

    Object exportPref = getPreference(ACTION_EXPORT_DATA);

    if (!exportPref.equals("")) {
      Integer exportPeriod = (Integer)exportPref;
      Log.i(TAG, "recreate export schedule:" + exportPeriod);
      if (exportPeriod != 0) {
        Schedule export_p = new Schedule.BasicSchedule(
            BigDecimal.valueOf(exportPeriod), BigDecimal.ZERO, false, false);
        funfManager.registerPipelineAction(this, ACTION_EXPORT_DATA, export_p);
        scheduleExportEnabled = true;
        export_period = exportPeriod;
      }
    }

    String jsonArrayStr = (String)getPreference(ACTIVE_SENSORS);
    Log.i(TAG, "ACTIVE sensor:" + jsonArrayStr);
    if (!jsonArrayStr.isEmpty()){
      try {
        List<Object> periods = (List<Object>)JsonUtil.getObjectFromJson(jsonArrayStr);
        // will get a list of lists (key, value)
        // it could be {"active.sensors" : []}
        for (int i = 0; i > periods.size(); i++){
          List keyVal = (List)periods.get(i);
          String sensor = (String) keyVal.get(0);
          int period = (Integer) keyVal.get(1);
          Log.i(TAG, "key(sensor):" + sensor);
          Log.i(TAG, "value(period):" + period);
          addSensorCollection(sensor, period);
        }
      } catch (JSONException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }


  }

  @Override
  public void onDestroy() {
    // TODO Auto-generated method stub
    Log.i(TAG, "SesorDBPipeline" + this + "got killed at:" + System.currentTimeMillis());
  }

  @Override
  public void onRun(String action, JsonElement config) {
    if (ACTION_ARCHIVE_DATA.equals(action)) {
      archive();

    }
    if (ACTION_EXPORT_DATA.equals(action)) {
      // Do something else
      export(format);

    }
    if (ACTION_CLEAR_BACKUP.equals(action)) {
      clearBackup();
    }

  }

  // bunch of getter and setters;
  /*
   * The setting of new archive/export/clearBackup period will only take effects
   * on the next registering of the pipe (need to un-register PipeAction first)
   */
  public Map<String, Integer> getActiveSensor() {
    return activeSensors;
  }

  public int getArchivePeriod() {
    return this.archive_period;
  }

  public void setArchivePeriod(int period) {
    this.archive_period = period;

  }

  public int getExportPeriod() {
    return this.export_period;
  }

  public void setExportPeriod(int period) {
    this.export_period = period;

  }

  public int getClearBackupPeriod() {
    return this.clear_period;
  }

  public void setClearBackPeriod(int period) {
    this.clear_period = period;
  }

  public boolean getScheduleArchiveEnabled() {
    return this.scheduleArchiveEnabled;
  }

  public void setScheduleArchiveEnabled(boolean enabled) {
    if (this.scheduleArchiveEnabled != enabled)
      this.scheduleArchiveEnabled = enabled;

    if (scheduleArchiveEnabled) {
      Schedule archivePeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.archive_period), BigDecimal.ZERO, false, false);

      funfManager.registerPipelineAction(this, ACTION_ARCHIVE_DATA, archivePeriod);
    } else {
      funfManager.unregisterPipelineAction(this, ACTION_ARCHIVE_DATA);
    }
    savePreference(ACTION_ARCHIVE_DATA, enabled? this.archive_period : 0);

  }

  public boolean getScheduleExportEnabled() {
    return this.scheduleExportEnabled;
  }

  public void setScheduleExportEnabled(boolean enabled) {
   if (this.scheduleExportEnabled != enabled)
     this.scheduleExportEnabled = enabled;

   if (scheduleExportEnabled){
     Schedule exportPeriod = new Schedule.BasicSchedule(
         BigDecimal.valueOf(this.export_period), BigDecimal.ZERO, false, false);
     funfManager.registerPipelineAction(this, ACTION_EXPORT_DATA, exportPeriod);
   } else {
     funfManager.unregisterPipelineAction(this, ACTION_EXPORT_DATA);
   }

   savePreference(ACTION_EXPORT_DATA, enabled? this.export_period : 0);

  }

  public boolean getScheduleClearbackupEnabled() {
    return this.scheduleClearBackupEnabled;
  }

  public void setScheduleClearbackupEnabled(boolean enabled) {
    if (this.scheduleClearBackupEnabled != enabled)
      this.scheduleClearBackupEnabled = enabled;

    if (scheduleClearBackupEnabled) {
      Schedule clearPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.clear_period), BigDecimal.ZERO, false, false);
      funfManager.registerPipelineAction(this, ACTION_CLEAR_BACKUP, clearPeriod);
    } else {
      funfManager.unregisterPipelineAction(this, ACTION_CLEAR_BACKUP);
    }

    savePreference(ACTION_CLEAR_BACKUP, enabled? this.clear_period : 0);

  }

  public boolean getHideSensitiveData(){
    return this.hideSensitiveData;
  }

  public void setHideSensitiveData(boolean newVal){
    this.hideSensitiveData = newVal;
  }


  private void archive(){
    Intent i = new Intent(funfManager, NameValueDatabaseService.class);
    Log.i(TAG, "archiving data...at: " + System.currentTimeMillis());
    i.setAction(DatabaseService.ACTION_ARCHIVE);
    i.putExtra(DatabaseService.DATABASE_NAME_KEY, SensorDbUtil.DB_NAME);
    funfManager.startService(i);
  }

  public void export(String format) {
    Log.i(TAG, "exporting data...at: " + System.currentTimeMillis());

    Bundle b = new Bundle();
    b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, SensorDbUtil.DB_NAME);
    b.putString(NameValueDatabaseService.EXPORT_KEY, format);
    Intent i = new Intent(funfManager, NameValueDatabaseService.class);
    i.setAction(DatabaseService.ACTION_EXPORT);
    i.putExtras(b);
    funfManager.startService(i);

  }

  public void clearBackup(){
    Intent i = new Intent(funfManager, NameValueDatabaseService.class);
    Log.i(TAG, "clear data backup....at " +  System.currentTimeMillis());
    i.setAction(DatabaseService.ACTION_CLEAR_BACKUP);
    i.putExtra(DatabaseService.DATABASE_NAME_KEY, SensorDbUtil.DB_NAME);
    funfManager.startService(i);
  }

  @Override
  public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
    // TODO Auto-generated method stub
    Log.v(TAG, "Data COMPLETE: " + completeProbeUri);

  }

  @Override
  public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
    // TODO Auto-generated method stub
    Log.i(TAG, "Data received: " + completeProbeUri + ": " + data.toString());


    final JsonObject dataObject = data.getAsJsonObject();
    dataObject.add("probe",
        completeProbeUri.get(RuntimeTypeAdapterFactory.TYPE));
    dataObject.add("timezoneOffset", new JsonPrimitive(localOffsetSeconds));

    final long timestamp = data.get(BaseProbeKeys.TIMESTAMP).getAsLong();
    final String probeName = completeProbeUri.get("@type").getAsString();

    Bundle b = new Bundle();
    b.putString(NameValueDatabaseService.DATABASE_NAME_KEY,
        SensorDbUtil.DB_NAME);
    b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
    b.putString(NameValueDatabaseService.NAME_KEY, probeName);
    b.putString(NameValueDatabaseService.VALUE_KEY, dataObject.toString());
    Intent i = new Intent(funfManager,  NameValueDatabaseService.class);
    i.setAction(DatabaseService.ACTION_RECORD);
    i.putExtras(b);
    funfManager.startService(i);


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
   * add sensor to the activeSensor set, and register itself to funfManger for
   * listening to probe events. Each sensor only allow one schedule
   */
  public void addSensorCollection(String sensorName, int period) {

    // if we already have this sensor, then do nothing.
    if (activeSensors.containsKey(sensorName)) {
      ;// do nothing
    } else {
      Log.i(TAG, "Registering data requests.");
      JsonElement dataRequest = getDataRequest(period,
          sensorMapping.get(sensorName));
      Log.i(TAG, "Data request: " + dataRequest.toString());

      funfManager.requestData(this, dataRequest);
      activeSensors.put(sensorName, period);
      savePreference(ACTIVE_SENSORS, activeSensors);
    }
  }

  private JsonElement getDataRequest(int interval, String probeName) {
    // This will set the schedule to FunfManger for this probe
    // List<JsonElement> dataRequests = new ArrayList<JsonElement>();
    /*
     * Accepted format for probe configuration {"@type":
     * "edu.mit.media.funf.probe.builtin.BluetoothProbe", "maxScanTime": 40,
     * "@schedule": { "strict": true, "interval": 60, "duration": 30,
     * "opportunistic": true } }
     */

    // fine tuning for SimpleLocationProbe
    if(probeName.equals("edu.mit.media.funf.probe.builtin.SimpleLocationProbe")){
      return getLocationRequest(interval, probeName);
    }

    JsonElement dataRequest = new JsonObject();
    ((JsonObject) dataRequest).addProperty("@type",
        probeName);
    if (probeName.equals("edu.mit.media.funf.probe.builtin.SmsProbe")
        || probeName.equals("edu.mit.media.funf.probe.builtin.CallLogProbe") 
        || probeName.equals("edu.mit.media.funf.probe.builtin.TelephonyProbe")) {
      ((JsonObject) dataRequest).addProperty("hideSensitiveData", hideSensitiveData);
    }

    //((JsonObject) dataRequest).addProperty("maxScanTime", 40);
    JsonElement scheduleObject = new JsonObject();
    ((JsonObject) scheduleObject).addProperty("strict", true);
    ((JsonObject) scheduleObject).addProperty("interval", interval);
    ((JsonObject) scheduleObject).addProperty("opportunistic", true);

    if(probeName.equals("edu.mit.media.funf.probe.builtin.LightSensorProbe")){
      ((JsonObject) scheduleObject).addProperty("duration", 5);
    }

    ((JsonObject) dataRequest).add("@schedule", scheduleObject);

    return dataRequest;
  }


  private JsonElement getLocationRequest(int interval, String probeName) {

    JsonElement dataRequest = new JsonObject();
    ((JsonObject) dataRequest).addProperty("@type", probeName);

    // simpleLocationProbe configuration (useGPS = true, useNetwork=true, useCache=false)
    ((JsonObject) dataRequest).addProperty("useCache", false); // we want to get the new location
    //TODO: detect whether it's moving or not (indoor or outdoor)

    //((JsonObject) dataRequest).addProperty("maxScanTime", 40);
    JsonElement scheduleObject = new JsonObject();
    ((JsonObject) scheduleObject).addProperty("strict", true);
    ((JsonObject) scheduleObject).addProperty("interval", interval);
    ((JsonObject) scheduleObject).addProperty("opportunistic", true);

    ((JsonObject) dataRequest).add("@schedule", scheduleObject);

    return dataRequest;
  }

  /*
   * We already make sure that the caller(Sensor DB) will only pass in active
   * sensor collection If the sensor is currently not active, then do nothing.
   */
  public void removeSensorCollection(String sensorName) {

    if (activeSensors.containsKey(sensorName)) {
      Log.i(TAG, "Un-Registering data requests.");

      JsonElement dataRequest = getDataRequest(activeSensors.get(sensorName),
          sensorMapping.get(sensorName));

      Log.i(TAG, "Data request: " + dataRequest.toString());

      funfManager.unrequestData(this, dataRequest);
      activeSensors.remove(sensorName);
      savePreference(ACTIVE_SENSORS, activeSensors);

    } else {
      Log.i(TAG, sensorName + " is not active");
    }

  }
  /*
   * We limit each type of sensor only can be collected by one configured task
   */
  
  public void updateSensorCollection(String sensorName, int interval){
    Log.i(TAG, "Update data request, remove the request, then add");
    removeSensorCollection(sensorName);
    addSensorCollection(sensorName, interval);
    
  }
  //
  // Return a list of list 
  //
  public Set<Entry<String, Integer>> currentActiveSensor(){
    return activeSensors.entrySet();
    
    
  }
  


}
