// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.DBTrust;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.HttpsUploadService;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import edu.mit.media.funf.security.Base64Coder;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.time.DecimalTimeUnit;

/*
 * DBTrustUtil 
 */

public class DBTrustUtil{
 
	private static final String TAG = "DBTrustUtil";
	public static final String PIPE_NAME = "DBTrust"; // this is used as the database name
	
 
	// TODO: make these two urls dynamic

	private static final String UPLOAD_URL = "http://air.csail.mit.edu:8002/connectors/set_funf_data";
	private static final String SET_KEY_URL = "http://air.csail.mit.edu:8002/connectors/set_funf_key";
	
	private String uploadURL;
	private String setKeyURL; 
	
	private static String pdsURL;
	private static String pdsPort;
	private static final String POSTFIX_UPLOAD_URL = "/connectors/set_funf_data";
	private static final String POSTFIX_SET_KEY_URL = "/connectors/set_funf_key";
	
	
	private static final long UPLOAD_PERIOD = 7200;
	private static final long ARCHIVE_PERIOD = 3600;
	private static final String ACCESS_TOKEN_KEY = "accessToken";
	private static String accessToken;
	private static SharedPreferences sharedPreferences;
	private static Handler handler = new Handler();
	protected boolean enabledUpload = false;
	protected boolean enabledArchive = false;
 
 
	private static final boolean DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY = true;
	
	// add local offset from UTC timestamp when data is saved to DB 
	private Calendar calendar = Calendar.getInstance(Locale.getDefault());
	private BigDecimal localOffsetSeconds = BigDecimal.valueOf(
			calendar.get(Calendar.ZONE_OFFSET)
					+ calendar.get(Calendar.DST_OFFSET), DecimalTimeUnit.MILLI);
	
	// OAuth stuff 
	
	private static volatile String clientID = null;
	private static volatile String clientSecret = null;
	
	final String TOKEN_URL = "http://air.csail.mit.edu:8001/oauth2/token/";
	private static String tokenURL;
	private static String registryServerURL;
	private static String registryPort;
	private final static String POSTFIX_TOKEN_URL = "/oauth2/token/";
	
	private static final String SCOPE = "funf_write";
//  Fuming's app client info (for Trustframework internal testing)
	private static final String CLIENT_ID = "062044d6b51e39e6eb8066a4516a4a";
	private static final String CLIENT_SECRET = "330c8fa32eb184dea65b6e69f39a16";
 	
	public static final String PASSWORD_KEY = "PASSWORD";

	private DBTrustUtil() {
		 

	}
	
	/*
	 *  This function is for saving name/value(json object) to remote DB. The remote DB current is a 
	 *  mongo (non-sql) database. 
	 *  It Saves data (name and value) to the remote database. " +
	 *  The value itself is a json representation in List. The list contains one two item list per key in the json object it is representing." +
	 *  Each two item list has the key String as its first element, the second element is the representation of a json object in list recursively." +
	 *	Later the name will be used to retrieve the value")	
	*/
	public static void SaveToTrustDB(Context ctxt, String name, YailList value){
		 // The name will be used to retrieve the value in the remote db later (either through HTTP request e.g. someurl&name
		 // all key/value pair will be coupled with TIMESTAMP when save to remote DB
		 
		 final long timestamp = System.currentTimeMillis()/1000;
		 
		 Bundle b = new Bundle();
			b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, PIPE_NAME);
			b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
			b.putString(NameValueDatabaseService.NAME_KEY, name);
			
