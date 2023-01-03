---
layout: documentation
title: Sensors
---

[&laquo; Back to index](index.html)
# Sensors

Table of Contents:

* [AccelerometerSensor](#AccelerometerSensor)
* [ActivityProbeSensor](#ActivityProbeSensor)
* [BarcodeScanner](#BarcodeScanner)
* [Barometer](#Barometer)
* [BatterySensor](#BatterySensor)
* [CallLogHistory](#CallLogHistory)
* [CellTowerProbeSensor](#CellTowerProbeSensor)
* [Clock](#Clock)
* [ContactInfo](#ContactInfo)
* [GyroscopeSensor](#GyroscopeSensor)
* [Hygrometer](#Hygrometer)
* [LightSensor](#LightSensor)
* [LocationProbeSensor](#LocationProbeSensor)
* [LocationSensor](#LocationSensor)
* [MagneticFieldSensor](#MagneticFieldSensor)
* [NearField](#NearField)
* [OrientationSensor](#OrientationSensor)
* [Pedometer](#Pedometer)
* [PedometerSensor](#PedometerSensor)
* [ProximitySensor](#ProximitySensor)
* [RunningApplications](#RunningApplications)
* [ScreenStatus](#ScreenStatus)
* [SmsHistory](#SmsHistory)
* [SocialProximitySensor](#SocialProximitySensor)
* [TelephonyInfo](#TelephonyInfo)
* [Thermometer](#Thermometer)
* [WifiSensor](#WifiSensor)

## AccelerometerSensor  {#AccelerometerSensor}

Non-visible component that can detect shaking and measure acceleration approximately in three
 dimensions using SI units (m/s<sup>2</sup>). The components are:

 - **xAccel**: 0 when the phone is at rest on a flat surface, positive when the phone is tilted
   to the right (i.e., its left side is raised), and negative when the phone is tilted to the
   left (i.e., its right size is raised).
 - **yAccel**: 0 when the phone is at rest on a flat surface, positive when its bottom is raised,
   and negative when its top is raised.
 - **zAccel**: Equal to -9.8 (earth's gravity in meters per second per second when the device is
   at rest parallel to the ground with the display facing up, 0 when perpendicular to the ground,
   and +9.8 when facing down. The value can also be affected by accelerating it with or against
   gravity.



### Properties  {#AccelerometerSensor-Properties}

{:.properties}

{:id="AccelerometerSensor.Available" .boolean .ro .bo} *Available*
: Returns whether the `AccelerometerSensor` hardware is available on the device.

{:id="AccelerometerSensor.Enabled" .boolean} *Enabled*
: Specifies whether the sensor should generate events.  If `true`{:.logic.block},
 the sensor will generate events.  Otherwise, no events are
 generated even if the device is accelerated or shaken.

{:id="AccelerometerSensor.LegacyMode" .boolean .wo .do} *LegacyMode*
: Prior to the release that added this property the AccelerometerSensor component passed through sensor values directly as received from the Android system. However these values do not compensate for tablets that default to Landscape mode, requiring the MIT App Inventor programmer to compensate. However compensating would result in incorrect results in Portrait mode devices such as phones. We now detect Landscape mode tablets and perform the compensation. However if your project is already compensating for the change, you will now get incorrect results. Although our preferred solution is for you to update your project, you can also just set this property to “true” and our compensation code will be deactivated. Note: We recommend that you update your project as we may remove this property in a future release.

{:id="AccelerometerSensor.MinimumInterval" .number} *MinimumInterval*
: Specifies the minimum interval required between back-to-back [`Shaking`](#AccelerometerSensor.Shaking) events,
 in milliseconds.
 Once the phone starts being shaken, all further [`Shaking`](#AccelerometerSensor.Shaking) events will be ignored
 until the interval has elapsed.

{:id="AccelerometerSensor.Sensitivity" .number} *Sensitivity*
: Specifies the sensitivity of the accelerometer. Valid values are: `1` (weak), `2` (moderate),
 and `3` (strong).

{:id="AccelerometerSensor.XAccel" .number .ro .bo} *XAccel*
: Returns the acceleration in the X-dimension in SI units (m/s²).
 The sensor must be enabled to return meaningful values.

{:id="AccelerometerSensor.YAccel" .number .ro .bo} *YAccel*
: Returns the acceleration in the Y-dimension in SI units (m/s²).
 The sensor must be enabled to return meaningful values.

{:id="AccelerometerSensor.ZAccel" .number .ro .bo} *ZAccel*
: Returns the acceleration in the Z-dimension in SI units (m/s²).
 The sensor must be enabled to return meaningful values.

### Events  {#AccelerometerSensor-Events}

{:.events}

{:id="AccelerometerSensor.AccelerationChanged"} AccelerationChanged(*xAccel*{:.number},*yAccel*{:.number},*zAccel*{:.number})
: Indicates the acceleration changed in the X, Y, and/or Z dimensions.

{:id="AccelerometerSensor.Shaking"} Shaking()
: Indicates the device started being shaken or continues to be shaken.

### Methods  {#AccelerometerSensor-Methods}

{:.methods}
None


## ActivityProbeSensor  {#ActivityProbeSensor}

Component for ActivityProbeSensor



### Properties  {#ActivityProbeSensor-Properties}

{:.properties}

{:id="ActivityProbeSensor.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="ActivityProbeSensor.DefaultInterval" .number .ro .bo} *DefaultInterval*
: Returns the timestamp of latest reading

{:id="ActivityProbeSensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="ActivityProbeSensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="ActivityProbeSensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="ActivityProbeSensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="ActivityProbeSensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#ActivityProbeSensor-Events}

{:.events}

{:id="ActivityProbeSensor.ActivityInfoReceived"} ActivityInfoReceived(*timestamp*{:.number},*activityLevel*{:.text})
: Indicates that the activity info has been received.

{:id="ActivityProbeSensor.ActivityScanComplete"} ActivityScanComplete()
: Indicates that one round of scan has finish

### Methods  {#ActivityProbeSensor-Methods}

{:.methods}

{:id="ActivityProbeSensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="ActivityProbeSensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should "run once" to listen for activity level information

{:id="ActivityProbeSensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="ActivityProbeSensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="ActivityProbeSensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## BarcodeScanner  {#BarcodeScanner}

Component for scanning a QR code and getting back the resulting string.



### Properties  {#BarcodeScanner-Properties}

{:.properties}

{:id="BarcodeScanner.Result" .text .ro .bo} *Result*
: Gets the text result of the previous scan.

{:id="BarcodeScanner.UseExternalScanner" .boolean} *UseExternalScanner*
: Set whether or not you wish to use an External Scanning program such as
 Bar Code Scanner. If false a version of ZXing integrated into App Inventor
 will be used.

### Events  {#BarcodeScanner-Events}

{:.events}

{:id="BarcodeScanner.AfterScan"} AfterScan(*result*{:.text})
: Indicates that the scanner has read a (text) result and provides the result

### Methods  {#BarcodeScanner-Methods}

{:.methods}

{:id="BarcodeScanner.DoScan" class="method"} <i/> DoScan()
: Begins a barcode scan, using the camera. When the scan is complete, the
 AfterScan event will be raised.

## Barometer  {#Barometer}

Physical world component that can measure the ambient air pressure if
 supported by the hardware.



### Properties  {#Barometer-Properties}

{:.properties}

{:id="Barometer.AirPressure" .number .ro .bo} *AirPressure*
: The atmospheric pressure in hPa (millibar), if the sensor is available 
 and enabled.

{:id="Barometer.Available" .boolean .ro .bo} *Available*
: Specifies whether or not the device has the hardware to support the `Barometer` component.

{:id="Barometer.Enabled" .boolean} *Enabled*
: Specifies whether the sensor should generate events.  If `true`{:.logic.block},
 the sensor will generate events.  Otherwise, no events are
 generated.

{:id="Barometer.RefreshTime" .number} *RefreshTime*
: The requested minimum time in milliseconds between changes in readings being reported. Android is not guaranteed to honor the request. Setting this property has no effect on pre-Gingerbread devices.

### Events  {#Barometer-Events}

{:.events}

{:id="Barometer.AirPressureChanged"} AirPressureChanged(*pressure*{:.number})
: Called when a change is detected in the air pressure (provided in hPa).

### Methods  {#Barometer-Methods}

{:.methods}
None


## BatterySensor  {#BatterySensor}

Component for BatterySensor



### Properties  {#BatterySensor-Properties}

{:.properties}

{:id="BatterySensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="BatterySensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="BatterySensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="BatterySensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="BatterySensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#BatterySensor-Events}

{:.events}

{:id="BatterySensor.BatteryInfoReceived"} BatteryInfoReceived(*timestamp*{:.number},*scale*{:.number},*level*{:.number})
: Indicates that the battery status info has been received.

### Methods  {#BatterySensor-Methods}

{:.methods}

{:id="BatterySensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="BatterySensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Enable battery sensor to run once

{:id="BatterySensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="BatterySensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="BatterySensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## CallLogHistory  {#CallLogHistory}

Component for CallLogHistory



### Properties  {#CallLogHistory-Properties}

{:.properties}

{:id="CallLogHistory.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The type of the call (incoming = "1", outgoing = "2" or missed = "3").

{:id="CallLogHistory.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="CallLogHistory.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="CallLogHistory.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="CallLogHistory.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="CallLogHistory.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#CallLogHistory-Events}

{:.events}

{:id="CallLogHistory.CalllogsInfoReceived"} CalllogsInfoReceived(*date*{:.number},*duration*{:.number},*name*{:.text},*number*{:.text},*numberType*{:.text},*type*{:.text})
: Indicates that the calllog info has been received.

### Methods  {#CallLogHistory-Methods}

{:.methods}

{:id="CallLogHistory.AfterDate" class="method"} <i/> AfterDate(*datePoint*{:.text})
: Specify the date after which the callogs occurred
 The formate should be "YYYY-MM-DD HH:mm:ss" (see http://docs.oracle.com/javase /1.5.0/docs/api/java/text/SimpleDateFormat.html) 
 for example "2012-06-01 20:00:00" will calllogs after June 1, 8pm
 Empty value will return all calllogs

{:id="CallLogHistory.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="CallLogHistory.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Enable calllog history probe to run once

{:id="CallLogHistory.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="CallLogHistory.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="CallLogHistory.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## CellTowerProbeSensor  {#CellTowerProbeSensor}

Component for CellTowerProbeSensor



### Properties  {#CellTowerProbeSensor-Properties}

{:.properties}

{:id="CellTowerProbeSensor.Cellid" .number .ro .bo} *Cellid*
: Returns the latest reading of the cell id of the cell tower

{:id="CellTowerProbeSensor.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="CellTowerProbeSensor.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="CellTowerProbeSensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="CellTowerProbeSensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="CellTowerProbeSensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="CellTowerProbeSensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="CellTowerProbeSensor.LocationAreaCode" .number .ro .bo} *LocationAreaCode*
: Returns the latest reading of the area code of the cell tower

{:id="CellTowerProbeSensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

{:id="CellTowerProbeSensor.Timestamp" .number .ro .bo} *Timestamp*
: Returns the timestamp of latest reading

### Events  {#CellTowerProbeSensor-Events}

{:.events}

{:id="CellTowerProbeSensor.CellInfoReceived"} CellInfoReceived()
: Indicates that the celltower sensor info has been received.

{:id="CellTowerProbeSensor.CellTowerScanComplete"} CellTowerScanComplete()
: Indicates that one round of scan has finish

### Methods  {#CellTowerProbeSensor-Methods}

{:.methods}

{:id="CellTowerProbeSensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="CellTowerProbeSensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should "run once" to listen for celltower information

{:id="CellTowerProbeSensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="CellTowerProbeSensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="CellTowerProbeSensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## Clock  {#Clock}

![Image of the Clock component](images/clock.png)

 Non-visible component that provides the instant in time using the internal clock on the phone.
 It can fire a timer at regularly set intervals and perform time calculations, manipulations,
 and conversions.

 Operations on dates and times, such as from [`DatePicker`](userinterface.html#DatePicker) and
 [`TimePicker`](userinterface.html#TimePicker), are accomplished through methods in Clock. Date
 and Time are represented as InstantInTime and Duration.

  - **Instant**: consists of Year, Month, DayOfMnoth, Hour, Minute, and SEcond. An instant can be
    created using the [`MakeInstant`](#Clock.MakeInstant), [`MakeInstantFromMillis`](#Clock.MakeInstantFromMillis) and
    [`MakeInstantFromParts`](#Clock.MakeInstantFromParts) methods.
  - **Duration**: time in milliseconds elapsed between instants. Duration can be obtained by the
    [`Duration`](#Clock.Duration) method.

 Instants are assumed to be in the device's local time zone. When they are converted to or from
 milliseconds, the milliseconds for a given Instance are calculated from January 1, 1970 in UTC
 (Greenwich Mean Time).

 Methods to convert an Instant to text are also available. Acceptable patterns are empty string,
 `MM/dd/YYYY HH:mm:ss a`, or `MMM d, yyyy HH:mm`. The empty string will provide the default
 format, which is `"MMM d, yyyy HH:mm:ss a"` for [`FormatDateTime`](#Clock.FormatDateTime),
 `"MMM d, yyyy"` for [`FormatDate`](#Clock.FormatDate). To see all possible formats, please
 see [here](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).

 A note on combining date and time: In order to combine the date from one Instant and the time
 from another, for example from a [`DatePicker`](userinterface.html#DatePicker) and
 [`TimePicker`](userinterface.html#TimePicker), extract the parts as text and use the text to
 create a new Instant. For example:

 ![Example code blocks to combine date and time picker data](images/date_and_time_instant.png)



### Properties  {#Clock-Properties}

{:.properties}

{:id="Clock.TimerAlwaysFires" .boolean} *TimerAlwaysFires*
: Will fire even when application is not showing on the screen if true

{:id="Clock.TimerEnabled" .boolean} *TimerEnabled*
: Specifies whether the [`Timer`](#Clock.Timer) event should run.

{:id="Clock.TimerInterval" .number} *TimerInterval*
: Specifies the interval between subsequent [`Timer`](#Clock.Timer) events.

   **Note**: Drift may occur over time and that the system may not honor the
 timing specified here if the app or another process on the phone is busy.

### Events  {#Clock-Events}

{:.events}

{:id="Clock.Timer"} Timer()
: The Timer event runs when the timer has gone off.

### Methods  {#Clock-Methods}

{:.methods}

{:id="Clock.AddDays" class="method returns InstantInTime"} <i/> AddDays(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some days after the given instant.

{:id="Clock.AddDuration" class="method returns InstantInTime"} <i/> AddDuration(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some duration after the argument

{:id="Clock.AddHours" class="method returns InstantInTime"} <i/> AddHours(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some hours after the given instant.

{:id="Clock.AddMinutes" class="method returns InstantInTime"} <i/> AddMinutes(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some minutes after the given instant.

{:id="Clock.AddMonths" class="method returns InstantInTime"} <i/> AddMonths(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some months after the given instant.

{:id="Clock.AddSeconds" class="method returns InstantInTime"} <i/> AddSeconds(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some seconds after the given instant.

{:id="Clock.AddWeeks" class="method returns InstantInTime"} <i/> AddWeeks(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns An instant in time some weeks after the given instant.

{:id="Clock.AddYears" class="method returns InstantInTime"} <i/> AddYears(*instant*{:.InstantInTime},*quantity*{:.number})
: Returns an instant in time some years after the given instant.

{:id="Clock.DayOfMonth" class="method returns number"} <i/> DayOfMonth(*instant*{:.InstantInTime})
: Returns the day of the month.

{:id="Clock.Duration" class="method returns number"} <i/> Duration(*start*{:.InstantInTime},*end*{:.InstantInTime})
: Returns the milliseconds by which end follows start (+ or -)

{:id="Clock.DurationToDays" class="method returns number"} <i/> DurationToDays(*duration*{:.number})
: Returns the duration converted from milliseconds to days.

{:id="Clock.DurationToHours" class="method returns number"} <i/> DurationToHours(*duration*{:.number})
: Returns the duration converted from milliseconds to hours.

{:id="Clock.DurationToMinutes" class="method returns number"} <i/> DurationToMinutes(*duration*{:.number})
: Returns the duration converted from milliseconds to minutes.

{:id="Clock.DurationToSeconds" class="method returns number"} <i/> DurationToSeconds(*duration*{:.number})
: Returns the duration converted from milliseconds to seconds.

{:id="Clock.DurationToWeeks" class="method returns number"} <i/> DurationToWeeks(*duration*{:.number})
: Returns the duration converted from milliseconds to weeks.

{:id="Clock.FormatDate" class="method returns text"} <i/> FormatDate(*instant*{:.InstantInTime},*pattern*{:.text})
: Converts and formats an instant into a string of date with the specified pattern. To learn
 more about valid patterns, please see
 [SimpleDateFormat](https://developer.android.com/reference/java/text/SimpleDateFormat).

{:id="Clock.FormatDateTime" class="method returns text"} <i/> FormatDateTime(*instant*{:.InstantInTime},*pattern*{:.text})
: Converts and formats an instant into a string of date and time with the specified pattern.
 To learn more about valid patterns, please see
 [SimpleDateFormat](https://developer.android.com/reference/java/text/SimpleDateFormat).

{:id="Clock.FormatTime" class="method returns text"} <i/> FormatTime(*instant*{:.InstantInTime})
: Converts and formats the given instant into a string with the specified pattern. To learn
 more about valid patterns, please see
 [SimpleDateFormat](https://developer.android.com/reference/java/text/SimpleDateFormat).

{:id="Clock.GetMillis" class="method returns number"} <i/> GetMillis(*instant*{:.InstantInTime})
: Returns the instant in time measured as milliseconds since 1970.

{:id="Clock.Hour" class="method returns number"} <i/> Hour(*instant*{:.InstantInTime})
: Returns the hours for the given date.

{:id="Clock.MakeDate" class="method returns InstantInTime"} <i/> MakeDate(*year*{:.number},*month*{:.number},*day*{:.number})
: Returns an instant in time specified by year, month, date in UTC.
Valid values for the month field are 1-12 and 1-31 for the day field.

{:id="Clock.MakeInstant" class="method returns InstantInTime"} <i/> MakeInstant(*from*{:.text})
: Returns an instant in time specified by MM/dd/YYYY hh:mm:ss or MM/dd/YYYY or hh:mm.

{:id="Clock.MakeInstantFromMillis" class="method returns InstantInTime"} <i/> MakeInstantFromMillis(*millis*{:.number})
: Returns an instant in time specified by the milliseconds since 1970 in UTC.

{:id="Clock.MakeInstantFromParts" class="method returns InstantInTime"} <i/> MakeInstantFromParts(*year*{:.number},*month*{:.number},*day*{:.number},*hour*{:.number},*minute*{:.number},*second*{:.number})
: Returns an instant in time specified by year, month, date, hour, minute, second in UTC.

{:id="Clock.MakeTime" class="method returns InstantInTime"} <i/> MakeTime(*hour*{:.number},*minute*{:.number},*second*{:.number})
: Returns an instant in time specified by hour, minute, second in UTC.

{:id="Clock.Minute" class="method returns number"} <i/> Minute(*instant*{:.InstantInTime})
: Returns the minutes for the given date.

{:id="Clock.Month" class="method returns number"} <i/> Month(*instant*{:.InstantInTime})
: Returns the number of the month for the given instant.

{:id="Clock.MonthName" class="method returns text"} <i/> MonthName(*instant*{:.InstantInTime})
: Returns the name of the month for the given instant.

{:id="Clock.Now" class="method returns InstantInTime"} <i/> Now()
: Returns the current instant in time read from phone's clock.

{:id="Clock.Second" class="method returns number"} <i/> Second(*instant*{:.InstantInTime})
: Returns the seconds for the given instant.

{:id="Clock.SystemTime" class="method returns number"} <i/> SystemTime()
: Returns the phone's internal time.

{:id="Clock.Weekday" class="method returns number"} <i/> Weekday(*instant*{:.InstantInTime})
: Returns the weekday for the given instant.

{:id="Clock.WeekdayName" class="method returns text"} <i/> WeekdayName(*instant*{:.InstantInTime})
: Returns the name of the weekday for the given instant.

{:id="Clock.Year" class="method returns number"} <i/> Year(*instant*{:.InstantInTime})
: Returns the year of the given instant.

## ContactInfo  {#ContactInfo}

Component for ContactInfo



### Properties  {#ContactInfo-Properties}

{:.properties}

{:id="ContactInfo.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="ContactInfo.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="ContactInfo.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="ContactInfo.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="ContactInfo.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#ContactInfo-Events}

{:.events}

{:id="ContactInfo.ContactInfoReceived"} ContactInfoReceived(*firstName*{:.text},*familyName*{:.text},*displayName*{:.text},*phoneNum*{:.text},*phoneType*{:.text},*email*{:.text},*emailType*{:.text},*address*{:.text},*addressType*{:.text},*organizationName*{:.text},*website*{:.text},*notes*{:.text})
: Indicates that the contact info has been received.

### Methods  {#ContactInfo-Methods}

{:.methods}

{:id="ContactInfo.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="ContactInfo.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Enable contact information sensor to run once

{:id="ContactInfo.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="ContactInfo.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="ContactInfo.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## GyroscopeSensor  {#GyroscopeSensor}

Component providing data from the device's gyroscope sensor.



### Properties  {#GyroscopeSensor-Properties}

{:.properties}

{:id="GyroscopeSensor.Available" .boolean .ro .bo} *Available*
: Indicates whether a gyroscope sensor is available.

{:id="GyroscopeSensor.Enabled" .boolean} *Enabled*
: Enabled property getter method.

{:id="GyroscopeSensor.XAngularVelocity" .number .ro .bo} *XAngularVelocity*
: The angular velocity around the X axis, in degrees per second.

{:id="GyroscopeSensor.YAngularVelocity" .number .ro .bo} *YAngularVelocity*
: The angular velocity around the Y axis, in degrees per second.

{:id="GyroscopeSensor.ZAngularVelocity" .number .ro .bo} *ZAngularVelocity*
: The angular velocity around the Z axis, in degrees per second.

### Events  {#GyroscopeSensor-Events}

{:.events}

{:id="GyroscopeSensor.GyroscopeChanged"} GyroscopeChanged(*xAngularVelocity*{:.number},*yAngularVelocity*{:.number},*zAngularVelocity*{:.number},*timestamp*{:.number})
: Indicates that the gyroscope sensor data has changed. The timestamp parameter is the time in nanoseconds at which the event occurred.

### Methods  {#GyroscopeSensor-Methods}

{:.methods}
None


## Hygrometer  {#Hygrometer}

Physical world component that can measure the relative ambient air 
 humidity if supported by the hardware.



### Properties  {#Hygrometer-Properties}

{:.properties}

{:id="Hygrometer.Available" .boolean .ro .bo} *Available*
: Specifies whether or not the device has the hardware to support the `Hygrometer` component.

{:id="Hygrometer.Enabled" .boolean} *Enabled*
: Specifies whether the sensor should generate events.  If `true`{:.logic.block},
 the sensor will generate events.  Otherwise, no events are
 generated.

{:id="Hygrometer.Humidity" .number .ro .bo} *Humidity*
: Returns the relative ambient humidity as a percentage.
 The sensor must be enabled and available 
 to return meaningful values.

{:id="Hygrometer.RefreshTime" .number} *RefreshTime*
: The requested minimum time in milliseconds between changes in readings being reported. Android is not guaranteed to honor the request. Setting this property has no effect on pre-Gingerbread devices.

### Events  {#Hygrometer-Events}

{:.events}

{:id="Hygrometer.HumidityChanged"} HumidityChanged(*humidity*{:.number})
: Indicates the relative humidity changed.

### Methods  {#Hygrometer-Methods}

{:.methods}
None


## LightSensor  {#LightSensor}

Physical world component that can measure the light level.



### Properties  {#LightSensor-Properties}

{:.properties}

{:id="LightSensor.Available" .boolean .ro .bo} *Available*
: Specifies whether or not the device has the hardware to support the `LightSensor` component.

{:id="LightSensor.AverageLux" .number .ro .bo} *AverageLux*
: Returns the brightness in lux by averaging the previous 10 measured values.
 The sensor must be enabled and available to return meaningful values.

{:id="LightSensor.Enabled" .boolean} *Enabled*
: Specifies whether the sensor should generate events.  If `true`{:.logic.block},
 the sensor will generate events.  Otherwise, no events are
 generated.

{:id="LightSensor.Lux" .number .ro .bo} *Lux*
: Returns the last measured brightness in lux.
 The sensor must be enabled and available to return meaningful values.

{:id="LightSensor.RefreshTime" .number} *RefreshTime*
: The requested minimum time in milliseconds between changes in readings being reported. Android is not guaranteed to honor the request. Setting this property has no effect on pre-Gingerbread devices.

{:id="LightSensor.Timestamp" .number .ro .bo} *Timestamp*
: Returns the timestamp of latest reading.

### Events  {#LightSensor-Events}

{:.events}

{:id="LightSensor.LightChanged"} LightChanged(*lux*{:.number})
: Indicates the light level changed.

### Methods  {#LightSensor-Methods}

{:.methods}
None


## LocationProbeSensor  {#LocationProbeSensor}

Record GPS location periodically



### Properties  {#LocationProbeSensor-Properties}

{:.properties}

{:id="LocationProbeSensor.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="LocationProbeSensor.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="LocationProbeSensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="LocationProbeSensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="LocationProbeSensor.GoodEnoughAccuracy" .number} *GoodEnoughAccuracy*
: The goodEnoughAccuracy of the location data that the sensor listens to

{:id="LocationProbeSensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="LocationProbeSensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="LocationProbeSensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

{:id="LocationProbeSensor.UseCache" .boolean} *UseCache*
: Set whether the location info will use the last known location without acquiring new location from 
 GPS or Network fix

{:id="LocationProbeSensor.UseGPS" .boolean} *UseGPS*
: Indicate whether the locationProbe uses GPS or not

{:id="LocationProbeSensor.UseNetwork" .boolean} *UseNetwork*
: Indicate whether the locationProbe uses GPS or not

### Events  {#LocationProbeSensor-Events}

{:.events}

{:id="LocationProbeSensor.LocationInfoReceived"} LocationInfoReceived(*timestamp*{:.number},*mLatitude*{:.number},*mLongitude*{:.number},*mAccuracy*{:.number},*mProvider*{:.text})
: Indicates that the Location info has been received.

{:id="LocationProbeSensor.LocationUpdateComplete"} LocationUpdateComplete()
: Indicates that the updating Location info has completed.

### Methods  {#LocationProbeSensor-Methods}

{:.methods}

{:id="LocationProbeSensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="LocationProbeSensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should "run once" to listen for location information
 and raise the corresponding events.

{:id="LocationProbeSensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="LocationProbeSensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="LocationProbeSensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## LocationSensor  {#LocationSensor}

Non-visible component providing location information, including [`Latitude`](#LocationSensor.Latitude),
 [`Longitude`](#LocationSensor.Longitude), [`Altitude`](#LocationSensor.Altitude) (if supported by the device), speed (if supported by
 the device), and address. This can also perform "geocoding", converting a given address (not
 necessarily the current one) to a latitude (with the [`LatitudeFromAddress`](#LocationSensor.LatitudeFromAddress)
 method) and a longitude (with the [`LongitudeFromAddress`](#LocationSensor.LongitudeFromAddress) method).

 In order to function, the component must have its [`Enabled`](#LocationSensor.Enabled) property set to
 `true`{:.logic.block}, and the device must have location sensing enabled through wireless
 networks or GPS satellites (if outdoors).

 Location information might not be immediately available when an app starts. You'll have to wait
 a short time for a location provider to be found and used, or wait for the
 [`LocationChanged`](#LocationSensor.LocationChanged) event.

 The emulator does not emulate sensors on all devices. Code should be tested on a physical device.



### Properties  {#LocationSensor-Properties}

{:.properties}

{:id="LocationSensor.Accuracy" .number .ro .bo} *Accuracy*
: The `LocationSensor` will be able to locate the device with a varying degree of confidence,
 based on the quality of satellite, cell towers, and other data used to estimate location.
 The `Accuracy` value is the radius in meters around the sensor's detected location. The device
 has a 68% chance to be located within this radius. More precise location detection will result
 in a smaller accuracy number, which allows the app to have more confidence where the device
 is actually located.

   If the accuracy is not known, the return value is 0.0

{:id="LocationSensor.Altitude" .number .ro .bo} *Altitude*
: Altitude of the device measured in meters, if available.

   Altitude is measured from the
 [World Geodetic System 84 reference ellipsoid](https://gisgeography.com/wgs84-world-geodetic-system/),
 not sea level.

   Note that it is difficult for devices to accurately sense altitude. Altitude reported on a
 phone/tablet can easily be off by 30 meters or more.

{:id="LocationSensor.AvailableProviders" .list .ro .bo} *AvailableProviders*
: List of available service providers, such as gps or network. This information is provided
 as a list and in text form.

{:id="LocationSensor.CurrentAddress" .text .ro .bo} *CurrentAddress*
: Physical street address of the device from Google's map database.

   The address might not always be available from the provider, and the address reported may not
 always be of the building where the device is located.

   If Google has no address information available for a particular location, this will return
 `No address available`.

{:id="LocationSensor.DistanceInterval" .number} *DistanceInterval*
: Determines the minimum distance interval, in meters, that the sensor will try to use for
 sending out location updates. For example, if this is set to 50, then the sensor will fire a
 [`LocationChanged`](#LocationSensor.LocationChanged) event only after 50 meters have been
 traversed. However, the sensor does not guarantee that an update will be received at exactly
 the distance interval. It may take more than 5 meters to fire an event, for instance.

   It is also useful to check against [`Accuracy`](#LocationSensor.Accuracy) when using this property. When your
 device is moving, the accuracy of the detected location is constantly changing.

{:id="LocationSensor.Enabled" .boolean} *Enabled*
: If `true`{:.logic.block}, the `LocationSensor` will attempt to read location information from
 GPS, WiFi location, or other means available on the device. This setting does not control
 whether location information is actually available. Device location must be enabled or
 disabled in the device settings.

{:id="LocationSensor.HasAccuracy" .boolean .ro .bo} *HasAccuracy*
: If `true`{:.logic.block}, the device can report its accuracy level.

{:id="LocationSensor.HasAltitude" .boolean .ro .bo} *HasAltitude*
: If `true`{:.logic.block}, the device can report its altitude.

{:id="LocationSensor.HasLongitudeLatitude" .boolean .ro .bo} *HasLongitudeLatitude*
: If `true`{:.logic.block}, the device can report longitude and latitude.  It is
 always the case that either both or neither are.

{:id="LocationSensor.Latitude" .number .ro .bo} *Latitude*
: The most recently available latitude value in degrees reported to 5 decimal places.
 If no value is available, 0 will be returned.
 Latitude is a value between 90 (north) and -90 (south), where 0 marks the Equator.

{:id="LocationSensor.Longitude" .number .ro .bo} *Longitude*
: The most recent available longitude value in degrees reported to 5 decimal places.
 If no value is available, 0 will be returned.
 Longitude is a value between 180 (east) and -180 (west), where 0 marks the Prime Meridian.

{:id="LocationSensor.ProviderLocked" .boolean .bo} *ProviderLocked*
: The device will not change the service provider.

   It is possible for a device to switch service providers when the current provider is unable
 to provide adequate location information. `ProviderLocked` is a Boolean value: true/false.
 Set to `true`{:.logic.block} to prevent providers from changing. Set to `false`{:.logic.block}
 to allow for automatic switching when necessary.

{:id="LocationSensor.ProviderName" .text .bo} *ProviderName*
: The current service provider. The provider will most likely be either GPS or network.

{:id="LocationSensor.TimeInterval" .number} *TimeInterval*
: Determines the minimum time interval, in milliseconds, that the sensor will try to use for
 sending out location updates. However, location updates will only be received when the
 location of the phone actually changes, and use of the specified time interval is not
 guaranteed. For example, if 30000 is used as the time interval, location updates will never
 be fired sooner than 30000ms, but they may be fired anytime after.

   Values smaller than 30000ms (30 seconds) are not practical for most devices. Small values
 may drain battery and overwork the GPS.

### Events  {#LocationSensor-Events}

{:.events}

{:id="LocationSensor.LocationChanged"} LocationChanged(*latitude*{:.number},*longitude*{:.number},*altitude*{:.number},*speed*{:.number})
: Indicates that a new location has been detected. Speed is reported in meters/second
 Other values match their properties.

{:id="LocationSensor.StatusChanged"} StatusChanged(*provider*{:.text},*status*{:.text})
: Indicates that the status of the location provider service has changed, such as when a
 provider is lost or a new provider starts being used.

### Methods  {#LocationSensor-Methods}

{:.methods}

{:id="LocationSensor.LatitudeFromAddress" class="method returns number"} <i/> LatitudeFromAddress(*locationName*{:.text})
: Derives latitude from the given `locationName`.

{:id="LocationSensor.LongitudeFromAddress" class="method returns number"} <i/> LongitudeFromAddress(*locationName*{:.text})
: Derives longitude from the given `locationName`.

## MagneticFieldSensor  {#MagneticFieldSensor}

Component for MagneticFieldSensor



### Properties  {#MagneticFieldSensor-Properties}

{:.properties}

{:id="MagneticFieldSensor.AbsoluteStrength" .number .ro .bo} *AbsoluteStrength*
: Indicates the absolute strength of the field.

{:id="MagneticFieldSensor.Available" .boolean .ro .bo} *Available*
: Indicates that there is a magnetic field sensor in the device and it is available.

{:id="MagneticFieldSensor.Enabled" .boolean} *Enabled*
: Indicates whether or not the magnetic field sensor is enabled and working.

{:id="MagneticFieldSensor.MaximumRange" .number .ro .bo} *MaximumRange*
: Indicates the maximum range the magnetic sensor can reach.

{:id="MagneticFieldSensor.XStrength" .number .ro .bo} *XStrength*
: Indicates the field's strength in the X-axis.

{:id="MagneticFieldSensor.YStrength" .number .ro .bo} *YStrength*
: Indicates the field's strength in the Y-axis.

{:id="MagneticFieldSensor.ZStrength" .number .ro .bo} *ZStrength*
: Indicates the field's strength in the Z-axis.

### Events  {#MagneticFieldSensor-Events}

{:.events}

{:id="MagneticFieldSensor.MagneticChanged"} MagneticChanged(*xStrength*{:.number},*yStrength*{:.number},*zStrength*{:.number},*absoluteStrength*{:.number})
: Triggers when magnetic field has changed, setting the new values in parameters.

### Methods  {#MagneticFieldSensor-Methods}

{:.methods}
None


## NearField  {#NearField}

Non-visible component to provide NFC capabilities. For now this component supports the reading
 and writing of text tags only (if supported by the device).

 In order to read and write text tags, the component must have its [`ReadMode`](#NearField.ReadMode)
 property set to `true`{:.logic.block} or `false`{:.logic.block} respectively.

 **Note:** This component will only work on Screen1 of any App Inventor app.



### Properties  {#NearField-Properties}

{:.properties}

{:id="NearField.LastMessage" .text .ro .bo} *LastMessage*
: Returns the content of the most recently received tag.

{:id="NearField.ReadMode" .boolean} *ReadMode*
: Specifies whether the NFC hardware should operate in read mode (`true`{:.logic.block}) or
 write mode (`false`{:.logic.block}).

{:id="NearField.TextToWrite" .text .bo} *TextToWrite*
: Specifies the content that will be written to the tag when in write mode. This method has no
 effect if [`ReadMode`](#NearField.ReadMode) is `true`{:.logic.block}.

{:id="NearField.WriteType" .number .ro .bo} *WriteType*
: Returns the write type for the NFC component. For this version of the component, it is always
 `1`.

### Events  {#NearField-Events}

{:.events}

{:id="NearField.TagRead"} TagRead(*message*{:.text})
: Indicates that a new tag has been detected.
 Currently this is only a plain text tag, as specified in the
 manifest.

{:id="NearField.TagWritten"} TagWritten()
: Indicates that a tag has come into range of the NFC sensor and has been written.

### Methods  {#NearField-Methods}

{:.methods}
None


## OrientationSensor  {#OrientationSensor}

![Example of the OrientationSensor icon](images/orientationsensor.png)

 Use an orientation sensor component to determine the phone's spatial orientation.

 An orientation sensor is a non-visible component that reports the following three values, in
 degrees:

  - **Roll** : 0 degree when the device is level, increasing to 90 degrees as the device is
    tilted up onto its left side, and decreasing to −90 degrees when the device is tilted up onto
    its right side.
  - **Pitch** : 0 degree when the device is level, increasing to 90 degrees as the device is
    tilted so its top is pointing down, then decreasing to 0 degree as it gets turned over.
    Similarly, as the device is tilted so its bottom points down, pitch decreases to −90 degrees,
    then increases to 0 degree as it gets turned all the way over.
  - **Azimuth** : 0 degree when the top of the device is pointing north, 90 degrees when it is
    pointing east, 180 degrees when it is pointing south, 270 degrees when it is pointing west,
    etc.

 These measurements assume that the device itself is not moving.



### Properties  {#OrientationSensor-Properties}

{:.properties}

{:id="OrientationSensor.Angle" .number .ro .bo} *Angle*
: Returns an angle that tells the direction in which the device is tiled. That is, it tells the
 direction of the force that would be felt by a ball rolling on the surface of the device.

{:id="OrientationSensor.Available" .boolean .ro .bo} *Available*
: Indicates whether the orientation sensor is present on the device.

{:id="OrientationSensor.Azimuth" .number .ro .bo} *Azimuth*
: Returns the azimuth angle of the device.
 To return meaningful values the sensor must be enabled.

{:id="OrientationSensor.Enabled" .boolean} *Enabled*
: Specifies whether the orientation sensor is enabled.

{:id="OrientationSensor.Magnitude" .number .ro .bo} *Magnitude*
: Returns a number between 0 and 1 indicating how much the device
 is tilted. It gives the magnitude of the force that would be felt
 by a ball rolling on the surface of the device. For the angle of
 tilt, use [`Angle`](#OrientationSensor.Angle).

{:id="OrientationSensor.Pitch" .number .ro .bo} *Pitch*
: Returns the pitch angle of the device.
 To return meaningful values the sensor must be enabled.

{:id="OrientationSensor.Roll" .number .ro .bo} *Roll*
: Returns the roll angle of the device.
 To return meaningful values the sensor must be enabled.

### Events  {#OrientationSensor-Events}

{:.events}

{:id="OrientationSensor.OrientationChanged"} OrientationChanged(*azimuth*{:.number},*pitch*{:.number},*roll*{:.number})
: The `OrientationChanged` event handler is run when the orientation has changed.

### Methods  {#OrientationSensor-Methods}

{:.methods}
None


## Pedometer  {#Pedometer}

This component keeps count of steps using the accelerometer.



### Properties  {#Pedometer-Properties}

{:.properties}

{:id="Pedometer.Distance" .number .ro .bo} *Distance*
: Returns the approximate distance traveled in meters.

{:id="Pedometer.ElapsedTime" .number .ro .bo} *ElapsedTime*
: Returns the time elapsed in milliseconds since the pedometer has started.

{:id="Pedometer.SimpleSteps" .number .ro .bo} *SimpleSteps*
: Returns the number of simple steps taken since the pedometer has started.

{:id="Pedometer.StopDetectionTimeout" .number} *StopDetectionTimeout*
: Returns the duration of idleness (no steps detected) after which to go into a "stopped" state.

{:id="Pedometer.StrideLength" .number} *StrideLength*
: Returns the current estimate of stride length in meters, if calibrated, or returns the
 default (0.73 m) otherwise.

{:id="Pedometer.WalkSteps" .number .ro .bo} *WalkSteps*
: Returns the number of walk steps taken since the pedometer has started.

### Events  {#Pedometer-Events}

{:.events}

{:id="Pedometer.SimpleStep"} SimpleStep(*simpleSteps*{:.number},*distance*{:.number})
: This event is run when a raw step is detected.

{:id="Pedometer.WalkStep"} WalkStep(*walkSteps*{:.number},*distance*{:.number})
: This event is run when a walking step is detected. A walking step is a step that appears to be involved in forward motion.

### Methods  {#Pedometer-Methods}

{:.methods}

{:id="Pedometer.Reset" class="method"} <i/> Reset()
: Resets the step counter, distance measure and time running.

{:id="Pedometer.Save" class="method"} <i/> Save()
: Saves the pedometer state to the phone. Permits permits accumulation of steps and distance between invocations of an App that uses the pedometer. Different Apps will have their own saved state.

{:id="Pedometer.Start" class="method"} <i/> Start()
: Starts the pedometer.

{:id="Pedometer.Stop" class="method"} <i/> Stop()
: Stops the pedometer.

## PedometerSensor  {#PedometerSensor}

Component for PedometerSensor



### Properties  {#PedometerSensor-Properties}

{:.properties}

{:id="PedometerSensor.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="PedometerSensor.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="PedometerSensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="PedometerSensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="PedometerSensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="PedometerSensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="PedometerSensor.SensitivityLevel" .text} *SensitivityLevel*
: The sensitivity level used to detect walking. 9 different options for this value: "extra high", "very high", "high", "higher", "medium", "lower", "low", "very low", "extra low"

{:id="PedometerSensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#PedometerSensor-Events}

{:.events}

{:id="PedometerSensor.PedometerInfoReceived"} PedometerInfoReceived(*timestamp*{:.number})
: Indicates that the pedometer(one walking step) info has been received.

{:id="PedometerSensor.PedometerScanComplete"} PedometerScanComplete()
: Indicates that one round of scan has finish

### Methods  {#PedometerSensor-Methods}

{:.methods}

{:id="PedometerSensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="PedometerSensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Enable pedometer sensor to run once

{:id="PedometerSensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="PedometerSensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="PedometerSensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## ProximitySensor  {#ProximitySensor}

A sensor component that can measure the proximity of an object (in cm) relative to the view
 screen of a device. This sensor is typically used to determine whether a handset is being held
 up to a persons ear; i.e. lets you determine how far away an object is from a device. Many
 devices return the absolute distance, in cm, but some return only near and far values. In this
 case, the sensor usually reports its maximum range value in the far state and a lesser value
 in the near state. It reports the following value:

   - **Distance**: The distance from the object to the device



### Properties  {#ProximitySensor-Properties}

{:.properties}

{:id="ProximitySensor.Available" .boolean .ro .bo} *Available*
: Reports whether or not the device has a proximity sensor.

{:id="ProximitySensor.Distance" .number .ro .bo} *Distance*
: Returns the distance from the object to the device.
 The sensor must be enabled to return meaningful values.

{:id="ProximitySensor.Enabled" .boolean} *Enabled*
: If true, the sensor will generate events.  Otherwise, no events
 are generated.

{:id="ProximitySensor.KeepRunningWhenOnPause" .boolean} *KeepRunningWhenOnPause*
: Returns value of keepRunningWhenOnPause.

{:id="ProximitySensor.MaximumRange" .number .ro .bo} *MaximumRange*
: Determines a sensor's maximum range. Some proximity sensors return binary values
 that represent "near" or "far." In this case, the sensor usually reports
 its maximum range value in the far state and a lesser value in the near state.
 Typically, the far value is a value > 5 cm, but this can vary from sensor to sensor.

### Events  {#ProximitySensor-Events}

{:.events}

{:id="ProximitySensor.ProximityChanged"} ProximityChanged(*distance*{:.number})
: Triggered when distance (in cm) of the object to the device changes.

### Methods  {#ProximitySensor-Methods}

{:.methods}
None


## RunningApplications  {#RunningApplications}

Component for RunningApplications



### Properties  {#RunningApplications-Properties}

{:.properties}

{:id="RunningApplications.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="RunningApplications.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="RunningApplications.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="RunningApplications.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="RunningApplications.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="RunningApplications.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="RunningApplications.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#RunningApplications-Events}

{:.events}

{:id="RunningApplications.AppsInfoReceived"} AppsInfoReceived(*timestamp*{:.number},*appName*{:.text},*className*{:.text},*packageName*{:.text})
: Indicates that the running applications info has been received.

### Methods  {#RunningApplications-Methods}

{:.methods}

{:id="RunningApplications.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="RunningApplications.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should "run once" to probe the current running applications

{:id="RunningApplications.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="RunningApplications.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="RunningApplications.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## ScreenStatus  {#ScreenStatus}

Component for ScreenStatus



### Properties  {#ScreenStatus-Properties}

{:.properties}

{:id="ScreenStatus.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="ScreenStatus.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="ScreenStatus.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="ScreenStatus.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="ScreenStatus.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="ScreenStatus.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="ScreenStatus.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#ScreenStatus-Events}

{:.events}

{:id="ScreenStatus.ScreenInfoReceived"} ScreenInfoReceived(*timestamp*{:.number},*screenOn*{:.boolean})
: Indicates that the running applications info has been received.

### Methods  {#ScreenStatus-Methods}

{:.methods}

{:id="ScreenStatus.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="ScreenStatus.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should "run once" to listen for current screenStatus

{:id="ScreenStatus.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="ScreenStatus.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="ScreenStatus.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## SmsHistory  {#SmsHistory}

Component for SmsHistory



### Properties  {#SmsHistory-Properties}

{:.properties}

{:id="SmsHistory.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="SmsHistory.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="SmsHistory.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="SmsHistory.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="SmsHistory.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="SmsHistory.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#SmsHistory-Events}

{:.events}

{:id="SmsHistory.SmsInfoReceived"} SmsInfoReceived(*date*{:.number},*address*{:.text},*body*{:.text},*type*{:.text},*read*{:.boolean})
: Indicates that the calllog info has been received.

### Methods  {#SmsHistory-Methods}

{:.methods}

{:id="SmsHistory.AfterDate" class="method"} <i/> AfterDate(*datePoint*{:.text})
: Specify the date after which the sms message occurred
 The format should be "YYYY-MM-DD HH:mm:ss" (see http://docs.oracle.com/javase /1.5.0/docs/api/java/text/SimpleDateFormat.html) 
 for example "2012-06-01 20:00:00" will read all SMS after June 1, 8pm
 Empty value will return all SMS messages

{:id="SmsHistory.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="SmsHistory.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Enable sms history sensor to run once

{:id="SmsHistory.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="SmsHistory.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="SmsHistory.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## SocialProximitySensor  {#SocialProximitySensor}

Detects whether or not a Bluetooth-enabled device is close by or not.



### Properties  {#SocialProximitySensor-Properties}

{:.properties}

{:id="SocialProximitySensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="SocialProximitySensor.Enabled" .boolean .ro .bo} *Enabled*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="SocialProximitySensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="SocialProximitySensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="SocialProximitySensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="SocialProximitySensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#SocialProximitySensor-Events}

{:.events}

{:id="SocialProximitySensor.SocialProximityInfoReceived"} SocialProximityInfoReceived(*timestamp*{:.number},*deviceName*{:.text},*macAddress*{:.text},*rssi*{:.number},*mClass*{:.number})
: Indicates that the proximity sensor info has been received.

{:id="SocialProximitySensor.SocialProximityScanComplete"} SocialProximityScanComplete()
: Indicates that one round of scan has finish

### Methods  {#SocialProximitySensor-Methods}

{:.methods}

{:id="SocialProximitySensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="SocialProximitySensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should run once to listen for proximity
 changes and raise the corresponding events.

{:id="SocialProximitySensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="SocialProximitySensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="SocialProximitySensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

{:id="SocialProximitySensor.SetDiscoverable" class="method"} <i/> SetDiscoverable(*seconds*{:.number})
: Enabling discoverability

{:id="SocialProximitySensor.getOwnDeviceName" class="method returns text"} <i/> getOwnDeviceName()
: Get your own device name (Bluetooth)

{:id="SocialProximitySensor.setOwnDeviceName" class="method"} <i/> setOwnDeviceName(*name*{:.text})
: Overwrite your own device name (Bluetooth)

## TelephonyInfo  {#TelephonyInfo}

This is a wrapper class for Android's telephonyManager. 
 For more information about telephonyManager read its documentation at
 http://developer.android.com/reference/android/telephony/TelephonyManager.html



### Properties  {#TelephonyInfo-Properties}

{:.properties}

{:id="TelephonyInfo.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="TelephonyInfo.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="TelephonyInfo.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="TelephonyInfo.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="TelephonyInfo.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#TelephonyInfo-Events}

{:.events}

{:id="TelephonyInfo.TelephonyInfoReceived"} TelephonyInfoReceived(*deviceId*{:.text},*lineNumber*{:.text},*voicemailNumber*{:.text},*networkOperator*{:.text},*networkOperatorName*{:.text},*networkCounteryIso*{:.text},*simSerialNumber*{:.text},*simOperator*{:.text})
: Indicates that the Telephony info has been received.

### Methods  {#TelephonyInfo-Methods}

{:.methods}

{:id="TelephonyInfo.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="TelephonyInfo.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Enable telephonyInfo sensor to run once

{:id="TelephonyInfo.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="TelephonyInfo.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="TelephonyInfo.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/

## Thermometer  {#Thermometer}

Physical world component that can measure the ambient air temperature if
 supported by the hardware.



### Properties  {#Thermometer-Properties}

{:.properties}

{:id="Thermometer.Available" .boolean .ro .bo} *Available*
: Specifies whether or not the device has the hardware to support the `Thermometer` component.

{:id="Thermometer.Enabled" .boolean} *Enabled*
: Specifies whether the sensor should generate events.  If `true`{:.logic.block},
 the sensor will generate events.  Otherwise, no events are
 generated.

{:id="Thermometer.RefreshTime" .number} *RefreshTime*
: The requested minimum time in milliseconds between changes in readings being reported. Android is not guaranteed to honor the request. Setting this property has no effect on pre-Gingerbread devices.

{:id="Thermometer.Temperature" .number .ro .bo} *Temperature*
: Returns the temperature in degrees Celsius.
 The sensor must be enabled and available 
 to return meaningful values.

### Events  {#Thermometer-Events}

{:.events}

{:id="Thermometer.TemperatureChanged"} TemperatureChanged(*temperature*{:.number})
: Indicates a change of temperature, provided in degrees Celsius.

### Methods  {#Thermometer-Methods}

{:.methods}
None


## WifiSensor  {#WifiSensor}

Component for WifiSensor



### Properties  {#WifiSensor-Properties}

{:.properties}

{:id="WifiSensor.DefaultDuration" .number .ro .bo} *DefaultDuration*
: The default duration (in seconds) of each scan for this probe

{:id="WifiSensor.DefaultInterval" .number .ro .bo} *DefaultInterval*
: The default interval (in seconds) between each scan for this probe

{:id="WifiSensor.EnableSaveToDB" .boolean .wo .bo} *EnableSaveToDB*
: Set to indicate whether the returned sensor data should be saved to db automatically

{:id="WifiSensor.EnabledSchedule" .boolean .ro .bo} *EnabledSchedule*
: Indicates whether the user has specified that the sensor should listen
 for changes and raise the corresponding events.

{:id="WifiSensor.Frequency" .number .ro .bo} *Frequency*
: Returns the latest reading of the frequency of a wireless AP

{:id="WifiSensor.HideSensitiveData" .boolean} *HideSensitiveData*
: Indicates whether the returned values will be privacy safe (hashed) or not

{:id="WifiSensor.LocalDBName" .text .ro .bo} *LocalDBName*
: Return the name of the database in which the sensed data is stored 
 (actually there's only one db for sensed data (SensorData)

{:id="WifiSensor.SetScheduleInterval" .number .wo} *SetScheduleInterval*
: Set the length of the interval for a re-occurring probe activity. 
 Setting this value triggers the probe using the new schedule

### Events  {#WifiSensor-Events}

{:.events}

{:id="WifiSensor.WifiInfoReceived"} WifiInfoReceived(*timestamp*{:.number},*bssid*{:.text},*ssid*{:.text},*level*{:.number},*frequency*{:.number},*capabilities*{:.text})
: Indicates that the Wifi sensor info has been received.

{:id="WifiSensor.WifiScanComplete"} WifiScanComplete()
: Indicates that one round of scan has finish

### Methods  {#WifiSensor-Methods}

{:.methods}

{:id="WifiSensor.CreateNotification" class="method"} <i/> CreateNotification(*title*{:.text},*text*{:.text},*enabledSound*{:.boolean},*enabledVibrate*{:.boolean},*packageName*{:.text},*className*{:.text},*extraKey*{:.text},*extraVal*{:.text})
: Create a notication with message to wake up another activity when tap on the notification

{:id="WifiSensor.Enabled" class="method"} <i/> Enabled(*enabled*{:.boolean})
: Indicates whether the sensor should "run once" to listen for wifi access points information
 and raise the corresponding events.

{:id="WifiSensor.EnabledSchedule" class="method"} <i/> EnabledSchedule(*enabledSchedule*{:.boolean})
: Indicates whether the sensor should listen for proximity periodically and
 raise the corresponding events.

{:id="WifiSensor.ExportFolderPath" class="method returns text"} <i/> ExportFolderPath()
: Get the path of the foler to which sensor db are exported

{:id="WifiSensor.ExportSensorDB" class="method"} <i/> ExportSensorDB()
: Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
 csv file(s) or JSON file(s). Each type of sensor data in the database
 will be export it as one file.
 The export path is under SDcard/packageName/export/
