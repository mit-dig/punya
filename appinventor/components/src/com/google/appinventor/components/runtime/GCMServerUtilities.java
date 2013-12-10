/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.appinventor.components.runtime;


import com.google.appinventor.components.runtime.GCMRegistrar;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Helper class used to communicate with the demo server.
 */
public final class GCMServerUtilities {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();
    
    private static final String TAG = "ServerUtilities";
    //URL for webapp from Google App Scripts (GAS)
    private static final String GAS_BASE = "https://script.google.com/macros";
    
    //This need to be dynamically assigne.
    //static String SERVER_URL = "";
   
    /**
     * Register this account/device pair within the server.
     *
     * @return whether the registration succeeded or not.
     */
    static boolean register(final Context context, final String regId, String SERVER_URL) {
        Log.i(TAG, "registering device (regId = " + regId + ")");
        String serverUrl = "";        
        Map<String, String> params = new HashMap<String, String>();
        
        // If the webapp is from GAS, we need to add redirect url extension to the params because Google
        // does not allow that.
        if(SERVER_URL.contains(GAS_BASE)){
        	serverUrl = SERVER_URL;
        	params.put("type", "register");
        	
        }        
        else {
        	serverUrl = SERVER_URL + "/register";
        }
        
        params.put("regId", regId);
        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Log.d(TAG, "Attempt #" + i + " to register");
            try {
                post(serverUrl, params);
                GCMRegistrar.setRegisteredOnServer(context, true);
                return true;
            } catch (IOException e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return false;
    }
    


    /**
     * Unregister this account/device pair within the server.
     */
    static void unregister(final Context context, final String regId, String SERVER_URL) {
        Log.i(TAG, "unregistering device (regId = " + regId + ")");
        
        Log.i(TAG, "registering device (regId = " + regId + ")");
        String serverUrl = "";        
        Map<String, String> params = new HashMap<String, String>();
        
        // If the webapp is from GAS, we need to add redirect url extension to the params because Google
        // does not allow that.
        if(SERVER_URL.contains(GAS_BASE)){
        	serverUrl = SERVER_URL;
        	params.put("type", "unregister");
        	
        }        
        else {
        	serverUrl = SERVER_URL + "/unregister";
        }

        params.put("regId", regId);
        try {
            Log.i(TAG,"Before post method, the params is "+params.toString());
            post(serverUrl, params);
            Log.i(TAG,"Before the GCMRegistrar.setRegisteredOnServer");
            GCMRegistrar.setRegisteredOnServer(context, false);
            Log.i(TAG,"After the GCMRegistrar.setRegisteredOnServer");
        } catch (IOException e) {
            Log.i(TAG,"Exception is "+e.toString());
        }
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        Log.v(TAG, "Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            Log.v(TAG, "After open the Http URL connection");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            Log.v(TAG, "Before the OutputStream");
            OutputStream out = conn.getOutputStream();
            Log.v(TAG, "After the OutputStream");
            out.write(bytes);
            Log.v(TAG, "After the out.write(bytes)");
            out.close();
            Log.v(TAG, "After the out.close()");
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
              Log.v(TAG, "Post failed with error code " + status);
              throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
                Log.v(TAG, "After conn.disconnect()");
            }
        }
      }
}
