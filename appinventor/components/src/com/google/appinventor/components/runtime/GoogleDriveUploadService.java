package com.google.appinventor.components.runtime;

import java.util.HashSet;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;

public class GoogleDriveUploadService extends UploadService {

  final HashSet<GoogleDriveExceptionListener> allListeners = new HashSet<GoogleDriveExceptionListener>();
  
  
  @Override
  protected RemoteFileArchive getRemoteArchive(String id) {
    return new GoogleDriveArchive(getApplicationContext() );
  }
  
  
  /**
   * Binder interface to the probe
   */

  public class LocalBinder extends Binder {
    public GoogleDriveUploadService getService() {
            return GoogleDriveUploadService.this;
        }
    }
  private final IBinder mBinder = new LocalBinder();
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
  
  
  /*
   * for activity (bound AI component) to register for exception
   */
  public void registerException(GoogleDriveExceptionListener listener) {
    // TODO Auto-generated method stub
    allListeners.add(listener);
    
    
  }

}
