package com.google.appinventor.components.runtime;

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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

@DesignerComponent(version = YaVersion.GOOGLECLOUDMESSAGING_COMPONENT_VERSION, 
description = "", category = ComponentCategory.FUNF, nonVisible = true, iconName = "images/info.png")
@UsesPermissions(permissionNames = "com.google.android.c2dm.permission.RECEIVE, "
        + "android.permission.INTERNET, android.permission.GET_ACCOUNTS, "
        + "android.permission.WAKE_LOCK")
public final class GoogleCloudMessaging extends AndroidNonvisibleComponent
implements Component,OnDestroyListener{

    protected boolean mIsBound = false;
    protected GCMIntentService mBoundGCMIntentService = null;
    private final String TAG = "GoogleCloudMessaging";
    public static final String INIT_INTENTSERVICE_ACTION = "bind_init"; //do nothing

    // gcmLock synchronizes uses of any/all gcm objects
    // in this class. As far as I can tell, these objects are not thread-safe
    private final Object gcmLock = new Object();

    // the following fields should only be accessed from the UI thread
    private volatile String SERVER_URL = "";
    private volatile String SENDER_ID = "";

    protected boolean enabled = false; // run once
    protected boolean enabledSchedule = false; // run periodically

    protected Activity mainUIThreadActivity;
    
    private String gcmMessage = "";
    
    public GoogleCloudMessaging(ComponentContainer container) {
        super(container.$form());
        
        // Set up listeners
        mainUIThreadActivity = container.$context();

        // start GCMIntentService
        Intent i = new Intent(mainUIThreadActivity, GCMIntentService.class);
        i.setAction(INIT_INTENTSERVICE_ACTION);
        GCMBaseIntentService.runIntentInService(mainUIThreadActivity, i, 
                GCMBroadcastReceiver.getDefaultIntentServiceClassName(mainUIThreadActivity));
        doBindService();
    }
    
    @Override
    public void onDestroy() {
      // remember to unbind
      doUnbindService();
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

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    public void Enabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            Register();
        } else {
            UnRegister();
        }
    }

    /**
     * Authenticate to Google Cloud Messaging
     */
    @SimpleFunction(description = "Removes the GCM authorization from this running app instance")
    public void Register() {
        Log.i(TAG, "Start the registration process");
        Log.i(TAG, "The sender id is " + SENDER_ID);
        Log.i(TAG, "The server URL is " + SERVER_URL);
        AsynchUtil.runAsynchronously(new Runnable() {
            public void run() {
                final String regId = GCMRegistrar.getRegistrationId(form);

                if (regId.equals("")) {
                    Log.i(TAG, "The divice is NOT registered on the server.");
                    GCMRegistrar.register(form, SENDER_ID);
                    Log.i(TAG, "After the registration process.");
                } else {
                    Log.i(TAG, "The registration id is not empty.");
                    if (GCMRegistrar.isRegisteredOnServer(form)) {
                        Log.i(TAG, "It is registered on the server.");
                        GCMInfoReceived();
                        return;
                    }
                }
            }
        });
    }

    /**
     * Remove authentication for this app instance
     */
    @SimpleFunction(description = "Removes the GCM authorization from this running app instance")
    public void UnRegister() {
        synchronized (gcmLock) {
            GCMRegistrar.unregister(form);
        }
    }

    /**
     * Indicates that the GCM info has been received.
     */
    @SimpleEvent
    public void GCMInfoReceived() {
        Log.i(TAG, "Waiting to receive info from the server.");
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
            Log.i(TAG, "Bound to GCMIntentService");
            registerGCMEvent();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundGCMIntentService = null;
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
        mIsBound = true;
        Log.i(TAG,
                "GCMIntentService is bound, and now we could have register dataRequests");
    }

    void doUnbindService() {
        if (mIsBound) {
            Log.i(TAG,"unbinding the attached service");
            // Detach our existing connection.
            mainUIThreadActivity.unbindService(mConnection);
            mIsBound = false;
        }
    }
    
    final Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            gcmMessage = msg.obj.toString();
            Log.i(TAG, " before call GCMInfoReceived()");
            GCMInfoReceived();
            Log.i(TAG, " after call GCMInfoReceived()");
        }
    };

    GCMEventListener listener = new GCMEventListener() {
        @Override
        public void onMessageReceived(String msg) {
          // through the event handler
          Log.i(TAG, "Received one message from the listener");
          Message message = myHandler.obtainMessage();
          if (msg ==null){
              msg = "This is a dummy message.";
          }
          message.obj = msg;
          myHandler.sendMessage(message);
        }
    };

    public void registerGCMEvent() {
        this.mBoundGCMIntentService.requestGCMMessage(listener);
    }   
}