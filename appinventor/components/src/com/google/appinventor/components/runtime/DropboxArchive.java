// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.io.File;

import com.dropbox.client2.exception.DropboxException;
import com.google.appinventor.components.runtime.util.DropboxUtil;

import android.content.Context;

import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
/*
 * This class is the funf-style class to implement a kind of RemoteFileArchive  
 * that called within a UploadService class
 */
public class DropboxArchive implements RemoteFileArchive{
  
  // just a fake id for implmenting the interface
  public static final String DROPBOX_ID = "dropbox://appinventor/__ID__"; 
  
  private Context context;
  
  public DropboxArchive(Context context) {
    this.context = context;
  }
  
  @Override
  public boolean add(File file) throws Exception{
 
      return DropboxUtil.uploadDataFile(context, file);
 
  }
  
 

  @Override
  public String getId() {
    return DROPBOX_ID;
  }
  
}
