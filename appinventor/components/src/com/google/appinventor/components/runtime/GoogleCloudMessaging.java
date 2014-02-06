// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import java.util.concurrent.locks.Lock;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import android.R;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/*
 * GCM registration process
 * 
 * 1. An android device sends a request to your server (you need to set it up or
 * public server which runs the GCM client server code).
 * 
 * 2. Your client server sends request to the GCM Server to the register the device 
 * upon the request from your server. 
 * 
 * 3. The GCM server returns the registration id to your server, and your server returns 
 * the registration id back to your android device.
 * 
 * 4. If the android device wants to unregister itself from the list, the android device 
 * need to send the unregister request directly to the GCM Server.
 * 
 * GCM message flow 
 * 
 * 1. Your server need to provide an interface for user to send messages to the registered 
 * android devices.
 * 
 * 2. Your server will forward the send message request to the GCM Server. The GCM server 
 * will process the request and will push the message to the registered android devices
 * 
 * Google Cloud Message Java file implementation 
 * 
 * 1. The components itself extends the AndroidNonvisibleComponent, and implements 
 * the Component and the OnDestroyListener. 
 * 
 * 2. It has the mainUIThreadActivity. The mainUIThreadActivity binds the 
 * GCMIntentService. 
 * 
 * 3. By default, the Google Cloud Message Server intent calls the GCMBroadcaseReceiver.
 * The GCMBroadcaseReceiver will start the GCMIntentService. 
 * 
 * 4. The GCMIntentService calls the GCMEventListener. The GCMEventListener passes back 
 * the message to the mainUIThreadActivity
 * 
 */

/* 
 * @author fuming@mit.mit (Fuming Shih)
 * @author wli17@mit.edu (Weihua Li)
 */
@DesignerComponent(version = YaVersion.GOOGLECLOUDMESSAGING_COMPONENT_VERSION, 
    description = "<p>A non-visible component that enables push notification  " +
    "by Google Cloud Messaging <a href=\"http://developer.android.com/" +
    "google/gcm/index.html\" target=\"_blank\">GCM</a>. " +   
    "<p>You must obtain the sender ID and the GCM server URL " +
    " specific to your app. </p> ",
    category = ComponentCategory.SOCIAL,
    nonVisible = true, 
    iconName = "images/googleCloudMessaging.png")
@UsesPermissions(permissionNames = "com.google.android.c2dm.permission.RECEIVE, "
        + "android.permission.INTERNET, "
        +"android.permission.GET_ACCOUNTS, "
        +"android.permission.WAKE_LOCK, "
        +"android.permission.VIBRATE"
        )
