---
layout: documentation
title: Cloud Storage
---

[&laquo; Back to index](index.html)
# Cloud Storage

Table of Contents:

* [Dropbox](#Dropbox)
* [GoogleDrive](#GoogleDrive)

## Dropbox  {#Dropbox}

Component for sync data on App Inventor to Dropbox.



### Properties  {#Dropbox-Properties}

{:.properties}

{:id="Dropbox.AppKey" .text} *AppKey*
: AppKey property setter method: sets the AppKey to be used
 when authorizing with Dropbox via OAuth.

{:id="Dropbox.AppSecret" .text .wo} *AppSecret*
: AppSecret property setter method: sets the App Secret to be used
 when authorizing with Dropbox via OAuth.

{:id="Dropbox.DropboxUploadFolder" .text .bo} *DropboxUploadFolder*
: Return the Dropbox folder(directory) to where the uploadedfile will be placed

{:id="Dropbox.ScheduleUploadEnabled" .boolean .ro .bo} *ScheduleUploadEnabled*
: Indicates whether there exists any schedule upload task

{:id="Dropbox.UploadPeriod" .number .ro .bo} *UploadPeriod*
: Indicates the interval for a re-occurring upload activity for uploading file to dropbox

{:id="Dropbox.WifiOnly" .boolean} *WifiOnly*
: Indicates whether the user has specified that the DropBox could only 
 use Wifi

{:id="Dropbox.appSecret" .text .ro .bo} *appSecret*
: AppSecret property getter method.

### Events  {#Dropbox-Events}

{:.events}

{:id="Dropbox.IsAuthorized"} IsAuthorized()
: Indicates when the authorization has been successful.

{:id="Dropbox.ServiceStatusChanged"} ServiceStatusChanged(*successful*{:.boolean},*log*{:.text})
: This event is raised when upload service status has changed

{:id="Dropbox.UploadDone"} UploadDone(*successful*{:.boolean})
: Indicates when the authorization has been successful.

### Methods  {#Dropbox-Methods}

{:.methods}

{:id="Dropbox.Authorize" class="method"} <i/> Authorize()
: Start WebAuthentication session by logging in to Dropbox with a username and password.

{:id="Dropbox.CheckAuthorized" class="method returns boolean"} <i/> CheckAuthorized()
: Check whether we already have a Dropbox access token

{:id="Dropbox.DeAuthorize" class="method"} <i/> DeAuthorize()
: Remove authentication for this app instance

{:id="Dropbox.GetScheduleTaskLog" class="method returns text"} <i/> GetScheduleTaskLog()
: Get the message log of the most recent schedule upload task

{:id="Dropbox.GetScheduleTaskLogTime" class="method returns text"} <i/> GetScheduleTaskLogTime()
: Get the finshed datetime of last upload service task

{:id="Dropbox.GetScheduleTaskStatus" class="method returns boolean"} <i/> GetScheduleTaskStatus()
: Get the status of the status of the most recent schedule upload task

{:id="Dropbox.ScheduleUpload" class="method"} <i/> ScheduleUpload(*folderPath*{:.text},*period*{:.number})
: Enable upload scheduled task based on specified filepath of a folder locally in parameter <code>folderPath</code>. One use case is to upload all the save photosin some SD folder periodically. The parameter <code>period</code> is in second.

{:id="Dropbox.ScheduleUploadDB" class="method"} <i/> ScheduleUploadDB(*dbName*{:.text},*period*{:.number})
: Enable upload scheduled task based on specified filepath of a folder locally. One use case is to upload all the save photosin some SD folder periodically

{:id="Dropbox.StopScheduleUpload" class="method"} <i/> StopScheduleUpload()
: Stop the schedule uploading task

{:id="Dropbox.UploadDB" class="method"} <i/> UploadDB(*dbName*{:.text})
: Enable to upload the specified db file to remote storage place for backup. Will first archive the database. By default, if it's database name forsensor compoent, it should be "SensorData"

{:id="Dropbox.UploadData" class="method"} <i/> UploadData(*filepath*{:.text})
: This function uploads the file (as specified with its filepath) to dropbox folder.

## GoogleDrive  {#GoogleDrive}

Component for GoogleDrive



### Properties  {#GoogleDrive-Properties}

{:.properties}

{:id="GoogleDrive.GoogleDriveFolder" .text .bo} *GoogleDriveFolder*
: Return the Google Drive folder(directory) to where the uploadedfile(s) will be placed

{:id="GoogleDrive.WifiOnly" .boolean} *WifiOnly*
: Indicates whether the user has specified that the GoogleDrive component could only 
 use Wifi to upload data

### Events  {#GoogleDrive-Events}

{:.events}

{:id="GoogleDrive.IsAuthorized"} IsAuthorized()
: Indicates when the authorization has been successful.

{:id="GoogleDrive.ServiceStatusChanged"} ServiceStatusChanged(*target*{:.text},*successful*{:.boolean},*log*{:.text},*time*{:.text})
: This event is raised when background upload tasks status for a <target> has changed. If successful, <code>successful</code> will return true, else it returns false and <code>log</code> returns the error messages

{:id="GoogleDrive.UploadDone"} UploadDone(*successful*{:.boolean})
: Indicates when the upload task has been successful done.

### Methods  {#GoogleDrive-Methods}

{:.methods}

{:id="GoogleDrive.AddScheduledTask" class="method"} <i/> AddScheduledTask(*taskName*{:.text},*target*{:.text},*driveFolder*{:.text},*period*{:.number})
: Add a background task for uploading file(s) to Google Drive. The task will upload the file(s) that is specified in <code>target</code> to the Google drive folder specified in <code>driveFolder</code> with every <code>period</code> interval. Save <code>taskName</code> for later reference for removing the task

{:id="GoogleDrive.Authorize" class="method"} <i/> Authorize()
: Start OAuth2 Authentication to ask for user's permission for using Google Drive

{:id="GoogleDrive.CheckAuthorized" class="method returns boolean"} <i/> CheckAuthorized()
: Check whether we already have already the Google Drive access token

{:id="GoogleDrive.CopyFile" class="method"} <i/> CopyFile(*googleDocId*{:.text})
: Copy the public master app scripts

{:id="GoogleDrive.CreateFolder" class="method"} <i/> CreateFolder(*folderName*{:.text})
: Copy the public master app scripts

{:id="GoogleDrive.DeAuthorize" class="method"} <i/> DeAuthorize()
: Remove authentication for this app instance

{:id="GoogleDrive.GetAllUploadTasks" class="method returns list"} <i/> GetAllUploadTasks()
: Obtain all the names for current scheduled upload tasks

{:id="GoogleDrive.GetUploadTaskInfo" class="method returns list"} <i/> GetUploadTaskInfo(*taskName*{:.text})
: Obtain the information for a upload task that's running in the background.Will return a list containing the target, google drive folder, and period of the upload task. If the specified task does not exist then the return list will be empty

{:id="GoogleDrive.RemoveScheduledTask" class="method"} <i/> RemoveScheduledTask(*taskName*{:.text})
: Remove a background task for uploading files(s).

{:id="GoogleDrive.UploadData" class="method"} <i/> UploadData(*target*{:.text},*GoogleDriveFolder*{:.text})
: Upload the file(s)as specified <code>target</code> (can be the file path of a single file or a folder. Specify the destination folder in Google Drive in variable <code>GoogleDriveFolder</code>