			String jsonVal = value.toJSONString(); // 
			b.putString(NameValueDatabaseService.VALUE_KEY, jsonVal);
			Intent i = new Intent(ctxt, NameValueDatabaseService.class);
			i.setAction(DatabaseService.ACTION_RECORD);
			i.putExtras(b);
			ctxt.startService(i);
	 }
	
	/*
	 * This function is used to save 
	 */
	public static void SaveToTrustDB(String name, Object value){
		
		
	}

	
	
	public static void archiveData(Context ctxt) {

		Intent i = new Intent(ctxt, NameValueDatabaseService.class);
		Log.i(TAG, "archiving data.");
		i.setAction(DatabaseService.ACTION_ARCHIVE);
		i.putExtra(DatabaseService.DATABASE_NAME_KEY, PIPE_NAME);
		ctxt.startService(i);

	}
	
	
	public static Class<? extends UploadService> getUploadServiceClass() {
		return HttpsUploadService.class;
	}
	
	
	public static void uploadData(Context ctxt, boolean wifiOnly) {
		archiveData(ctxt);
		String archiveName = PIPE_NAME;
//		String uploadUrl = UPLOAD_URL;
		String uploadUrl = getUploadURL();
		Intent i = new Intent(ctxt, getUploadServiceClass());
		i.putExtra(UploadService.ARCHIVE_ID, archiveName);
		i.putExtra(UploadService.REMOTE_ARCHIVE_ID, uploadUrl);
		i.putExtra(UploadService.NETWORK,
				(wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
						: UploadService.NETWORK_ANY);
		ctxt.startService(i);

	}
	
 
	private static String getUploadURL(){
		return pdsURL + ":" + pdsPort + POSTFIX_UPLOAD_URL;
	}
	private static String getSetKeyURL(){
		return pdsURL + ":" + pdsPort + POSTFIX_SET_KEY_URL;
	}
	private static String getTokenURL(){
		return registryServerURL + ":"+ registryPort + POSTFIX_TOKEN_URL;
	}
	
	  /**
	   * Authenticate to TrustFramework using OAuth
 	   * Redirects user to login to Trust Framework via the Http and
	   * obtain the accessToken if we don't already have authorization
	   *  
	   */
	public static void authorize(final Form form, DBTrust callerComp,
			        String username, String password) {
		// if success, write access_token = prefs.getString("accessToken", "");
		// as true into SharedPreference,
		  final String myUsername = username;
		  final String myPassword = password;
		  final Context callerContext = form.$context();
		  final DBTrust callerComponent = callerComp;
		  
		  final String _clientId = (clientID == null)? CLIENT_ID:clientID;
		  final String _clientSecret = (clientSecret == null)? CLIENT_SECRET:clientSecret;
		  
		  final String authHeader = Base64Coder.encodeString(_clientId + ":" + _clientSecret);
		  
		  // we also need to dynamically construct the urls for trustframework
		  final String uploadURL = getUploadURL();
		  final String setKeyURL = getSetKeyURL();
		  final String tokenURL = getTokenURL();
 
		  // debugging
		  Log.i(TAG, "uploadURL:" + uploadURL);
		  Log.i(TAG, "setKeyURL:" + setKeyURL);
		  Log.i(TAG, "tokenURL:" + tokenURL);
		  Log.i(TAG, "_clientId:" + _clientId);
		  Log.i(TAG, "_clientSecret:" + _clientSecret);
		  Log.i(TAG, "authHeader:" + authHeader);
		  
		  

		AsynchUtil.runAsynchronously(new Runnable() {
			public void run() {

				try {
					HttpClient httpclient = new DefaultHttpClient();
					//HttpPost httppost = new HttpPost(TOKEN_URL);
					HttpPost httppost = new HttpPost(tokenURL);
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
							2);

					nameValuePairs.add(new BasicNameValuePair("grant_type",
							"password"));

					nameValuePairs.add(new BasicNameValuePair("scope", SCOPE));
					nameValuePairs.add(new BasicNameValuePair("username",
							myUsername));
					nameValuePairs.add(new BasicNameValuePair("password",
							myPassword));
					
					nameValuePairs.add(new BasicNameValuePair("client_id",
							_clientId));
					nameValuePairs.add(new BasicNameValuePair("client_secret",
							_clientSecret));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					
					httppost.addHeader(
							"AUTHORIZATION",
							"basic "
									+ authHeader);

					
					Log.i(TAG, "URI:" + httppost.getURI());
					Log.i(TAG, "Before sending http request for accessToken");
					
					HttpResponse response = httpclient.execute(httppost);
					
					if (response == null) {
						throw new Exception("Http respone is null");

					}
					JsonObject json = getResponseJSON(response)
							.getAsJsonObject();
					Log.i(TAG, "return json:" + json);
					JsonElement error = json.get("error");
 
					String access_token = json.get("access_token")
								.getAsString();
 
					String refresh_token = json.get("refresh_token")
								.getAsString();
 
					Long expire_in = json.get("expires_in").getAsLong();
					if (json == null) {
						Log.e(TAG, "Could not parse http response into json");
						throw new Exception(
								"Http rsponse could not be parsed into json");
					}
					else if (error != null) {
						Log.e(TAG,
								"Http rsponse to token request contained an error");
						throw new Exception(
								"Http rsponse to token request contained an error");
					} else if (access_token != null & refresh_token != null
							& expire_in != null) {
						// validate the oauth response with included access and
						// refresh
						// token
						Log.i(TAG, "Obtained Token" + access_token);
						Editor editor = sharedPreferences.edit();
						editor.putString(ACCESS_TOKEN_KEY, access_token);
						editor.putString("refreshToken", refresh_token);
						editor.putString("expiresIn", String.valueOf(expire_in));
						editor.commit();

					} else// the token was invalid
					{
						Log.e(TAG, "Unexpected error in getting token response");
						throw new Exception(
								"Unexpected error in getting token response");
					}

					// if(isTokenValid())
					if (true) {
						if (!sharedPreferences.contains("PASSWORD")) {
							try {
								getDataPassword(sharedPreferences);
								List<NameValuePair> funfnameValuePairs = new ArrayList<NameValuePair>(
										1);

								funfnameValuePairs.add(new BasicNameValuePair(
										"funf_key", sharedPreferences
												.getString("PASSWORD", "")));
								funfnameValuePairs.add(new BasicNameValuePair(
										"overwrite", "true"));// remove this
																// after
								// testing!
								sendAuthorizedGetRequest(
										funfnameValuePairs,
										getSetKeyURL(),
										sharedPreferences);
								
						          handler.post(new Runnable() {
						              @Override
						              public void run() {
						                callerComponent.UploadIsAuthorized();
						                // this function will call back to AI component to tell it 
						                // is ready to upload
						              }
						            });

							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

					} else {

						Log.e(TAG, "Server returned an invlid access token.");
						throw new Exception(
								"Server returned an invlid access token.");
					}

				} catch (NetworkErrorException e) {
					//need to have more different type of exception here. Look at Twitter component
					Log.e(TAG, "exception:", e);
				}
				catch (Exception e){
					form.dispatchErrorOccurredEvent(callerComponent, "Authorize",
			                  ErrorMessages.ERROR_WEB_UNABLE_TO_GET, e.getMessage());
				}
			}
		});

	}
	  
	private static String getDataPassword(SharedPreferences prefs) {

		if (prefs.contains(PASSWORD_KEY))
			return prefs.getString(PASSWORD_KEY, null);
		String password = prefs.getString(PASSWORD_KEY, null);
		if (password == null) {
			// Set the password in the pipeline the first time it is created
			// password = randomPassword().substring(0, 8);//our service only
			// supports 8 chars...there might be a better way to do this.
			// This password is used to encrypt the data. 
			password = "changeme"; 
			prefs.edit().putString(PASSWORD_KEY, password).commit();

			// TODO: queue UUID, password to be sent to server
		}
		Log.i(TAG, "Password set to: " + password);
		return password;
	}

	private static JsonElement getResponseJSON(HttpResponse response) throws Exception {
		try {
			HttpEntity ht = response.getEntity();
			BufferedHttpEntity buf = new BufferedHttpEntity(ht);
			InputStream is = buf.getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));

			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
			}
			JsonParser jsonParser = new JsonParser();
			// JSONParser parser = new JSONParser();
			JsonElement json = jsonParser.parse(total.toString());
			if (json == null) {
				Log.e(TAG, "Could not parse http response into json");
				throw new Exception(
						"Http rsponse could not be parsed into json");
			}

			return json;
		} catch (IOException ex) {
			Log.e("GetAccessToken", "IOException " + ex.getMessage());
			return null;
		} catch (JsonParseException ex) {
			Log.e("GetAccessToken", "ParseException " + ex.getMessage());
			return null;
		}
	}
	
	public static HttpResponse sendAuthorizedGetRequest(
			List<NameValuePair> nameValuePairs, String url,
			SharedPreferences prefs) throws Exception {
		HttpResponse response = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();
			String access = prefs.getString("accessToken", "");
			BasicNameValuePair pair = new BasicNameValuePair("bearer_token",
					access);
			if (nameValuePairs == null) {
				nameValuePairs = new ArrayList<NameValuePair>(1);
			}
			nameValuePairs.add(pair);
			String paramString = URLEncodedUtils
					.format(nameValuePairs, "utf-8");
			url += "?" + paramString;
			HttpGet httpget = new HttpGet(url);
			httpget.setHeader("Authorization",
					"Bearer " + prefs.getString("accessToken", null));
			response = httpclient.execute(httpget);
		} catch (IOException ex) {

		}
		return response;

	}
	
	
 
	/*
	 * Set the URLs and Ports for Remote PDS servers and Registry server 
	 *  (personal data store is the place to store uloaded data; registry server is the one that issues OAuth2 token)
	 */
	
	public static void SetDBTrust(String pdsURL, String pdsPort, String registryURL, String registryPort){
		
		DBTrustUtil.pdsURL = pdsURL;
		DBTrustUtil.pdsPort = pdsPort;
		DBTrustUtil.registryPort = registryPort;
		DBTrustUtil.registryServerURL = registryURL;
	}

	
}
