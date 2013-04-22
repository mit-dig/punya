package com.google.appinventor.components.runtime;

import java.util.HashSet;
import java.util.Random;

import com.google.appinventor.components.runtime.GCMRegistrar;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";
    // Set of listeners for any changes of the form
    final HashSet<GCMEventListener> GCMEventListeners = new HashSet<GCMEventListener>();

    public GCMIntentService() {
        super("895146158148");
        Log.i(TAG, "GCMIntentService start");
    }
   
    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message");
        String newMessage = intent.getExtras().getString("message");
        for (GCMEventListener listener : GCMEventListeners) {
            listener.onMessageReceived(newMessage);
            Log.i(TAG, "Listener:" + listener.toString());
          }
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        //displayMessage(context, getString(R.string.gcm_registered));
        GCMServerUtilities.register(context, registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
        //displayMessage(context, getString(R.string.gcm_unregistered));
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            GCMServerUtilities.unregister(context, registrationId);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
            Log.i(TAG, "Ignoring unregister callback");
        }
    }

    @Override
    protected void onError(Context arg0, String arg1) {
        // TODO Auto-generated method stub
    }
    
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        GCMIntentService getService() {
            // Return this instance of LocalService so clients can call public methods
            return GCMIntentService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** method for clients */
    public int getRandomNumber() {
      return mGenerator.nextInt(100);
    }
    
    public void requestGCMMessage(GCMEventListener listener){
        //add the listener to the list of listerners
        GCMEventListeners.add(listener);
    }
}
