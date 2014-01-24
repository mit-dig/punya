package com.google.appinventor.components.runtime;

import java.util.HashSet;
import java.util.Random;

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

import com.google.appinventor.components.annotations.SimpleFunction;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";
    protected Context mainUIThreadContext;
    
    private String SERVER_URL = "";
    
    private SharedPreferences sharedPreferences;
    
    public static String PACKAGE_NAME;
    
    // notification
    private static final String ARGUMENT_GCM = "APP_INVENTOR_GCM";
    private static final String GCM_MESSAGE_PLAYLOAD_KEY = "gcmMessage";
    private Notification notification;
    private PendingIntent mContentIntent;
    private NotificationManager mNM;
    private final int PROBE_NOTIFICATION_ID = 1;
    
    private String optInOrOut = "";
    
    // Set of listeners for any changes of the form
    final HashSet<GCMEventListener> GCMEventListeners = new HashSet<GCMEventListener>();
    final HashSet<GCMEventListener> GCMRegEventListeners = new HashSet<GCMEventListener>();
    final HashSet<GCMEventListener> GCMSysEventListeners = new HashSet<GCMEventListener>();

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
        String newMessage;
        
        PACKAGE_NAME = mainUIThreadContext.getPackageName();
        Log.i(TAG,"The package name is "+PACKAGE_NAME+".");
        
        // get Notification Manager
        String ns = mainUIThreadContext.NOTIFICATION_SERVICE;
        Log.i(TAG, "Before creating the GCMIntentService");
        mNM = (NotificationManager) mainUIThreadContext.getSystemService(ns);
        Log.i(TAG, "After creating the GCMIntentService");  
        

        if (!intent.getExtras().containsKey(GCM_MESSAGE_PLAYLOAD_KEY)) {
        	newMessage = "Error: This message didn't contain the gcmMessage field in the extras.";
        } else {
            newMessage = intent.getExtras().getString(GCM_MESSAGE_PLAYLOAD_KEY); 
        }

        sharedPreferences = context.getSharedPreferences(GCMConstants.PREFS_GOOGLECLOUDMESSAGING,Context.MODE_PRIVATE);
        Log.i(TAG, "The shared preference is "+sharedPreferences.toString()); 
        
        String optPref;
        if (optInOrOut.equals("")){
            optPref = sharedPreferences.getString(GCMConstants.PREFS_GCM_MESSAGE, "");
        }else{
            optPref = optInOrOut;
            setSharedPreference(context,optPref);
        } 
        Log.i(TAG, "The opt preference is "+optPref);
              
        // When the user opts in to get message, but the listener list is empty. This happens when
        // the activity is killed, and then GCMIntentService is the new instance just instantiated 
        // by the GCMBrocastReceiver.
        if (optPref.equals("in") && GCMEventListeners.isEmpty()){
            try {
                CreateNotification("You got message.","Press to open.",true,true,PACKAGE_NAME,
                        PACKAGE_NAME+".Screen1",ARGUMENT_GCM,newMessage);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        for (GCMEventListener listener : GCMEventListeners) {
            listener.onMessageReceived(newMessage);
            Log.i(TAG, "The new message is :" + newMessage);
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
                listener.onMessageReceived("Registered successfully with GCM 3rd party server!");
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
        if((!GCMEventListeners.contains(listener))&&(!GCMEventListeners.contains(listener))){
            Log.i(TAG, "No listener registered."); 
            //add the listener to the list of listerners
            if(type.equals(GoogleCloudMessaging.MESSAGE_GCM_TYPE))
                GCMEventListeners.add(listener);
            else{//for registeration type of messages
                GCMRegEventListeners.add(listener);
            }
            //Save the action into the sharedPreference
            setSharedPreference(context,"in");
            Log.i(TAG, "After the requestGCMMessage"); 
        }
    }
    
    // This is a method for App Inventor's component to option out two types of messages from Google GCM
    // 1. general GCM message 2. registeration finished message
    public void unRequestGCMMessage(Context context, GCMEventListener listener, String type){
        //remove the listener to the list of listerners
        if(type.equals(GoogleCloudMessaging.MESSAGE_GCM_TYPE))
            if (!GCMEventListeners.isEmpty()){
                GCMEventListeners.remove(listener);
            }           
        else if (type.equals(GoogleCloudMessaging.REG_GCM_TYPE)){
            //for registeration type of message
            if (!GCMRegEventListeners.isEmpty()){
                GCMRegEventListeners.remove(listener);
            }
        } else if (type.equals(GoogleCloudMessaging.SYS_GCM_TYPE)){
            if (!GCMSysEventListeners.isEmpty()){
                GCMSysEventListeners.remove(listener);
            }
        } 
        //Save the action into the sharedPreference
        setSharedPreference(context,"out");
    }
    
    // The SharedPreference is used to store user's preference for opt in/out the message.
    // The GCMIntentService binds with the GoogleCloudMessaging components as it starts. Whenever 
    // the activity got killed, the GCMIntentService will be unbinded. At the background, the 
    // GCMIntenetService will be killed at anytime, no guarantee here. If it got killed, it should
    // be able to recall the last preference to opt in/out. FYI, if the GCMIntentService is not binded
    // with the GoogleCloudMessaging Component, the GCMBroadcastReceiver will instantiate a new instance
    // of the GCMIntentServices whenever the GCMBroadcastReceiver receive an intent from the remote 
    // GCM service.
    private void setSharedPreference(Context context, String pref){
        //save the GCM Message opt in/out preference to sharedPreference for later use
        sharedPreferences = context.getSharedPreferences(GCMConstants.PREFS_GOOGLECLOUDMESSAGING,Context.MODE_PRIVATE);
        Log.i(TAG, "The shared preference is "+sharedPreferences.toString()); 
        
        final SharedPreferences.Editor sharedPrefsEditor =
        sharedPreferences.edit();
        sharedPrefsEditor.putString(GCMConstants.PREFS_GCM_MESSAGE,pref);
        sharedPrefsEditor.commit();
        optInOrOut = pref; 
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
