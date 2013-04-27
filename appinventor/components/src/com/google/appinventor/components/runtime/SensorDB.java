package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;


/* 
 * This is equivalent to the class "DatabaseService" in Funf library excepts we don't 
 * provide recording data explicitly in the App Inventor interface. (Because it should never be done
 * in the UI level to save sensor stream data which will cause performance issues)
 * The component will only provide archive, export, clear backup functions with scheduling. 
 * 1) archive: copy the sqlite db file and moved it to sd card, under /packageName/dbName/archive/ 
 *    (note: this function will delete the sqlite db as well)
 * 2) export: will export the sqlite db with specified format to sd card, under /packageName/export/
 * 3) clear backup: every time when we execute archive function, a copy will be put into /packageName/dbName/backup
 *    as well for backup purpose. A user (developer) may want to clear the backup files after a while.  
 */
@DesignerComponent(version = YaVersion.WIFISENSOR_COMPONENT_VERSION,
    description = "Non-visible component that provides method      s to backup and upload " +
    		"sensor db on the phone.",
    category = ComponentCategory.BASIC,
    nonVisible = true,
    iconName = "images/tinyDB.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, "
  + "android.permission.ACCESS_NETWORK_STATE, "
  + "android.permission.WAKE_LOCK, "
  + "android.permission.READ_LOGS, "
  + "android.permission.INTERNET")
@UsesLibraries(libraries = "funf.jar")
public class SensorDB {
  
   
  
  
  

}
