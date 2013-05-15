// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.google.appinventor.components.runtime.util.DropboxUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.text.format.DateFormat;
import android.util.Log;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;

import edu.mit.media.funf.util.EqualsUtil;
import edu.mit.media.funf.util.HashCodeUtil;
import edu.mit.media.funf.util.LockUtil;
import edu.mit.media.funf.util.LogUtil;
 


/*
 * For DropboxUploadService, we try to distinguish two types of upload, 
 * one is the normal-uploading-files which will just upload all the files 
 * in the current specified file path.
 * 
 * The second one is uploading the database file 
 */
public class DropboxUploadService extends UploadService {
  
  
  public static final String FILE_TYPE = "filetype";
  
  
  public static final int
  REGULAR_FILE = 0,
  DATABASE_FILE = 1;
  
  @Override
  protected RemoteFileArchive getRemoteArchive(String id) {
    return new DropboxArchive(getApplicationContext());
  }
  private static final String TAG = "DropboxUploadService";



  private static final int MAX_DROPBOX_RETRIES = 3;
  
  private ConnectivityManager connectivityManager;
  private Map<String, Integer> fileFailures;
  private Map<String, Integer> remoteArchiveFailures;
  private Queue<ArchiveFile> dbFilesToUpload;
  private Queue<RegularArchiveFile> filesToUpload;
  private Thread dbUploadThread;
  private Thread regularUploadThread;
  private WakeLock lock;
  
  //App Inventor specific method for communicating error message to component
  private boolean status = true;
  private String error_message = "";
  
  // use sharedPreference to write out the result of each uploading task
  private SharedPreferences sharedPreferences;
  
