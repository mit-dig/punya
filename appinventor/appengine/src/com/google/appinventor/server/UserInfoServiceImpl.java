// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import java.security.cert.Certificate;

import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.shared.rpc.user.User;
import com.google.appinventor.shared.rpc.user.UserInfoService;
import com.google.appinventor.shared.storage.StorageUtil;

/**
 * Implementation of the user information service.
 *
 * <p>Note that this service must be state-less so that it can be run on
 * multiple servers.
 *
 */
public class UserInfoServiceImpl extends OdeRemoteServiceServlet implements UserInfoService {

  // Storage of user settings
  private final transient StorageIo storageIo = StorageIoInstanceHolder.INSTANCE;

  private static final long serialVersionUID = -7316312435338169166L;

  /**
   * Returns user information.
   *
   * @return  user information record
   */
  @Override
  public User getUserInformation() {
    return userInfoProvider.getUser();
  }

  /**
   * Retrieves the user's settings.
   *
   * @return  user's settings
   */
  @Override
  public String loadUserSettings() {
    return storageIo.loadSettings(userInfoProvider.getUserId());
  }

  /**
   * Stores the user's settings.
   * @param settings  user's settings
   */
  @Override
  public void storeUserSettings(String settings) {
    storageIo.storeSettings(userInfoProvider.getUserId(), settings);
  }

  /**
   * Returns true if the current user has a user file with the given file name
   */
  @Override
  public boolean hasUserFile(String fileName) {
    return storageIo.getUserFiles(userInfoProvider.getUserId()).contains(fileName);
  }

  /**
   * Deletes the user file with the given file name
   */
  @Override
  public void deleteUserFile(String fileName) {
    storageIo.deleteUserFile(userInfoProvider.getUserId(), fileName);
  }

  /**
   * Get the SHA1 fingerprint for the user's keystore
   */
  @Override
  public String getUserFingerprintSHA1() {
    final String userId = userInfoProvider.getUserId();
    byte[] keystoreBytes = storageIo.downloadRawUserFile(userId, StorageUtil.ANDROID_KEYSTORE_FILENAME);
    BufferedReader br = null;
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(keystoreBytes);
      KeyStore store = KeyStore.getInstance("jks");
      char[] pass = "android".toCharArray();
      store.load(bais, pass);
      Certificate cert = store.getCertificateChain("androidkey")[0];
      MessageDigest md = MessageDigest.getInstance( "SHA1" );
      String unprocessed = new BigInteger( 1, md.digest( cert.getEncoded() ) ).toString(16).toUpperCase();
      StringBuffer sb = new StringBuffer();
      for(int i=0;i<unprocessed.length();i++) {
        sb.append(unprocessed.charAt(i));
        if(i % 2 == 1 && i != unprocessed.length()-1) {
          sb.append(":");
        }
      }
      return sb.toString();
//      String[] keytoolCommandline = {
//          System.getProperty("java.home") + "/bin/keytool",
//          "-v", "-list",
//          "-keystore", keystoreFile.getAbsolutePath(),
//          "-alias", "AndroidKey",
//          "-storepass", "android",
//          "-keypass", "android"
//      };
//      ProcessBuilder processBuilder = new ProcessBuilder(keytoolCommandline);
//      Process process = processBuilder.start();
//      process.waitFor();
//      keystoreFile.delete();
//      BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
//      br = new BufferedReader(new InputStreamReader(bis));
//      String line = null;
//      while((line = br.readLine()) != null) {
//        if(line.contains("SHA1")) {
//          String[] bits = line.split(": ");
//          System.err.println("Keystore fingerprint: "+bits[1]);
//          return bits[1];
//        }
//      }
//      return "";
    } catch(IOException e) {
      
    } catch (KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CertificateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if(br != null) {
        try {
          br.close();
        } catch(IOException e) {
          
        }
      }
    }
    return null;
  }
}
