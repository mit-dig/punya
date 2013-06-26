package com.google.appinventor.components.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;


import com.google.api.client.http.FileContent;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import edu.mit.media.funf.storage.RemoteFileArchive;

public class GoogleDriveArchive implements RemoteFileArchive {
  private static final String TAG = "GoogleDriveArchive";

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
  private com.google.api.services.drive.model.File gdFolder;
 
  private final SharedPreferences sharedPreferences;

  private String googleDriveFolderName;


  public GoogleDriveArchive(Context context, String GoogleDriveFolderName) {
    Log.i(TAG, "we are here in GDArchive");
    this.mContext = context;
    sharedPreferences = context.getSharedPreferences(GoogleDrive.PREFS_GOOGLEDRIVE, context.MODE_PRIVATE);
    this.mAccount = sharedPreferences.getString(GoogleDrive.PREF_ACCOUNT_NAME, "");
    Log.i(TAG, "mAccount:" + this.mAccount);

    this.googleDriveFolderName = GoogleDriveFolderName;

    
    
  }
  
  private com.google.api.services.drive.model.File getGoogleDriveFolder() {

    com.google.api.services.drive.model.File folder = null;
    Log.i(TAG, "in getGoogleDriveFolder");
    Log.i(TAG, "Drive folder name:" + googleDriveFolderName);

    if (this.googleDriveFolderName.equals(GoogleDrive.DEFAULT_GD_FOLDER)) {
      try {
        Log.i(TAG, "we upload directly to root folder");
        folder = mService.files().get("root").execute();
      } catch (Exception e) {
        Log.e(TAG, "exception!");
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    } else { // if the user has set some GD_folder that he likes to upload the files to
      String query = "mimeType = 'application/vnd.google-apps.folder' and title = '"
          + googleDriveFolderName + "' and 'root' in parents";
      // query if the specify folder exists or not, if not we will creat one
      FileList files = ExcuteQuery(query);
      
      if (files.getItems().isEmpty()) {
        Log.i(TAG, "is empty");
        folder = createGoogleFolder();
      }

      else {
        Log.i(TAG, "is not empty");
        Log.i(TAG, files.getItems().toString());
        folder = files.getItems().get(0);// if the folder exists, it should be
                                         // the only one
      }
    }

    return folder;

  }
  
  @Override
  public boolean add(File file) throws Exception {
    // TODO Auto-generated method stub
    
    return uploadDataFile(file);
 
  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  private FileList ExcuteQuery(String query){
  	//this is to search a folder name that is passed in by the caller
  	//https://developers.google.com/drive/search-parameters
    Log.i(TAG, "Query: " + query);
  	FileList files = null;
      try {
      	
      	files =  mService.files().list().setQ(query).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      return files;
  }

  private boolean uploadSingleFile(File file) throws Exception {
    // this method actually does the uploading, only successful upload will
    // return true, else will be exception throws to
    // the caller methods
    String dataPath = file.getAbsolutePath();

    // Need to get Google Drive folder information if an app specifies the
    // folder to upload to
    // 1. get or create folder if not exist\

    try {
      Log.i(TAG, "Before processGDFile");
      // processGDFile(Drive service, String parentId, File localFile)
      com.google.api.services.drive.model.File processedFile = processGDFile(
          mService, gdFolder.getId(), file);

      Log.i(TAG, "Return From Google Drive Drive sending the file...");

      return true;

    } catch (FileNotFoundException e) {
      Log.w(TAG, "File not found: " + dataPath);
      throw e;
    }

  }
  
  private com.google.api.services.drive.model.File createGoogleFolder()  {
  	Log.i(TAG, "in createGoogleFolder......");
  	//go to the root, and create a folder and return the handle
  	com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
    body.setTitle(this.googleDriveFolderName);
    body.setMimeType("application/vnd.google-apps.folder");
    body.setParents(Arrays.asList(new ParentReference().setId("root")));
 
    com.google.api.services.drive.model.File file = null;
    try {
      file = mService.files().insert(body).execute();
      
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Log.i(TAG, "We have created a Google Drive Folder with name" + this.googleDriveFolderName);
    return file;
 
	}
  
  
  private com.google.api.services.drive.model.File processGDFile(Drive service,
      String parentId, File localFile) throws Exception {
    Log.i(TAG, "We are in processGDFile");
    boolean existed = false;
    com.google.api.services.drive.model.File processedFile;
    // Determine whether the file exists in this folder or not.
    String q = "'" + parentId + "' in parents and" + "title = " + "'"
        + localFile.getName() + "'";

    FileContent mediaContent = new FileContent("", localFile);

    FileList resultFileList = ExcuteQuery(q);
 
    try {
      // if this file already exists in GD, we do update
      if (!resultFileList.getItems().isEmpty()) { 
        Log.i(TAG, "the file exists, use update....");
        existed = true;
        com.google.api.services.drive.model.File gdFile = resultFileList
            .getItems().get(0);
        processedFile = service.files()
            .update(gdFile.getId(), gdFile, mediaContent).execute();
        // Uncomment the following line to print the File ID.
        Log.i(TAG, "Processed File ID: %s" + processedFile.getId());

      } else {
        Log.i(TAG, "the file is new, create its meta data");
        // Start the new File's metadata.
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(localFile.getName());

        // Set the parent folder.
        if (parentId != null && parentId.length() > 0) {
          body.setParents(Arrays.asList(new ParentReference().setId(parentId)));
        }
        Log.i(TAG, " before insert body");
        //TODO: detect the file name's extension, if it's csv or docx, then we force it to be converted


        Drive.Files.Insert insertOperation = service.files().insert(body, mediaContent).setConvert(true);
        processedFile = insertOperation.execute();
        //processedFile = service.files().insert(body, mediaContent).execute();
 
        Log.i(TAG, "Processed File ID: %s" + processedFile.getId());

      }
    } catch (Exception e) {
      throw e;
    }
    return processedFile;

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
  private void getDriveService() {
    String mAccountName = this.mAccount;

    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
        mContext, DriveScopes.DRIVE);
    Log.i(TAG, "before set selectedAccountName:" + mAccountName);
    credential.setSelectedAccountName(mAccountName);
    try {
      credential.getToken();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (GoogleAuthException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Log.i(TAG, "before build drive service... ");
    // set up Drive service
    mService = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
        new GsonFactory(), credential).build();
    // now we have the service, we can get Google Drive folder
    gdFolder = getGoogleDriveFolder();

    Log.i(TAG, "after drive service... ");

  }

  private boolean uploadFolderFiles(File file) throws Exception {
    // In case when the specified File path is a directory
    // Note that we don't do nested looping through a folder to get all the
    // folders and files
    Log.i(TAG, "@upalodFolderFiles");
    File[] listOfFiles = file.listFiles();
    for (File f : listOfFiles) {
      Log.i(TAG, "@uploadFolderFiles:singleFile" + f.toString());
      if (f.isFile()) {
        Log.i(TAG, "here" + f.toString());
        if (uploadSingleFile(f))
          ; // if successful, do nothing, else return false
        else
          return false;
      }
    }

    // only return true if all files in the folder has been succesfully
    // uploaded
    return true;

  }
  
  public boolean uploadDataFile(File file) throws Exception {
    Log.i(TAG, "before getDriveService");
    getDriveService(); // re-init the Drive service
    Log.i(TAG, "after getDriveService");
    if (file.isDirectory()) {
      Log.i(TAG, "it's a folder");
      return uploadFolderFiles(file);

    } else {
      Log.i(TAG, "it's a file");
      return uploadSingleFile(file);

    }

  }
  
  

}