  @Override
  public void onCreate() {
    super.onCreate();
    sharedPreferences =  getApplicationContext().getSharedPreferences(DropboxUtil.PREFS_DROPBOX,
        Context.MODE_PRIVATE);
    Log.i(TAG, "Creating...");
    connectivityManager =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    lock = LockUtil.getWakeLock(this);
    fileFailures = new HashMap<String, Integer>();
    remoteArchiveFailures = new HashMap<String, Integer>();
    dbFilesToUpload = new ConcurrentLinkedQueue<ArchiveFile>();
    filesToUpload = new ConcurrentLinkedQueue<RegularArchiveFile>();
    // TODO: consider and add multiple upload threads
    dbUploadThread = new Thread(new Runnable() {
      @Override
      public void run() {
        Log.i(TAG, "here dbThread");
        while(Thread.currentThread().equals(dbUploadThread) && !dbFilesToUpload.isEmpty()) {
            ArchiveFile archiveFile = dbFilesToUpload.poll();
            Log.i(TAG, "now poll the archiveFile(db) from the queue");
            //runArchive method deals with uploading db archived file to dropbox
            runArchive(archiveFile.archive, archiveFile.remoteArchive, archiveFile.file, archiveFile.network);
            Log.i(TAG, "after runArchive");
        }
        dbUploadThread = null;
        stopSelf();
        
      }
    });
    
    regularUploadThread = new Thread(new Runnable() {
      @Override
      public void run() {
        Log.i(TAG, "here....");
        
        while(Thread.currentThread().equals(regularUploadThread) && !filesToUpload.isEmpty()) {
          Log.i(TAG, "in thread");
          RegularArchiveFile archiveFile = filesToUpload.poll();
          Log.i(TAG, "now poll the archiveFile from the queue");
          //runUpload method deals with uploading regular files to dropbox
          runUpload(archiveFile.remoteArchive, archiveFile.file, archiveFile.network);
        }
        regularUploadThread = null;
        stopSelf();
        Log.i(TAG, "I have killed myself, so next thread can be called to run.");
      }
    });
    
    
  }
  

  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "Starting DropboxUploadService...");
    int network = intent.getIntExtra(NETWORK, NETWORK_ANY);
    //add one more extra in the intent that passes to DropboxUploadService
    int fileType = intent.getIntExtra(FILE_TYPE, REGULAR_FILE);
    Log.i(TAG, "fileType:" + fileType);

    if (isOnline(network)) {

      if (fileType == REGULAR_FILE) {
        // just upload the stuff without all heavy-lifting backup
        Log.i(TAG, "regular file....");

        String archiveName = intent.getStringExtra(ARCHIVE_ID);
        String remoteArchiveName = intent.getStringExtra(REMOTE_ARCHIVE_ID);
        if (archiveName != null && remoteArchiveName != null) {
          RemoteFileArchive remoteArchive = getRemoteArchive(remoteArchiveName);
          if (remoteArchive != null) {
            File file = new File(archiveName);//here file name will be archiveName
            regularArchive(remoteArchive, file,network);
          }
        }
        
        
        // Start upload thread if necessary, even if no files to ensure stop
        if (regularUploadThread != null && !regularUploadThread.isAlive()) {
          Log.i(TAG, "start a new uploading thread....");
          regularUploadThread.start();
        }
      } else {
        // here we have almost identical codes as UploadService.java
        Log.i(TAG, "db file....");

        String archiveName = intent.getStringExtra(ARCHIVE_ID);
        
        String remoteArchiveName = intent.getStringExtra(REMOTE_ARCHIVE_ID);
        if (archiveName != null && remoteArchiveName != null) {
          FileArchive archive = getArchive(archiveName);
          RemoteFileArchive remoteArchive = getRemoteArchive(remoteArchiveName);
          if (archive != null && remoteArchive != null) {
            for (File file : archive.getAll()) {
              Log.i(TAG, "...loop");
              dbArchive(archive, remoteArchive, file, network);
            }
          }
        }

        // Start upload thread if necessary, even if no files to ensure stop
        if (dbUploadThread != null && !dbUploadThread.isAlive()) {
          Log.i(TAG, "start a new uploading thread(DB)....");
          dbUploadThread.start();
          
        }

      }

    }
    
    return Service.START_STICKY;
  }
  
  
  //this ArchiveFile is a place holder for keeping info about REGULAR_FILE uploading, not the db file
  protected class RegularArchiveFile {
    public final RemoteFileArchive remoteArchive;
    public final File file;
    public final int network;
    public RegularArchiveFile(RemoteFileArchive remoteArchive, File file, int network) {
 
      this.remoteArchive = remoteArchive;
      this.file = file;
      this.network = network;
    }
    @Override
    public boolean equals(Object o) {
      return o != null && o instanceof ArchiveFile 
        && EqualsUtil.areEqual(remoteArchive.getId(), ((ArchiveFile)o).remoteArchive.getId())
        && EqualsUtil.areEqual(file, ((ArchiveFile)o).file);
    }
    @Override
    public int hashCode() {
      return HashCodeUtil.hash(HashCodeUtil.hash(HashCodeUtil.SEED, file), remoteArchive.getId());
    }
    
    
  }

  /*
   * override UploadService to have our own exception handling
   */
  
  @Override
  protected void runArchive(FileArchive archive, RemoteFileArchive remoteArchive, File file, int network) {
    Integer numRemoteFailures = remoteArchiveFailures
        .get(remoteArchive.getId());
    numRemoteFailures = (numRemoteFailures == null) ? 0 : numRemoteFailures;
    Log.i(LogUtil.TAG, "numRemoteFailures:" + numRemoteFailures);
    Log.i(LogUtil.TAG, "isOnline:" + isOnline(network));
    boolean successUpload = false;

    if (numRemoteFailures < MAX_REMOTE_ARCHIVE_RETRIES && isOnline(network)) {
      Log.i(LogUtil.TAG, "Archiving..." + file.getName());

      try {
        successUpload = remoteArchive.add(file);
      } catch (Exception e) {
        // something happen that we can't successfully upload the file to
        // dropbox. Write status logs and error messages to sharedpreference  
        status = successUpload;
        error_message = getErrorMessage(e);
        
      }
 
      if (successUpload) {
        archive.remove(file);
        saveStatusLog(true, "success");
      } else {
        Integer numFileFailures = fileFailures.get(file.getName());
        numFileFailures = (numFileFailures == null) ? 1 : numFileFailures + 1;
        numRemoteFailures += 1;
        fileFailures.put(file.getName(), numFileFailures);
        remoteArchiveFailures.put(remoteArchive.getId(), numRemoteFailures);
        // 3 Attempts
        if (numFileFailures < MAX_FILE_RETRIES) {
          dbFilesToUpload.offer(new ArchiveFile(archive, remoteArchive, file,
              network));
        } else {
          Log.i(LogUtil.TAG, "Failed to upload '" + file.getAbsolutePath()
              + "' after 3 attempts.");
          saveStatusLog(status, error_message);
        }
      }
    } else {
      Log.i(LogUtil.TAG,
          "Canceling upload.  Remote archive '" + remoteArchive.getId()
              + "' is not currently available.");
      saveStatusLog(status, error_message);
    }

  }
  
  private void saveStatusLog(boolean status, String message){
    //save to sharedPreference the latest status 
    final SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    String currentDatetime = dateFormat.format(date);
    
    sharedPrefsEditor.putString(Dropbox.DROPBOX_LASTUPLOAD_TIME, currentDatetime);
    sharedPrefsEditor.putBoolean(Dropbox.DROPBOX_LASTUPLOAD_STATUS, status);
    sharedPrefsEditor.putString(Dropbox.DROPBOX_LASTUPLOAD_REPORT, message);

    sharedPrefsEditor.commit();        
    
  }
  
  
  private void runUpload(RemoteFileArchive remoteArchive, File file, int network){
    // this part of code is copied and modified from UploadService.java in funf library
    // only uses when fileType == REGULAR_FILE, else we will use UploadService.java$runArchive()
    Integer numRemoteFailures = remoteArchiveFailures.get(remoteArchive.getId());
    numRemoteFailures = (numRemoteFailures == null) ? 0 : numRemoteFailures;
    Log.i(TAG, "numRemoteFailures:" + numRemoteFailures );
    Log.i(TAG, "isOnline:" + isOnline(network) );
    boolean successUpload = false;
 
    if (numRemoteFailures < MAX_DROPBOX_RETRIES && isOnline(network)) {
      Log.i(TAG, "uploading to dropbox..." + file.getName());
      try{
        successUpload = remoteArchive.add(file);
      }catch(Exception e){
        // something happen that we can't successfully upload the file to dropbox
        status = successUpload;
        error_message = getErrorMessage(e);
      }
 
      if(successUpload) {
        Log.i(TAG, "successful upload file to dropbox");
        saveStatusLog(true, "success");
        //do nothing
      } else {
        Integer numFileFailures = fileFailures.get(file.getName());
        numFileFailures = (numFileFailures == null) ? 1 : numFileFailures + 1;
        numRemoteFailures += 1;
        fileFailures.put(file.getName(), numFileFailures);
        remoteArchiveFailures.put(remoteArchive.getId(), numRemoteFailures);
        // 3 Attempts
        if (numFileFailures < MAX_FILE_RETRIES) {
          filesToUpload.offer(new RegularArchiveFile(remoteArchive, file, network));
        } else {
          Log.i(TAG, "Failed to upload '" + file.getAbsolutePath() + "' after 3 attempts.");
          saveStatusLog(status, error_message);
        }
      }
     }else {
      Log.i(TAG, "Canceling upload.  Remote archive '" + remoteArchive.getId() + "' is not currently available.");
      saveStatusLog(status, error_message);
    } 
  }

  
  @Override
  public boolean isOnline(int network) {
    NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
    
    if (network == NETWORK_ANY && netInfo != null && netInfo.isConnectedOrConnecting()) {
        return true;
    } else if (network == NETWORK_WIFI_ONLY ) {
      Log.i(TAG,"we are in isOnline(): NETOWORK_WIFI_ONLY "+ network);
      Log.i(TAG, "Prining out debugging info of connectivityStatus: ");

      State wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
      Log.i(TAG, wifiInfo.toString());
      if (State.CONNECTED.equals(wifiInfo) || State.CONNECTING.equals(wifiInfo)) {
        return true;
      }
    }
    return false;
}
  /*
   * This is a copy of UploadService$archive(), we need to replicate this because it's used in 
   * DropboxUploadSerive$onStartCommand that override UploadService$onStartCommand
   */
  public void dbArchive(FileArchive archive, RemoteFileArchive remoteArchive, File file, int network) {
    ArchiveFile archiveFile = new ArchiveFile(archive, remoteArchive, file, network);
    if (!dbFilesToUpload.contains(archiveFile)) {
      Log.i(TAG, "Queuing " + file.getName());
      dbFilesToUpload.offer(archiveFile);
    }

  }
  
  public void regularArchive(RemoteFileArchive remoteArchive, File file, int network){
    RegularArchiveFile regularArchiveFile = new RegularArchiveFile(remoteArchive, file, network);
    if(!filesToUpload.contains(regularArchiveFile)){
      Log.i(TAG, "We are Queuing " + file.getName());
      filesToUpload.offer(regularArchiveFile);
      
    }
    
  }
  
  @Override
  public void onDestroy() {
    if (regularUploadThread != null && regularUploadThread.isAlive()) {
      regularUploadThread = null;
    }
    
    if (dbUploadThread != null && dbUploadThread.isAlive()) {
      dbUploadThread = null;
    }
    
    if (lock.isHeld()) {
      lock.release();
    }
  }
  

  
  /**
   * Binder interface to the probe
   */

  public class LocalBinder extends Binder {
    public DropboxUploadService getService() {
            return DropboxUploadService.this;
        }
    }
  private final IBinder mBinder = new LocalBinder();
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
  

  private String getErrorMessage(Exception e){
    String errorMsg = "";
    
    try {
      throw e;
    } catch (DropboxUnlinkedException exp) {
      // TODO Auto-generated catch block
      errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_TWITTER_EXCEPTION, null); 
    } catch (DropboxFileSizeException exp){
      errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_DROPBOX_FILESIZE, null);

    } catch (DropboxPartialFileException exp){
      errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_DROPBOX_PARTIALFILE, null);
    } catch (DropboxServerException exp){
      if(exp.error == DropboxServerException._507_INSUFFICIENT_STORAGE){
        errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_DROPBOX_SERVER_INSUFFICIENT_STORAGE, null);
      }

    } catch (DropboxIOException exp){
      errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_DROPBOX_IO, null);
      
    } catch (FileNotFoundException exp){
      errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_DROPBOX_FILENOTFOUND, null);
      
    } catch (Exception exp) {
      //something bad just happened
      errorMsg = ErrorMessages.formatMessage(ErrorMessages.ERROR_DROPBOX_EXCEPTION, null);
    }
        
    return errorMsg;
  }
  
  
  
}
