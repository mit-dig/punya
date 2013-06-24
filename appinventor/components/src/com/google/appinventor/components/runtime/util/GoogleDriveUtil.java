package com.google.appinventor.components.runtime.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.api.services.drive.DriveScopes;
import com.google.appinventor.components.runtime.GoogleDriveUploadService;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
//import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;


//TODO: this class is obsolete (most of the codes is merged to GoogleDriveArchive.java
@Deprecated
public class GoogleDriveUtil {
  
  
  private static final String TAG = "GoogleDriveUtil";
  public static final String
  PREFS_GOOGLEDRIVE = "googleDrivePrefs",
  PREF_GOOGLE_ACCOUNT = "__GOOGLE_ACCOUNT_KEY__",
  PREF_GOOGLE_UPLOAD_FOLDER = "GoogleDrivePath", //default folder path on Google Drive
  PREF_GOOGLE_UPLOAD_DB_NAME = "gdUploadDBName", //default key for getting db name
  GOOGLE_UPLOAD_DB_DEFAULT = "SensorData"; //default name for sensor db

  private static final String DEFAULT_SCOPE = DriveScopes.DRIVE;
  private static Drive gdService;
//  private GoogleAccountCredential credential;
//  
//  public static Drive getDriveService(GoogleAccountCredential credential) {
//    
//    if (gdService == null) {
//      //first time created gdService, needs to also use GoogleCredential and select the Google account
//      
//      credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
//      startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
//      
//      
//      gdService =  new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
//          .build();
//
//    }
//    return gdService;
//  }
  
  
  private static boolean uploadSingleFile(Context context, File file) throws Exception {
    //this method actually does the uploading, only successful upload will return true, else will be exception throws to
    //the caller methods
    String dataPath = file.getAbsolutePath();
    FileInputStream is = null;
    String googleFolder = "";
    SharedPreferences pref = context.getSharedPreferences(PREFS_GOOGLEDRIVE, Activity.MODE_PRIVATE);

    
    try {
      is = new FileInputStream(file);
      Log.i(TAG, "file:" + file.toString()); 
//      Drive service = getDriveService();
      
      // We like to have a fixed folder path at Google Drive for db backup
      // Need documentation

      if (dataPath.endsWith("db")){
        //TODO
        //uploading database file
 
      }
      else{
        //uploading normal file (mostly csv files in our use case)
 
      }
 
      Log.i(TAG, "Return From Google Drive Drive sending the file...");
   
      return true;
      
    } catch (FileNotFoundException e) {
      Log.w(TAG, "File not found: " + dataPath);
      throw e;
    } 
    finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          Log.w(TAG, "Unable to close file: " + dataPath);
        }
      }
    }
    
  }
  
  private static boolean uploadFolderFiles(Context context, File file) throws Exception {
    // In case when the specified File path is a directory
    // Note that we don't do nested looping through a folder to get all the files
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
  
  public static boolean uploadDataFile(Context context, File file) throws Exception {

    
    if (file.isDirectory()){
      return uploadFolderFiles(context, file);
      
    }else{
      return uploadSingleFile(context, file);
      
    }
    
   
  }

}
