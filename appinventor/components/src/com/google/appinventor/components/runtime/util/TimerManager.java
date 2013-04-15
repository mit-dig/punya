// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.google.appinventor.components.runtime.ActivityResultListener;
import com.google.appinventor.components.runtime.Form;

import edu.mit.media.funf.time.TimeUtil;
import com.google.appinventor.components.runtime.Timer;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TimerManager extends Service{

	public static final String ACTION_KEEP_ALIVE = "timer.keepalive";
	public static final String ACTION_SCHEDULE = "timer.schedule";
	
	private Map<String, TimerAndContext> timers;
	private Map<String, String> timerContextNames;
	private Scheduler scheduler;
	private boolean activityReLaunched = false;
	
	
	private static final String TAG = "TimerManager";
	
	private static final int ACTIVITY_RECREATE_NOTIFICATION = 1;
	
	@Override
	public void onCreate() {
		
		this.scheduler = new Scheduler();
		this.timers = new HashMap<String, TimerAndContext>();
		this.timerContextNames = new HashMap<String, String>();
		Log.i(TAG, "I got created at: " + System.currentTimeMillis());
			
		
		
	}
	

	@Override
	public void onDestroy(){
	  
		Log.i(TAG, "too bad I also got killed at: " + System.currentTimeMillis());
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "receive intent:" + intent.toString());

		String action = intent.getAction();

		if (action == null || ACTION_KEEP_ALIVE.equals(action)) {
			// Does nothing, but wakes up FunfManager
			Log.i(TAG, "keep-alive TimerManager");
		} else if (ACTION_SCHEDULE.equals(action)) {
			
			Uri componentUri = intent.getData();
			String timerName = getPipelineName(componentUri);

			TimerAndContext tnc = timers.get(timerName);
			Log.i(TAG, "What is the context: " + tnc.getContext());
			Log.i(TAG, "Reference to Timer: " + tnc.getTimer());
			if (tnc.getContext() == null) { 
				
				// latest update(2012/11/24); we current disabled all handling the case if the activity that host the timer component is killed.
				//in this case, the activity which the timer belongs to is destroyed
				// we have several options here
				// 1. restart Activity. However, we leave the job to persist Timer's configuration and restart Timer to the user
				//   * the user should take care persisting the Timer configuration in the screen1.initialized. 
				//   ** By pulling stuff from TinyDB and enable timer in screen1.initialized()  again!
				//
				// 2. or we could use startActivity with special intent and let the newly created Form pass that BIRTH_CODE to Timer at Timer.onCreate()
				//    In the onCreate(), The Timer could take a look at the intent and call TimerManager to copy the configuration of the OLD_TIMER_UID 
				//    to the new_timer_uid
				//    but what happen if there are more than 1 Timer in it? --> doesn't make sense to even keep states of 
				//    the old timers when creating a new Activity, and recreate these two timers. Because in AI, once the Form is created, 
				//    all Timer components that're used in Activity will get recreated . The TimerManager don't know which one maps to which?
				
				//  In conclusion, AI user needs to keep the states of the timers somewhere and pull them back during screen.initialized()
				
				// latest updated by Fuming (2012/12/04)
			  // We will recreate the Activity using ClassName lookup, and leave the state persistence to the user of AI (they need to save the 
			  // persistent variables to tiny DB.
				try {
				  Log.i(TAG, "Oops, the activity was killed. Create a new one:" + timerName);
          Class<?> activityToWakup = Class.forName(timerContextNames.get(timerName));
          Intent startActivity = new Intent(getBaseContext(), activityToWakup);
          startActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          // before we restart the activity, we will clean up all timers' alarms in Android
          // this means that if there are three timers in this Activity, when the activity is destroyed, 
          // although the timers have dereferenced themselves from TimerManager, but we have to wait until 
          // next round for the first timer to be triggered by Android AlarmManager and then un-register
          // all timers from Android AlarmManager
          
          cleanOldTimers(); 
          // look up HashMap timers to find all <timerID, TimerAndContext> pairs that have 
          
          this.startActivity(startActivity);

          
        } catch (ClassNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } 
			  
			}

			else{
				//normal case....
				Timer timer = tnc.getTimer();
				Log.i(TAG, "Getting pipeline about to run: " + timer);
				String activityName = timerContextNames.get(timerName);
				Log.i(TAG, "Class name? " + activityName);//debug
				timer.onTriggered();
			}

		}

		return Service.START_FLAG_RETRY;
	}
	
	private void cleanOldTimers(){
	  // iterate through timers and find entry set that has its 
	  // value (TimerAndContext<Timer, Context>) as <null, null>
	  ArrayList<String> toRemove = new ArrayList<String>();
	  
	  for (Map.Entry<String, TimerAndContext> entry : timers.entrySet()){
	    String timerName = entry.getKey();
	    TimerAndContext tnc = entry.getValue();
	    
	    if(tnc.getTimer() == null && tnc.getContext() == null)
	      toRemove.add(timerName);

	  }
	  
	  Log.i(TAG, "Size of toRemove:" + toRemove.size());
	  
	  // 1. remove all Android alarm related to these old timers
	  // 2. remove from timers hashmap
	  for (String timerName : toRemove){
	    this.scheduler.cancel(timerName);
	    timers.remove(timerName);
	  }
	  
	  
	}
	
	public class LocalBinder extends Binder {
		public TimerManager getManager() {
			return TimerManager.this;
		}
	}
	

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return new LocalBinder();
	}

 
	public void registerTimer(String timerName, int newInterval, boolean wakeup,
			boolean repeating, Timer timer) {
		// TODO Auto-generated method stub
		synchronized (timers) {
			//although it's we will have two same kind of timer instances
			Log.i(TAG, "timer: " + timer.toString());
			Log.i(TAG, "timer: " + timer.getContainer().$context());
			
			TimerAndContext tnc = new TimerAndContext(timer, timer.getContainer().$context());
			timers.put(timerName, tnc);
			timerContextNames.put(timerName, timer.getContainer().$context().getClass().getName());
			
		}
		Log.i(TAG, "Before setting the schedule");
		//set schedule
		scheduler.set(timerName, newInterval, wakeup, repeating);
	}
	
	
	public void registerTimer(String timerName, int newInterval, boolean wakeup, 
	    boolean repeating, Calendar triggerDateTime, Timer timer){
    // TODO Auto-generated method stub
    synchronized (timers) {
      //although it's we will have two same kind of timer instances
      Log.i(TAG, "timer: " + timer.toString());
      Log.i(TAG, "timer: " + timer.getContainer().$context());
      
      TimerAndContext tnc = new TimerAndContext(timer, timer.getContainer().$context());
      timers.put(timerName, tnc);
      timerContextNames.put(timerName, timer.getContainer().$context().getClass().getName());
      
    }
    Log.i(TAG, "Before setting the schedule at specific date");
    //set schedule
    
    scheduler.set(timerName, newInterval, wakeup, repeating, triggerDateTime);
	  
	  
	}
	
	public static final String 
	AI_SCHEME = "AppInventor";

	public void unregisterTimer(String name) {
		// TODO Auto-generated method stub
		scheduler.cancel(name);
	}
	
	public static Uri getPipelineUri(String pipename) {
		Uri uri = new Uri.Builder()
		.scheme(AI_SCHEME)
		.path(pipename) // Automatically prepends slash
		.build();
		
		Log.i(TAG, "PipelineUri: " +uri.toString());
		return uri;
	}
	
	public static String getPipelineName(Uri pipelineUri) {
		
		
		String pipelineName = pipelineUri.getPath().substring(1); // Remove automatically prepended slash from beginning
		Log.i(TAG, "Get PipelineName from URI: "+ pipelineName);
		return pipelineName; 
	}
	
	/*
	 * This class will store the TimerPipeline (timer component) and the context (activity)
	 */
	public class TimerAndContext{
		private Timer timer;
		private Activity activity; 
		
		public TimerAndContext(Timer timer, Activity activity){
			 this.timer = timer;
			 this.activity = activity;

		}
		
		public Timer getTimer(){
			return timer;
		}
		public Context getContext(){
			
			return activity;
		}
	}
	
	public class Scheduler {
		private AlarmManager alarmManager;
		private Context context;

		public Scheduler() {
			this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			this.context = TimerManager.this;
		}


    public void cancel(String pipelineName) {

		
			Intent intent = new Intent();
			intent.setClass(context, TimerManager.class);
			intent.setData(getPipelineUri(pipelineName));
			intent.setAction(ACTION_SCHEDULE);


			PendingIntent operation = PendingIntent.getService(context, 0,
					intent, PendingIntent.FLAG_NO_CREATE);
			if (operation != null) {
				operation.cancel();
				Log.i(TAG, "after cancel timer for:" + pipelineName + " at:" + System.currentTimeMillis());
			}
		}

		public void set(String pipelineName, int interval, boolean wakeup,
				boolean repeating) {
			Log.i(TAG, "set timer for:" + pipelineName);
			// Creates pending intents that will call back into TimerManager
			// Uses alarm manager to time them
			Log.i(TAG, "set new interval:" + interval);
			Log.i(TAG, "set new wakeup:" + wakeup);
			Log.i(TAG, "set new repeating:" + repeating);
			
			Intent intent = new Intent();
			intent.setClass(context, TimerManager.class);
			intent.setData(getPipelineUri(pipelineName));
			intent.setAction(ACTION_SCHEDULE);

			PendingIntent operation = PendingIntent.getService(context, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT);
			long currentTimeMillis = System.currentTimeMillis();
			 
			long intervalMillis = interval * 1000;
			long startTimeMillis = intervalMillis + currentTimeMillis;

			if (repeating) {
				Log.i(TAG, "set repeating timer for: " + pipelineName);
				alarmManager.setRepeating(wakeup ? AlarmManager.RTC_WAKEUP
						: AlarmManager.RTC, startTimeMillis, intervalMillis,
						operation);
			} else {
				Log.i(TAG, "set one-time timer: " + pipelineName);
				alarmManager.set(wakeup ? AlarmManager.RTC_WAKEUP
						: AlarmManager.RTC, startTimeMillis, operation);
			}
		}
		
    public void set(String pipelineName, int interval, boolean wakeup,
        boolean repeating, Calendar triggerDateTime) {
      Log.i(TAG, "in setting alarm schedule with specific datetime");
      Log.i(TAG, "set timer for:" + pipelineName);
      Log.i(TAG, "set new interval:" + interval);
      Log.i(TAG, "set new wakeup:" + wakeup);
      Log.i(TAG, "set new repeating:" + repeating);
      Log.i(TAG, "set first triggerDatetime:" + triggerDateTime.getTime().toString());
      
      Intent intent = new Intent();
      intent.setClass(context, TimerManager.class);
      intent.setData(getPipelineUri(pipelineName));
      intent.setAction(ACTION_SCHEDULE);

      PendingIntent operation = PendingIntent.getService(context, 0,
          intent, PendingIntent.FLAG_UPDATE_CURRENT);

      long intervalMillis = interval * 1000;
      long startTimeMillis = triggerDateTime.getTimeInMillis();

      if (repeating) {
        Log.i(TAG, "set repeating timer for: " + pipelineName);
        alarmManager.setRepeating(wakeup ? AlarmManager.RTC_WAKEUP
            : AlarmManager.RTC, startTimeMillis, intervalMillis,
            operation);
      } else {
        Log.i(TAG, "set one-time timer: " + pipelineName);
        alarmManager.set(wakeup ? AlarmManager.RTC_WAKEUP
            : AlarmManager.RTC, startTimeMillis, operation);
      }
      
      
    }

	}


	public void dereferenceFormOfTimer(String timerUID) {
		// TODO Auto-generated method stub
		// clean the reference of timerPipeline and context to null
		Log.i(TAG, "set context and timer to null:");
		timers.put(timerUID, new TimerAndContext(null, null));
		// should I just clean all items in the timers? ok for now. If the activity has two timers, both will set to null, null in the timers_map 
		
		activityReLaunched = false;
		
	}
 
	

 
}
