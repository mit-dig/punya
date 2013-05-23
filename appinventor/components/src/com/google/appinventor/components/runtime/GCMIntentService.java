package com.google.appinventor.components.runtime;

import java.util.HashSet;
import java.util.Random;

import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.runtime.GCMRegistrar;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";
    protected Context mainUIThreadContext;
    
    private String SERVER_URL = "";
    
    private SharedPreferences sharedPreferences;
    private static final String REG_ID_TAG = "RegistrationId";
    
    // notification
    private Notification notification;
    private PendingIntent mContentIntent;
    private NotificationManager mNM;
    private final int PROBE_NOTIFICATION_ID = 1;
    
    private String optInOrOut = "";
    
    // Set of listeners for any changes of the form
    final HashSet<GCMEventListener> GCMEventListeners = new HashSet<GCMEventListener>();
    final HashSet<GCMEventListener> GCMRegEventListeners = new HashSet<GCMEventListener>();

    public GCMIntentService() {
        super();
        Log.i(TAG,"Start the GCMIntentServices");
        sharedPreferences = null;
    }
           
    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message");        
        Log.i(TAG, "The context is"+context.toString());
        mainUIThreadContext = context;
        
        // get Notification Manager
        String ns = mainUIThreadContext.NOTIFICATION_SERVICE;
        Log.i(TAG, "Before creating the GCMIntentService");
        mNM = (NotificationManager) mainUIThreadContext.getSystemService(ns);
        Log.i(TAG, "After creating the GCMIntentService");  
        
        String newMessage = intent.getExtras().getString("message");
                
        sharedPreferences = context.getSharedPreferences("GCMIntentService",Context.MODE_PRIVATE);
        Log.i(TAG, "The shared preference is "+sharedPreferences.toString()); 
        
        String optPref;
        if (optInOrOut.equals("")){
            optPref = sharedPreferences.getString(GCMConstants.PREFS_GCM_MESSAGE, "");
        }else{
            optPref = optInOrOut;
            final SharedPreferences.Editor sharedPrefsEditor =
            sharedPreferences.edit();
            sharedPrefsEditor.putString(GCMConstants.PREFS_GCM_MESSAGE,optPref);
            sharedPrefsEditor.commit();
        } 
        Log.i(TAG, "The opt preference is "+optPref);
              
        if (optPref.equals("in")){
            try {
                CreateNotification("You got message.","Please press to open.",true,true,"appinventor.ai_test.GCM",
                        "appinventor.ai_test.GCM.Screen1",null,null);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        for (GCMEventListener listener : GCMEventListeners) {
            listener.onMessageReceived(newMessage);
            Log.i(TAG, "Listener:" + listener.toString());
          }       
        Log.i(TAG, "After the onMessage method");         
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        boolean success = GCMServerUtilities.register(context, registrationId,SERVER_URL);
        if(success){
            for (GCMEventListener listener : GCMRegEventListeners) {
                listener.onMessageReceived("success");
                Log.i(TAG, "Listener:" + listener.toString());
              }    
        }      
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
        //displayMessage(context, getString(R.string.gcm_unregistered));
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            GCMServerUtilities.unregister(context, registrationId,SERVER_URL);
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
    
    // This is a method for App Inventor's component to receive two types of messages from Google GCM
    // 1. general GCM message 2. registeration finished message
    public void requestGCMMessage(Context context, GCMEventListener listener, String type){
        //add the listener to the list of listerners
        if(type.equals(GoogleCloudMessaging.MESSAGE_GCM_TYPE))
            GCMEventListeners.add(listener);
        else{//for registeration type of messages
            GCMRegEventListeners.add(listener);
        }
        
        //save the GCM Message opt in/out preference to sharedPreference for later use
        //1 => opt in
        //2 => opt out
        sharedPreferences = context.getSharedPreferences("GCMIntentService",Context.MODE_PRIVATE);
        Log.i(TAG, "The shared preference is "+sharedPreferences.toString()); 
        
        final SharedPreferences.Editor sharedPrefsEditor =
        sharedPreferences.edit();
        sharedPrefsEditor.putString(GCMConstants.PREFS_GCM_MESSAGE,"in");
        sharedPrefsEditor.commit();
        optInOrOut = "in";
    }
    
    // This is a method for App Inventor's component to receive two types of messages from Google GCM
    // 1. general GCM message 2. registeration finished message
    public void unRequestGCMMessage(Context context, GCMEventListener listener, String type){
        //add the listener to the list of listerners
        if(type.equals(GoogleCloudMessaging.MESSAGE_GCM_TYPE))
            if (!GCMEventListeners.isEmpty()){
                GCMEventListeners.remove(listener);
            }           
        else{//for registeration type of message
            if (!GCMRegEventListeners.isEmpty()){
                GCMRegEventListeners.remove(listener);
            }
        }
        
        //save the GCM Message opt in/out preference to sharedPreference for later use
        //1 => opt in
        //2 => opt out
        
        //2 => opt out
        sharedPreferences = context.getSharedPreferences("GCMIntentService",Context.MODE_PRIVATE);
        Log.i(TAG, "The shared preference is "+sharedPreferences.toString()); 
        
        final SharedPreferences.Editor sharedPrefsEditor =
        sharedPreferences.edit();
        sharedPrefsEditor.putString(GCMConstants.PREFS_GCM_MESSAGE,"out");
        sharedPrefsEditor.commit();
        optInOrOut = "out";
    }
    
    public void setSenderID(String sender_id){
        setSenderIds(sender_id);
    }
    
    public void setServerURL(String url){
        SERVER_URL = url;
    }
    
    /*
     * Add notification with some message and the app (actually it's
     * app.Screen1) it wants to activate
     * 
     * @param title
     * 
     * @param text
     * 
     * @param enabledSound
     * 
     * @param enabledVibrate
     * 
     * @param appName
     * 
     * @param extraKey
     * 
     * @param extraVal
     */

    @SimpleFunction(description = "Create a notication with message to wake up "
            + "another activity when tap on the notification")
    public void CreateNotification(String title, String text,
            boolean enabledSound, boolean enabledVibrate, String packageName,
            String className, String extraKey, String extraVal)
            throws ClassNotFoundException {

        Intent activityToLaunch = new Intent(Intent.ACTION_MAIN);

        Log.i(TAG, "packageName: " + packageName);
        Log.i(TAG, "className: " + className);

        // for local AI instance, all classes are under the package
        // "appinventor.ai_test"
        // but for those runs on Google AppSpot(AppEngine), the package name
        // will be
        // "appinventor.ai_GoogleAccountUserName"
        // e.g. pakageName = appinventor.ai_HomerSimpson.HelloPurr
        // && className = appinventor.ai_HomerSimpson.HelloPurr.Screen1

        ComponentName component = new ComponentName(packageName, className);
        activityToLaunch.setComponent(component);
        activityToLaunch.putExtra(extraKey, extraVal);

        Log.i(TAG, "we found the class for intent to send into notificaiton");
        activityToLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Log.i(TAG, "after the activityToLaunch");
        Log.i(TAG,"The mainUImainUIThreadContext is "+mainUIThreadContext.toString()); 
        Log.i(TAG,"The activityToLaunch is "+activityToLaunch.toString());         
        mContentIntent = PendingIntent.getActivity(mainUIThreadContext, 0,
                activityToLaunch, 0);

        Log.i(TAG, "after the PendingIntent.getActivity");
        Long currentTimeMillis = System.currentTimeMillis();
        
        Log.i(TAG, "after the System.currentTimeMillis");
        notification = new Notification(R.drawable.stat_notify_chat,
                "GCM Notification!", currentTimeMillis);

        Log.i(TAG, "After creating notification");
        notification.contentIntent = mContentIntent;
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        // reset the notification
        notification.defaults = 0;

        if (enabledSound)
            notification.defaults |= Notification.DEFAULT_SOUND;

        if (enabledVibrate)
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        notification.setLatestEventInfo(mainUIThreadContext,
                (CharSequence) title, (CharSequence) text, mContentIntent);
        Log.i(TAG, "after updated notification contents");
        mNM.notify(PROBE_NOTIFICATION_ID, notification);
        Log.i(TAG, "notified");
    }

}
