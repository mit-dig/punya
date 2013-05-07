// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
// for OAuth https upload once we have the access_token
public class HttpsArchive implements RemoteFileArchive {
	private static final String TAG = "RemoteFileArchive";
	private String uploadUrl;
	private String mimeType;
  private String access_token;
	
	public HttpsArchive(final String uploadUrl, String access_token) {
		this(uploadUrl, "application/x-binary", access_token);
	}
	
	public HttpsArchive(final String uploadUrl, final String mimeType, String access_token) {
		this.uploadUrl = uploadUrl;
		this.mimeType = mimeType;
    this.access_token = access_token;
	}
	
	public String getId() {
		return uploadUrl;
	}
	
	public boolean add(File file) {
		return HttpArchive.isValidUrl(uploadUrl) ? uploadFile(file, uploadUrl, access_token) : false;
	}
	
	// always verify the host - dont check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
	        public boolean verify(String hostname, SSLSession session) {
	                return true;
	        }
	};

	
	/**
	 * Trust every server - dont check for any certificate
	 */
	public static void trustAllHosts() {
	        // Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
	                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                        return new java.security.cert.X509Certificate[] {};
	                }

	                public void checkClientTrusted(X509Certificate[] chain,
	                                String authType) throws CertificateException {
	                }

	                public void checkServerTrusted(X509Certificate[] chain,
	                                String authType) throws CertificateException {
	                }
	        } };

	        // Install the all-trusting trust manager
	        try {
	                SSLContext sc = SSLContext.getInstance("TLS");
	                sc.init(null, trustAllCerts, new java.security.SecureRandom());
	                HttpsURLConnection
	                                .setDefaultSSLSocketFactory(sc.getSocketFactory());
	        } catch (Exception e) {
	                e.printStackTrace();
	        }
	}
	
	/**
	 * Copied (and slightly modified) from Friends and Family
	 * @param file
	 * @param uploadurl
	 * @return
	 */
	public static boolean uploadFile(File file,String uploadurl, String access_token) {
		HttpURLConnection conn = null; 
		DataOutputStream dos = null; 
		DataInputStream inStream = null; 

    Log.d("UPLOADDATA", access_token);

		String lineEnd = "\r\n"; 
		String twoHyphens = "--"; 
		String boundary =  "*****"; 

		int bytesRead, bytesAvailable, bufferSize; 
		byte[] buffer; 
		int maxBufferSize = 64*1024; //old value 1024*1024 

		boolean isSuccess = true;
		try 
		{ 
      Log.d("UPLOADDATA", "starting file upload");
			//------------------ CLIENT REQUEST 
			FileInputStream fileInputStream = null;
			//Log.i("FNF","UploadService Runnable: 1"); 
			try {
				fileInputStream = new FileInputStream(file); 
			}catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, "file not found");
			}
			// open a URL connection to the Servlet 
      Log.d("UPLOADDATA", "upload url: "+uploadurl);
      		String query = String.format("bearer_token=%s", URLEncoder.encode(access_token));
			URL url = new URL(uploadurl + "?" + query); 
			// Open a HTTP connection to the URL 
			if (url.getProtocol().toLowerCase().equals("https")) {
	            trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
	        } else {
	            conn = (HttpURLConnection) url.openConnection();
	        }
			Log.d("UPLOADDATA", "upload url with beaer_token: "+ url.toString());
			// Allow Inputs 
			conn.setDoInput(true); 
			// Allow Outputs 
			conn.setDoOutput(true); 
			// Don't use a cached copy. 
			conn.setUseCaches(false); 
			// set timeout
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			// Use a post method. 
			conn.setRequestMethod("POST"); 
			conn.setRequestProperty("Connection", "Keep-Alive"); 
			conn.setRequestProperty("Authorization", "Bearer " +access_token); 
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary); 

			dos = new DataOutputStream( conn.getOutputStream() ); 
			dos.writeBytes(twoHyphens + boundary + lineEnd); 
			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + file.getName() +"\"" + lineEnd); 
			dos.writeBytes(lineEnd); 

			//Log.i("FNF","UploadService Runnable:Headers are written"); 

			// create a buffer of maximum size 
			bytesAvailable = fileInputStream.available(); 
			bufferSize = Math.min(bytesAvailable, maxBufferSize); 
			buffer = new byte[bufferSize]; 

			// read file and write it into form... 
			bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
			while (bytesRead > 0) 
			{ 
				dos.write(buffer, 0, bufferSize); 
				bytesAvailable = fileInputStream.available(); 
				bufferSize = Math.min(bytesAvailable, maxBufferSize); 
				bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
			} 

			// send multipart form data necesssary after file data... 
			dos.writeBytes(lineEnd); 
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd); 

			// close streams 
			//Log.i("FNF","UploadService Runnable:File is written"); 
			fileInputStream.close(); 
			dos.flush(); 
			dos.close(); 
		} 
		catch (Exception e) 
		{ 
			
			Log.e("FNF", "UploadService Runnable:Client Request error", e);
			Log.e("UPLOADDATA", "Error: "+ e.getMessage() );
			isSuccess = false;
		} 

		//------------------ read the SERVER RESPONSE 
		try {
			if (conn.getResponseCode() != 200) {
				Log.e("FNF", "responseCode not = 200: " + conn.getResponseCode());
				isSuccess = false;
			}
		} catch (IOException e) {
			Log.e("FNF", "Connection error", e);
			isSuccess = false;
		}

    Log.d("UPLOADDATA", "ending file upload");
    Log.d("UPLOADDATA", "uploadFile_success?:" + isSuccess);
		return isSuccess;
	}

}
