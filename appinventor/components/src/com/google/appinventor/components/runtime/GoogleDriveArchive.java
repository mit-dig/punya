package com.google.appinventor.components.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.appinventor.components.runtime.GoogleDrive.AccessToken;
import com.google.appinventor.components.runtime.util.DropboxUtil;
import com.google.appinventor.components.runtime.util.GoogleDriveUtil;

import android.accounts.Account;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import edu.mit.media.funf.storage.RemoteFileArchive;

public class GoogleDriveArchive implements RemoteFileArchive {
  private static final String TAG = "GoogleDriveArchive";
  private static Drive service;
  private GoogleAccountCredential credential;
  
  public static final String
  PREFS_GOOGLEDRIVE = "googleDrivePrefs",
  PREF_GOOGLE_ACCOUNT = "__GOOGLE_ACCOUNT_KEY__",
  PREF_GOOGLE_UPLOAD_FOLDER = "GoogleDrivePath", //default folder path on Google Drive
  PREF_GOOGLE_UPLOAD_DB_NAME = "gdUploadDBName", //default key for getting db name
  GOOGLE_UPLOAD_DB_DEFAULT = "SensorData"; //default name for sensor db
  
  // just a fake id for implmenting the interface
  public static final String GOOGLEDRIVE_ID = "googledrive://appinventor/__ID__"; 
  
  private Context mContext;
  private Drive mService;
  private String mAccount;
 
  private final SharedPreferences sharedPreferences;
  private NotificationManager mNM;
  private String googleDriveFolderName;


  public GoogleDriveArchive(Context context, String GoogleDriveFolderName) {
    this.mContext = context;
    credential.usingOAuth2(context, DriveScopes.DRIVE);
    sharedPreferences = context.getSharedPreferences(GoogleDrive.PREFS_GOOGLEDRIVE, context.MODE_PRIVATE);
    this.mAccount = sharedPreferences.getString(GoogleDrive.PREF_ACCOUNT_NAME, "");
    mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    this.googleDriveFolderName = GoogleDriveFolderName;
  }
  
  
  @Override
  public boolean add(File file) throws Exception {
    // TODO Auto-generated method stub
    
    return uploadDataFile(mContext, file);
 
  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  private 	FileList ExcuteQuery(String query){
  	//this is to search a folder name that is passed in by the caller
  	//https://developers.google.com/drive/search-parameters
  	FileList files = null;
      try {
      	
      	files =  mService.files().list().setQ(query).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
 
      return files;
 
  }
  

  
  
  private boolean uploadSingleFile(Context context, File file) throws Exception {
    //this method actually does the uploading, only successful upload will return true, else will be exception throws to
    //the caller methods
    String dataPath = file.getAbsolutePath();
    FileInputStream is = null;
    com.google.api.services.drive.model.File gdFolder;
    
    mService = getDriveService();
    
    //Need to get Google Drive folder information, if app specifies the folder to upload to
    // 1. get or create folder if not exist\
    String query = "mimeType = 'application/vnd.google-apps.folder' and title = " + googleDriveFolderName ;

    FileList files = ExcuteQuery(query);
    // should only have one folder 
    if (files == null)
    	gdFolder = createGoogleFolder(googleDriveFolderName);
    	//File file : files.getItems()
    else {
    	gdFolder =  files.getItems().get(0);
    }
 
    try {
    	
//    	processGDFile(Drive service, String parentId, File localFile)
    	processGDFile(mService, gdFolder.getId(), file);
      	
 
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
  
  private com.google.api.services.drive.model.File createGoogleFolder(
			String gdFolderName) throws IOException {
  	
  	//go to the root, and create a folder and return the handle
  	com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
    body.setTitle(gdFolderName);
    body.setMimeType("application/vnd.google-apps.folder");
    body.setParents(Arrays.asList(new ParentReference().setId("root")));
    Log.i(TAG, "We have created a Google Drive Folder with name" + gdFolderName);
    
    com.google.api.services.drive.model.File file = service.files().insert(body).execute();
    return file;
 
	}
  
  
  private com.google.api.services.drive.model.File processGDFile(Drive service,
      String parentId, File localFile) {
    
  	boolean existed = false;
    //need to determine whether the file exist in this folder or not.
    String q = "'" + parentId + "' in parents and" + "title = " + "'" + localFile.getName() + "'";

    FileContent mediaContent = new FileContent(mimeType, localFile);
    
    
    
    com.google.api.services.drive.model.FileList gdFile = ExcuteQuery(q);
    // File's content. 
    try{
    	if(gdFile != null)
    		existed = true;
  			com.google.api.services.drive.model.File updateFile = service.files().update(body, ,mediaContent).execute();
  	// Uncomment the following line to print the File ID.
  			Log.i(TAG, "Processed File ID: %s" + updateFile.getId());
  			return updateFile;
     
  			else{
  				// Start the new File's metadata.
  				com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
  				body.setTitle(localFile.getName());
    
  				// Set the parent folder.
  				if (parentId != null && parentId.length() > 0) {
  					body.setParents(Arrays.asList(new ParentReference().setId(parentId)));
  				}
  				com.google.api.services.drive.model.File insertFile = service.files().insert(body, mediaContent).execute();
    	// Uncomment the following line to print the File ID.
  				Log.i(TAG, "Processed File ID: %s" + insertFile.getId());
  				return insertFile;
  			}
    } catch (IOException e) {
      System.out.println("An error occured: " + e);
      return null;
    }



	/**
   * Retrieve a authorized service object to send requests to the Google Drive
   * API. On failure to retrieve an access token, a notification is sent to the
   * user requesting that authorization be granted for the
   * {@code https://www.googleapis.com/auth/drive.file} scope.
   * 
   * @return An authorized service object.
   * @throws Exception 
   */
  private Drive getDriveService() throws Exception {
    if (mService == null) {
      try {
        GoogleAccountCredential credential = 
        		GoogleAccountCredential.usingOAuth2(mContext, DriveScopes.DRIVE_FILE);
        credential.setSelectedAccountName(mAccount);
        // Trying to get a token right away to see if we are authorized
        credential.getToken();
        mService = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
        		new GsonFactory(), credential).build();
			} catch (Exception e) {
				Log.e(TAG, "Failed to get token");
				// If the Exception is User Recoverable, we display a notification that will trigger the
				// intent to fix the issue.
				if (e instanceof UserRecoverableAuthException) {
					// This should not happen in our case
					throw e;
 
				} else {
					e.printStackTrace();
				}
			}
    }
    return mService;
  }
  
  private boolean uploadFolderFiles(Context context, File file) throws Exception {
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
  
  public boolean uploadDataFile(Context context, File file) throws Exception {

    
    if (file.isDirectory()){
      return uploadFolderFiles(context, file);
      
    }else{
      return uploadSingleFile(context, file);
      
    }
    
   
  }
  
  

}
