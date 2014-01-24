// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.R;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;

import android.os.IBinder;

import android.util.Log;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.UsesPermissions;

import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;

import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

import com.google.appinventor.components.runtime.util.TimerManager;
import com.google.appinventor.components.common.PropertyTypeConstants;


/**
 * Timer provides a timer to schedule actions. Everything is represented in
 * milliseconds.
 * 
 */

@DesignerComponent(version = YaVersion.TIMER_COMPONENT_VERSION, description = "Non-visible component that provides a timer to schedule actions", category = ComponentCategory.USERINTERFACE, nonVisible = true, iconName = "images/timer.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.VIBRATE, "
	+ "android.permission.WAKE_LOCK")
public final class Timer extends AndroidNonvisibleComponent implements
		Component, OnDestroyListener, OnPauseListener{
	
	private static final String TAG = "Timer";
	
	private final ComponentContainer container;
	
	/*
	 * Notification
	 */
	private Notification notification;
	private PendingIntent mContentIntent;
	private NotificationManager mNM;
	private static final int TIMER_NOTIFICATION_MIN_ID = 1666;
	private static int timerCounter = 0;
	private final int TIMER_NOTIFICATION_ID; //each timer could only have one unique notification id
	

	// These constants are needed as default values in @DesignerProperty - only
	// used there
	private static final int DEFAULT_INTERVAL = 60; // change this to seconds
	private static final boolean DEFAULT_ENABLED = false;
	//by default, we will wake up the phone to ensure the timer is always triggered
	private static final boolean DEFAULT_WAKE_UP = true; 
	private static final boolean DEFAULT_REPEATING = false;
	private static final int DAILY_INTERVAL = 1*60*60*24; // in seconds
	private static final int WEEKLY_INTERVAL = 1*60*60*24*7; //  

	private int interval;
	private boolean enabled;
	private boolean repeating;
	private boolean wakeup;
	// let the user know when will the clock got trigger
	private long nextTimeTriggered; 
	private int timeleftBeforeTriggered;
	
	// variables for triggering timer at a specific date or time in the future
	private boolean triggerAtSpecificTime = false;
	// use this variable to update first trigger datetime
	private final Calendar trigger_cal;
	// use this to set Timer's trigger time at specific timedate
	
	
	/*
	 * Binding to TimerMananger service
	 */
	private Activity mainUIThreadActivity;
	private TimerManager mBoundTimerManager = null;
	private boolean mIsBound = false;

	private String TimerUID = null; //every Timer should have their own TimerUID

	private String getTimerUID() {
		return TimerUID;
	}

	private static int getNotificationID(){  
	  timerCounter += 1;
    return TIMER_NOTIFICATION_MIN_ID + timerCounter;

	}
	
  private void assignTimerUID() {
    // is called in the constructor of the timer component
    if (TimerUID == null) {
      TimerUID = String.valueOf(System.currentTimeMillis());

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

			mBoundTimerManager = ((TimerManager.LocalBinder) service)
					.getManager();

			Log.i(TAG, "Bound to TimerManager at : " + System.currentTimeMillis());
		}

	public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundTimerManager = null;
			Log.i(TAG, "unbind Service at :" + System.currentTimeMillis());
			

		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		
		Log.v(TAG,
		"Try to bind TimerManager at : " + System.currentTimeMillis());
		mainUIThreadActivity.bindService(new Intent(mainUIThreadActivity,
				TimerManager.class), mConnection, Context.BIND_NOT_FOREGROUND);
		mIsBound = true;
		

	}

	void doUnbindService() {
		if (mIsBound) {

			// TimerEnabled(false);
			// Detach our existing connection.
			mainUIThreadActivity.unbindService(mConnection);
			
			mIsBound = false;
		}
	}
	/*
	 * A getter for internal use by TimerManager
	 */
	public ComponentContainer getContainer(){
		
		return container;
	}

	/**
	 * Creates a new Timer component.
	 * 
	 * @param container
	 *            ignored (because this is a non-visible component)
	 */
	public Timer(ComponentContainer container) {
		super(container.$form());
		
		this.container = container;
		form.registerForOnDestroy(this); //tells me if the form/activity is destroyed
		form.getClass().getName();
			 
		mainUIThreadActivity = container.$context();
		
		Log.i(TAG, "show me mainUIThreadActivity:" + mainUIThreadActivity);

		interval = DEFAULT_INTERVAL;
		repeating = DEFAULT_REPEATING;
		wakeup = DEFAULT_WAKE_UP;
		enabled = DEFAULT_ENABLED;
		assignTimerUID(); // assign an unique identifier for this timer
		TIMER_NOTIFICATION_ID = getNotificationID();
		// initialize timer calendar
		trigger_cal = new GregorianCalendar();
		trigger_cal.setTimeInMillis(System.currentTimeMillis());
		
		// start timerManager at least once
		
		Log.i(TAG, "start timerManager at least once" + mainUIThreadActivity);
		Intent i = new Intent(mainUIThreadActivity, TimerManager.class);
		mainUIThreadActivity.startService(i);

		// bind to the service
		doBindService();
		
		// get Notification Manager
		String ns = Context.NOTIFICATION_SERVICE;
		mNM = (NotificationManager) mainUIThreadActivity.getSystemService(ns);
		Log.i(TAG, "created notification manager");
 
	}
 
	/**
	 * NextTimeTriggered property getter method.
	 * 
	 * @return time stamp of the next trigger (in seconds since Unix time)
	 */
	@SimpleProperty(description = "The next timer triggered time in UTC time (seconds). return 0 when timer is disabled", category = PropertyCategory.BEHAVIOR)
	public long NextTimeTriggered() {
		
		if (!enabled)
			return 0; //just in case if some one calls the getter before enabling the Timer
		
		return nextTimeTriggered/1000;
	}	
	
	/**
	 * NexttimeTriggered property getter (in human-readable) form
	 * @return date formatted in human readable string of the next trigger
	 */
	
	@SimpleFunction(description = "The next triggered time in human readable formatted string. The return" +
								  "string represented the date for next trigger will be in format \"dd-MM-yyyy HH:mm:ss\"." )
	public String NextDateTimeTriggered(){
    String dateFormateInLocalTimeZone = ""; // Will hold the final converted date

    if (this.enabled) {
      Date localDate = null;
      SimpleDateFormat formatter;

      localDate = new Date();
      localDate.setTime(nextTimeTriggered);

      formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
      formatter.setTimeZone(trigger_cal.getTimeZone());

      dateFormateInLocalTimeZone = formatter.format(localDate);
    }else{
      
      dateFormateInLocalTimeZone = "n/a";
    }
    
    return dateFormateInLocalTimeZone;

	}
	
	
	/**
	 * timeleftBeforeTriggered property getter method.
	 * 
	 * @return seconds before next trigger
	 */
	@SimpleProperty(description = "How much time left before next trigger (in seconds)", category = PropertyCategory.BEHAVIOR)
	public int TimeLeftBeforeTriggered() {
		//update timeleftBeforeTriggered every time this method is called
		
		if (this.enabled){
		long currentMillis = System.currentTimeMillis();//this returns milli-seconds after UTC
		 
		timeleftBeforeTriggered = (int) ((nextTimeTriggered - currentMillis)/1000);

		
		}else{
			timeleftBeforeTriggered = -1;
		}
		
		return timeleftBeforeTriggered;
	}	
	
	/**
	 * Interval property getter method.
	 * 
	 * @return timer interval in seconds
	 */
	@SimpleProperty(description = "The timer interval in seconds", category = PropertyCategory.BEHAVIOR)
	public int TimerInterval() {
		return interval;
	}

	/**
	 * Interval property setter method: resets the interval between repeating timer
	 * events.
	 * 
	 * @param interval
	 *            timer interval in second
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "30")
	@SimpleProperty
	public void TimerInterval(int newInterval) {

		
		this.interval = newInterval;
	}
	

	
	/**
	   * Try to kill myself, force the From.onDestroyed() gets called
	   */
	@SimpleFunction
	public void FinishForm(){
		Log.i(TAG, "I am calling this to destroy my form, should see onDestroyed got called later!");
		form.finish();
		
		/* 
		 * http://stackoverflow.com/questions/6117341/why-implement-ondestroy-if-it-is-not-guaranteed-to-be-called
		 * onDestroy() = The final call you receive before your activity is destroyed. This can happen either 
		 * because the activity is finishing (someone called finish() on it, or because 
		 * the system is temporarily destroying this instance of the activity to save space.
		 */
		
	}
	
	/**
	 * Enabled property getter method.
	 * 
	 * @return {@code true} indicates a running timer, {@code false} a stopped
	 *         timer
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean TimerEnabled() {
		return enabled;
	}

	/**
	 * Enabled property setter method: starts or stops the timer.
	 * 
	 * @param enabled
	 *            {@code true} starts the timer, {@code false} stops it
	 */
//	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = DEFAULT_ENABLED ? "True"
//			: "False")
//	@SimpleProperty
  @SimpleFunction (description = "Enable the timer")
	public void TimerEnabled(boolean enabled) {
		if (this.enabled != enabled)
			this.enabled = enabled;

		if (enabled) {
			// do register timer to the main service

			mBoundTimerManager.registerTimer(getTimerUID(), interval, wakeup,
					repeating, Timer.this);
			
	    Log.i(TAG, "register schedule for timer:" + getTimerUID());
	    nextTimeTriggered = System.currentTimeMillis() + interval * 1000;
	      
			if(this.triggerAtSpecificTime){

			  mBoundTimerManager.registerTimer(getTimerUID(), interval, wakeup,
	          repeating, trigger_cal, Timer.this);
			  
			  nextTimeTriggered = trigger_cal.getTimeInMillis();
			}
			
			

			 

		} else {
			// do unregister timer to the main service
			mBoundTimerManager.unregisterTimer(getTimerUID());
			//reset nextTimeTriggered and timerLeftBeforeTriggered
			nextTimeTriggered = 0;
			timeleftBeforeTriggered = -1;
			Log.i(TAG, "unregister schedule for timer:" + getTimerUID() + " at:" + System.currentTimeMillis());
		}

	}

	/**
	 * WakeUp property getter method.
	 * 
	 * return {@code true} if the device is to be waken up from sleeping
	 */
	@SimpleProperty(description = "Wake up the device if asleep")
	public boolean WakeUp() {
		return this.wakeup;
	}

	/**
	 * WakeUp property setter method: instructs when to wake up the device if
	 * sleeping
	 * 
	 * @param yes
	 *            {@code true} if the device should be woken up from sleep
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = DEFAULT_WAKE_UP ? "True"
			: "False")
	@SimpleProperty
	public void WakeUp(boolean newWakeup) {

	  this.wakeup = newWakeup;

	}

	/**
	 * Repeating property getter method.
	 * 
	 * return {@code true} if the Alarm is to be set in Repeating mode
	 */
	@SimpleProperty(description = "Wake up the device if asleep")
	public boolean Repeating() {
		return this.repeating;
	}

	/**
	 * Repeating property setter method: instructs when to set the Alarm in
	 * Repeating mode
	 * 
	 * @param yes
	 *            {@code true} if the Alarm should be set in Repeating mode
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = DEFAULT_REPEATING ? "True"
			: "False")
	@SimpleProperty
	public void Repeating(boolean newRepeating) {
		
		this.repeating = newRepeating;

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		/*
		 * In case the underneath form got killed, need to tell the TimeManager that "oops I got killed"
		 */
		
		mBoundTimerManager.dereferenceFormOfTimer(getTimerUID());
		Log.i(TAG, "the activity is destroyed :" + System.currentTimeMillis());
		doUnbindService();

	}


	public void onTriggered() {
    Log.i(TAG, "Timer got triggered at:" + System.currentTimeMillis());

    if (this.repeating) {
      // we need to reset timeLeftBeforeTriggered and nextTimeTriggered
      long currentMillis = System.currentTimeMillis();
      nextTimeTriggered = currentMillis + interval * 1000;
      timeleftBeforeTriggered = (int) ((nextTimeTriggered - currentMillis) / 1000);

    }

    TimerTriggered();
	}

	/**
	 * Indicates that we receive the timer trigger
	 */
	@SimpleEvent
	public void TimerTriggered() {
	  
    if (enabled) {
      mainUIThreadActivity.runOnUiThread(new Runnable() {
        public void run() {
          Log.i(TAG, "Timer is triggered at:" + System.currentTimeMillis());
          EventDispatcher.dispatchEvent(Timer.this, "TimerTriggered");
        }
      });
      
      if (this.repeating == false) {
        // if the trigger is set previously as not repeating;
        this.enabled = false;
        nextTimeTriggered = 0;
        timeleftBeforeTriggered = -1;
      }
    }
	}
	
	
	
	/*
	 * Add notification with some message and the app (actually it's app.Screen1) it wants to activate
	 * We follow the same mechanism as ActivityStarter
	 * 
	 * @param title 
	 * @param text
	 * @param enabledSound
	 * @param enabledVibrate
	 * @param packageName
	 * @param className
	 * @param extraKey 
	 * @param extraVal 
	 * 
	 */
	
	@SimpleFunction(description = "Create a notication with message to wake up " +
			"another activity when tap on the notification")
	public void CreateNotification(String title, String text, boolean enabledSound, 
			boolean enabledVibrate, String packageName, String className, String extraKey, String extraVal) 
			throws ClassNotFoundException {

		Intent activityToLaunch = new Intent(Intent.ACTION_MAIN);
		
		Log.i(TAG, "packageName: " + packageName);
		Log.i(TAG, "className: " + className);
		
		// for local AI instance, all classes are under the package "appinventor.ai_test"
		// but for those runs on Google AppSpot(AppEngine), the package name will be "appinventor.ai_GoogleAccountUserName" 
		// e.g. pakageName = appinventor.ai_HomerSimpson.HelloPurr 
		// && className = appinventor.ai_HomerSimpson.HelloPurr.Screen1
		
		ComponentName component = new ComponentName(packageName, className);
		activityToLaunch.setComponent(component);
		activityToLaunch.putExtra(extraKey, extraVal);

		
		Log.i(TAG, "we found the class for intent to send into notificaiton");

		activityToLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		mContentIntent = PendingIntent.getActivity(mainUIThreadActivity, 0, activityToLaunch, 0);

		Long currentTimeMillis = System.currentTimeMillis();
		notification = new Notification(R.drawable.stat_notify_chat,
				"Timer Notification!", currentTimeMillis);

		Log.i(TAG, "After creating notification");
		notification.contentIntent = mContentIntent;
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		// reset the notification
		notification.defaults = 0;
		
		if(enabledSound)
			notification.defaults |= Notification.DEFAULT_SOUND;
		
		if(enabledVibrate)
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		
		notification.setLatestEventInfo(mainUIThreadActivity, (CharSequence)title, 
											(CharSequence)text, mContentIntent);
		Log.i(TAG, "after updated notification contents");
		mNM.notify(TIMER_NOTIFICATION_ID, notification);
		Log.i(TAG, "notified");
	
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		//when my form is onPause() && if I enabled any timer in this activity, 
		//then send a notification to the notification bar
		//the intent should knows which one is the 

	}
	

	@SimpleFunction(description = "Set to be triggered at a specific weekday in the future. "
      + "e.g. for Jan 30 2012 23:55, year=2012, month=1, date=30, hourOfDay=23, minute=55. ")
  public void setTimer(int year, int month, int date, int hourOfDay, int minute) {

    this.trigger_cal.set(year, month - 1, date, hourOfDay, minute);// java calendar
                                                          // setting is 0-based
    this.triggerAtSpecificTime = true;
  }

  @SimpleFunction(description = "Set the Timer to be triggered at the specified weekday" +
  		", hour and time, and repeat weekly. e.g Sunday = 1 and Monday =2. ")
  public void SetRepeatedWeeklyTimer(int weekday, int hour, int minute) {
    
    int days;
    //reset the firstTimerTriggerCal to current time
    trigger_cal.setTimeInMillis(System.currentTimeMillis());

    
    //if weekday is after today (e.g. weekday = Fri(6), and today= Wed(4))
    int today = trigger_cal.get(Calendar.DAY_OF_WEEK);
    int todayHour = trigger_cal.get(Calendar.HOUR_OF_DAY);
    int todayMin = trigger_cal.get(Calendar.MINUTE);
    
    if (today < weekday || 
         (today == weekday && todayHour <= hour && todayMin < minute)){
      days = weekday - today;
      
    }else{
      days = weekday + (7 - today);
      
    }
    
    trigger_cal.add(Calendar.DAY_OF_MONTH, days);
    
    trigger_cal.set(Calendar.HOUR_OF_DAY, hour);
    trigger_cal.set(Calendar.MINUTE, minute);
    Log.i(TAG, "the timer will be triggered at:" + trigger_cal.getTime().toString());

    this.triggerAtSpecificTime = true;
    this.repeating = true;
    this.interval = WEEKLY_INTERVAL;

  }
  
  @SimpleFunction(description = "Set the Timer to be triggered at specified " +
  		"hour and minute and repeat daily")
  public void SetRepeatedDailyTimer(int hour, int minute){
    //reset the firstTimerTriggerCal to current time
    trigger_cal.setTimeInMillis(System.currentTimeMillis());
    int hour_now = trigger_cal.get(Calendar.HOUR_OF_DAY);
    int minute_now = trigger_cal.get(Calendar.MINUTE);
    
    
    if (hour < hour_now || 
         (hour == hour_now && minute <= minute_now)){ 
   // if the specified hour is smaller than the current hour; trigger tomorrow
      trigger_cal.add(Calendar.DAY_OF_MONTH, 1);
      
    }
    
    
    trigger_cal.set(Calendar.HOUR_OF_DAY, hour);
    trigger_cal.set(Calendar.MINUTE, minute);
    Log.i(TAG, "the timer will be triggered at:" + trigger_cal.getTime().toString());
    

    this.triggerAtSpecificTime = true;
    this.repeating = true;
    this.interval = DAILY_INTERVAL;
    
    
  }
  

	@SimpleFunction(description = "Set timer's timezone.")
	public void SetTimerTimezone(String timezoneId){
	  trigger_cal.setTimeZone(TimeZone.getTimeZone(timezoneId));

	}
	
  /**
   * Get string representation of a timer's timezone.
   */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public String GetTimerTimezone(){
	  return trigger_cal.getTimeZone().getDisplayName();
	  
	}
	
	@SimpleFunction(description = "Reset the configuration of the timer to default " +
			"interval=30, repeating = false, wakeup = true")
	public void ResetTimer(){
	  
	  this.repeating = DEFAULT_REPEATING;
	  this.wakeup = DEFAULT_WAKE_UP;
	  this.triggerAtSpecificTime = false;

	}
	
		

}