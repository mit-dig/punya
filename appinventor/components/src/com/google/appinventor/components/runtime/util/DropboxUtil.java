// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.app.Activity;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;



public class DropboxUtil {
  public static final String TAG = "DropboxUtil";

  private enum FILETYPE { MEDIA, DB, OTHER}
  
  public static final String
  PREFS_DROPBOX = "dropboxPrefs",
  PREF_DROPBOX_APP_KEY = "__DROPBOX_APP_KEY__",
  PREF_DROPBOX_APP_SECRET = "__DROPBOX_APP_SECRET__",
  PREF_DROPBOX_ACCESSTOKEN_KEY = "__DROPBOX_ACCESSTOKEN_KEY__",
  PREF_DROPBOX_ACCESSTOKEN_SECRET = "__DROPBOX_ACCESSTOKEN_SECRET__",
  PREF_DROPBOX_UPLOAD_FOLDER = "dropboxPath",
  PREF_DROPBOX_UPLOAD_DB_DEFAULT = "dbData",
  PREF_DROPBOX_UPLOAD_DB_NAME = "dbname"; 
  //see uploadDataFile, we use this to get the path of the folder in which we place the db file
  
  final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;


  private static DropboxAPI<WebAuthSession> dropboxApi;

public static DropboxAPI<WebAuthSession> getDropboxApi(Context context) {
  // the App Inventor dropbox component need to handle the Web Authentication part, like how the Twitter 
  // component does
  if (dropboxApi == null) {
    SharedPreferences settings = context.getSharedPreferences(PREFS_DROPBOX, Activity.MODE_PRIVATE);
    String appKey = settings.getString(PREF_DROPBOX_APP_KEY, null);
    String appSecret = settings.getString(PREF_DROPBOX_APP_SECRET, null);
    String accessTokenKey = settings.getString(PREF_DROPBOX_ACCESSTOKEN_KEY, null);
    String accessTokenSecret =  settings.getString(PREF_DROPBOX_ACCESSTOKEN_SECRET, null);
    
    Log.i(TAG, "Access Token key:" + accessTokenKey);
    Log.i(TAG, "Access Token Secret:" + accessTokenSecret);
    
    AppKeyPair appKeyPair = new AppKeyPair(appKey, appSecret);
    AccessTokenPair accessTokenPair = new AccessTokenPair(accessTokenKey, accessTokenSecret);
    WebAuthSession session = new WebAuthSession(appKeyPair, ACCESS_TYPE, accessTokenPair);

    dropboxApi = new DropboxAPI<WebAuthSession>(session);

  }
      return dropboxApi;
}

public static File getAppDataFolder(Context context) {

  return new File(Environment.getExternalStorageDirectory(), context.getPackageName());

}


private static boolean uploadFolderFiles(Context context, File file) throws Exception {
  //in case when the specified File path is a directory, do we want to do a recursive upload?
  //we don't do nested looping through a folder to get all the files
  if(file.isDirectory()){
     File[] listOfFiles = file.listFiles();
     for (File f: listOfFiles) {
       if (uploadSingleFile(context, f))
         ; //if successful, do nothing, else return false
       else
         return false;
     }

  }
  return true;  

}

private static String getAppName(Context context){
  
  final PackageManager pm = context.getPackageManager();
  ApplicationInfo ai;
  try {
      ai = pm.getApplicationInfo(context.getPackageName(), 0);
  } catch (final NameNotFoundException e) {
      ai = null;
  }
  final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
  
  return applicationName;
}


private static boolean uploadSingleFile(Context context, File file) throws Exception {
  //this method actually does the uploading, only successful upload will return true, else will be exception throws to
  //the caller methods
  String dataPath = file.getAbsolutePath();
  FileInputStream is = null;
  String dropboxFolder = "";
  SharedPreferences pref = context.getSharedPreferences(PREFS_DROPBOX, Activity.MODE_PRIVATE);
  
  
  try {
    is = new FileInputStream(file);
    Log.i(TAG, "file:" + file.toString()); 
    DropboxAPI<WebAuthSession> mDBApi = getDropboxApi(context);

    
    // We like to have a fixed folder path at Dropbox for db backup
    // should be in /Apps/App Inventor/appName/db/db-name/, 
    // note that Apps/App Inventor/ is given by Dropbox when the ACCESS_TYPE is "folder"
    // For example, if someone build an app named "mySensorCollect", and has "SensorData" as DBName,
    // then the path on Dropbox will be /db/mySensorCollect/SensorData/xxx-2013-xxx-yyy.db

    if (dataPath.endsWith("db")){
      
      String dbName = pref.getString(PREF_DROPBOX_UPLOAD_DB_NAME, 
                            PREF_DROPBOX_UPLOAD_DB_DEFAULT);
      
      dropboxFolder = getAppName(context) + "/db/" + dbName;
      Log.i(TAG, "db dropbox folderpath:" + dropboxFolder);
      
    }
    else{
      //For other kinds of data other than db backup, the folder path will be:
      // Apps/App Inventor/appName/data/folder-name, folder-name will be specified by the user
      dropboxFolder = getAppName(context) + "/data/" + pref.getString(DropboxUtil.PREF_DROPBOX_UPLOAD_FOLDER, "");
     
      Log.i(TAG, "dropbox folderpath:" + dropboxFolder);
      
    }

    //if the user did not set in the Dropbox.java, then it will 
    Entry entry = mDBApi.putFileOverwrite("/" + dropboxFolder + "/" + file.getName(), is, file.length(), null);
    Log.i(TAG, "Return From dropbox API sending the file...");
 
    return true;
    
  } catch (FileNotFoundException e) {
    Log.w(TAG, "File not found: " + dataPath);
    throw e;
  } catch (DropboxException e) {
    
    Log.w(TAG, "Dropbox exception: " + dataPath + e.getMessage());
    Log.w(TAG, "what type:" + e.toString());
    throw e;
    
  } finally {
    if (is != null) {
      try {
        is.close();
      } catch (IOException e) {
        Log.w(TAG, "Unable to close file: " + dataPath);
      }
    }
  }
  
}
  

public static boolean uploadDataFile(Context context, File file) throws Exception {

  
  if (file.isDirectory()){
    return uploadFolderFiles(context, file);
    
  }else{
    return uploadSingleFile(context, file);
    
  }
  
 
}



}