public final class GoogleCloudMessaging extends AndroidNonvisibleComponent
        implements Component, OnDestroyListener {

    // notification
    private Notification notification;
    private PendingIntent mContentIntent;
    private NotificationManager mNM;
    private final int PROBE_NOTIFICATION_ID = 1;

    protected boolean mIsBound = false;
    protected GCMIntentService mBoundGCMIntentService = null;
    private final String TAG = "GoogleCloudMessaging";
    public static final String INIT_INTENTSERVICE_ACTION = "bind_init"; 
    
    protected static final String REG_GCM_TYPE = "reg";
    protected static final String MESSAGE_GCM_TYPE = "message";
    protected static final String SYS_GCM_TYPE = "system";
    private static final String REG_SUCCESSED_MSG = "Registered successfully with GCM 3rd party server!";

    // the following fields should only be accessed from the UI thread
    private volatile String SERVER_URL = "";
    private volatile String SENDER_ID = "";

    protected boolean enabled = false; // run once
    protected boolean enabledSchedule = false; // run periodically

    protected Activity mainUIThreadActivity;
    protected Context context;

    private String gcmMessage = "";
    
    private static final String REG_ID_TAG = "RegistrationId";
    private final SharedPreferences sharedPreferences;

    // private final SharedPreferences sharedPreferences;

    public GoogleCloudMessaging(ComponentContainer container) {
        super(container.$form());
        form.registerForOnDestroy(this);
        
        // Set up listeners
        context = container.$context();
        mainUIThreadActivity = container.$context();
        sharedPreferences = container.$context().getSharedPreferences(GCMConstants.PREFS_GOOGLECLOUDMESSAGING,Context.MODE_PRIVATE);
        
        // get Notification Manager
        String ns = Context.NOTIFICATION_SERVICE;
        mNM = (NotificationManager) mainUIThreadActivity.getSystemService(ns);
        
        // start GCMIntentService
        Intent i = new Intent(mainUIThreadActivity, GCMIntentService.class);
        i.setAction(INIT_INTENTSERVICE_ACTION);
        GCMBaseIntentService.runIntentInService(mainUIThreadActivity,i,
                GCMBroadcastReceiver.getDefaultIntentServiceClassName(mainUIThreadActivity));
        doBindService();
        
        //Set the initial value for the GCM message
        //This is for the relaunching the activity that has been waked up by the GCMIntentService
        //launch service
        gcmMessage = container.$form().getGCMStartValues();
        Log.i(TAG,"The initial value of the gcmMessage is "+gcmMessage);
    }
    
    // check to see if there is an exisiting preference for the GCM; if there is, enables the listeners.
    private void checkAndSetPreference() {
        Log.i(TAG, "Checking the preference now, either in or out");
        //check for if there is an exisiting preference for the GCM; if there is, enables the listeners.
        if(sharedPreferences.getString(GCMConstants.PREFS_GCM_MESSAGE, "").equals("in")) {
            Log.i(TAG,"Enabled the listeners after failure.");
            Enabled(true);
        }
    }
    

    private String retrieveRegId() {
        String regId = GCMRegistrar.getRegistrationId(form);  
        return regId;
      }
    

    @Override
    public void onDestroy() {
        // remember to unbind
        Log.i(TAG, "My GoogleCloudMessaging.java got destroyed");
        if (mIsBound && mConnection != null) {
          Log.i(TAG, "In the onDestroy method, before the doUnbindService method.");
          doUnbindService();
          Log.i(TAG, "In the onDestroy method, after the doUnbindService method.");
        }
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String RegId() {
        return retrieveRegId();
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String ServerURL() {
        return SERVER_URL;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    public void ServerURL(String SERVER_URL) {
        this.SERVER_URL = SERVER_URL;
    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String SenderID() {
        return SENDER_ID;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    public void SenderID(String SENDER_ID) {
        this.SENDER_ID = SENDER_ID;
    }
    

    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String ReturnMessage() {
        return gcmMessage;
    }

    // Add / remove the listeners
    // @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    // @SimpleProperty
    @SimpleFunction(description = "Enable Google Cloud Messaging to receive push notification")
    public void Enabled(boolean enable) {

        enabled = enable;
        if (enabled) {
            Log.i(TAG, "Before registerGCMEvent - regListener");
            registerGCMEvent(context, regListener, REG_GCM_TYPE);
            Log.i(TAG, "Before registerGCMEvent - msgListener");
            registerGCMEvent(context, msgListener, MESSAGE_GCM_TYPE);          
        } else {
            unRegisterGCMEvent(context, regListener, REG_GCM_TYPE);
            unRegisterGCMEvent(context, msgListener, MESSAGE_GCM_TYPE);
        }
    }
    
    @SimpleFunction()
    public boolean isRegistered(){
        return GCMRegistrar.isRegisteredOnServer(form);
    }
    
    @SimpleFunction()
    public boolean isServiceReady(){
        return mIsBound;
    }
    
    /**
     * Authenticate to Google Cloud Messaging
     */
    @SimpleFunction(description = "Add the GCM authorization to this running app instance")
    public void Register() {
        Log.i(TAG, "Start the registration process");
        Log.i(TAG, "The sender id is " + SENDER_ID);
        Log.i(TAG, "The server URL is " + SERVER_URL);

        Enabled(true);
        mBoundGCMIntentService.setSenderID(SENDER_ID);
        mBoundGCMIntentService.setServerURL(SERVER_URL);

        AsynchUtil.runAsynchronously(new Runnable() {
            public void run() {
                try {
                    final String regId = GCMRegistrar.getRegistrationId(form);                    
                    if (regId.equals("")) {
                        // Automatically registers application
                        GCMRegistrar.register(form, SENDER_ID);
                    } else {
                        // Device is already registered on GCM, check server.
                        if (GCMRegistrar.isRegisteredOnServer(form)) {
                            // Skips registration.
                        } else {
                            // Try to register again, but not in the UI thread.
                            // It's also necessary to cancel the thread onDestroy(),
                            // hence the use of AsyncTask instead of a raw thread.

                            if (!GCMServerUtilities.register(form, regId,SERVER_URL)) {
                                // At this point all attempts to register with the app
                                // server failed, so we need to unregister the device
                                // from GCM - the app will try to register again when
                                // it is restarted. Note that GCM will send an
                                // unregistered callback upon completion, but
                                // GCMIntentService.onUnregistered() will ignore it.
                                GCMRegistrar.unregister(form);
//                                form.dispatchErrorOccurredEvent(GoogleCloudMessaging.this, "Register",
//                                        500, "Please check the server/connection.");
                                //TODO: need to add a error message..
                                Log.i("TAG", "some one dies....and cause us to die...");
                            }
                        }
                    }
                    return;
                } catch (Exception e) {
                    Log.i(TAG, "within the exception of Register");
                    UnRegister();
                }
            }
        });
    }
    
    /**
     * Remove authentication for this app instance
     */
    @SimpleFunction(description = "Removes the GCM authorization from this running app instance")
    public void UnRegister() { 
        
        AsynchUtil.runAsynchronously(new Runnable() {
            public void run() {
                final String regId = GCMRegistrar.getRegistrationId(form);     
                try {
                    Enabled(false);
                    Log.i(TAG, "after remove the Regid: "+regId);
                    GCMServerUtilities.unregister(form, regId, SERVER_URL);
                    Log.i(TAG, "after unregister from the GCMServerUtilities");
                    GCMRegistrar.unregister(form);
                    Log.i(TAG, "after unregister from the GCMRegistrar");
                    return;
                } catch (Exception e) {
                    Log.i(TAG, "within the exception of UnRegister");
                }
            }
        });
    }

    /**
     * Indicates that the GCM info has been received.
     */
    @SimpleEvent
    public void GCMInfoReceived() {
        Log.i(TAG, "Waiting to receive info from the server and the enabled value is "+enabled);
        if (enabled) {
            mainUIThreadActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Log.i(TAG, "GCMInfoReceived() is called");
                    EventDispatcher.dispatchEvent(GoogleCloudMessaging.this,
                            "GCMInfoReceived");
                }
            });
        }
    }
    
    /**
     * Indicates when the server registration has been successful.
     */
    @SimpleEvent()
    public void RegInfoReceived() {
        Log.i(TAG, "Waiting to receive GCM Registration info from the GCM Service.");
        if (enabled) {
            mainUIThreadActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Log.i(TAG, "RegInfoReceived() is called");
                    EventDispatcher.dispatchEvent(GoogleCloudMessaging.this,
                            "RegInfoReceived");
                }
            });
        }
    }

    // try local binding to FunfManager
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundGCMIntentService = ((GCMIntentService.LocalBinder) service)
                    .getService();
            mIsBound = true;
            Log.i(TAG, "Bound to GCMIntentService");  
            checkAndSetPreference();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundGCMIntentService = null;
            mIsBound = false;
            Log.i(TAG, "Unbind GCMIntentService");
        }
    };

    void doBindService() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        mainUIThreadActivity.bindService(new Intent(mainUIThreadActivity,
                GCMIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG,
                "GCMIntentService is bound, and now we could have register dataRequests");
    }

    void doUnbindService() {
        if (mIsBound) {
            Log.i(TAG, "unbinding the attached service");
            // Detach our existing connection.
            mainUIThreadActivity.unbindService(mConnection);
            mIsBound = false;
        }
    }

    //The GCMIntentService dispatches the intent, and the GoogleCloudMessaging
    //component receive the intent using the listeners.
    //There are two types of messages: 1)Registration 2)Regular GCM Message   
    final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            gcmMessage = msg.obj.toString();
            Log.i(TAG,"The GCM Message is "+gcmMessage);
            GCMInfoReceived();
        }
    };

    GCMEventListener msgListener = new GCMEventListener() {
        @Override
        public void onMessageReceived(String msg) {
            // through the event handler
            Log.i(TAG, "Received one message from the msg listener");
            Message message = msgHandler.obtainMessage();
            if (msg == null) {
                msg = "This is a dummy message.";
            }
            message.obj = msg;
            msgHandler.sendMessage(message);
        }
    };
    

    final Handler regHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            gcmMessage = msg.obj.toString();
            RegInfoReceived();
        }
    };
    
    GCMEventListener regListener = new GCMEventListener() {
        @Override
        public void onMessageReceived(String msg) {
            // through the event handler
            Log.i(TAG, "Received one message from the reg listener");
            Message message = regHandler.obtainMessage();
            if (msg == null) {
                msg = "This is a dummy message.";
            }
            message.obj = msg;
            regHandler.sendMessage(message);
        }
    };
    
    //Add the listener to the GCMIntentService
    public void registerGCMEvent(Context context, GCMEventListener listener, String eventType) {
        Log.i(TAG, "Before the registerGCMEvent");
        mBoundGCMIntentService.requestGCMMessage(context, listener, eventType);
    }
    
    //Remove the listener to the GCMIntentService
    public void unRegisterGCMEvent(Context context,GCMEventListener listener, String eventType) {
        mBoundGCMIntentService.unRequestGCMMessage(context, listener, eventType);
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

        mContentIntent = PendingIntent.getActivity(mainUIThreadActivity, 0,
                activityToLaunch, 0);

        Long currentTimeMillis = System.currentTimeMillis();
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

        notification.setLatestEventInfo(mainUIThreadActivity,
                (CharSequence) title, (CharSequence) text, mContentIntent);
        Log.i(TAG, "after updated notification contents");
        mNM.notify(PROBE_NOTIFICATION_ID, notification);
        Log.i(TAG, "notified");
    }
}