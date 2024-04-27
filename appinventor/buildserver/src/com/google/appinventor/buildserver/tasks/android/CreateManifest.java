// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2021-2023 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.buildserver.tasks.android;

import com.google.appinventor.buildserver.BuildType;
import com.google.appinventor.buildserver.Project;
import com.google.appinventor.buildserver.Signatures;
import com.google.appinventor.buildserver.TaskResult;
import com.google.appinventor.buildserver.context.AndroidCompilerContext;
import com.google.appinventor.buildserver.interfaces.AndroidTask;
import com.google.appinventor.buildserver.util.PermissionConstraint;

import com.google.appinventor.components.common.YaVersion;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * compiler.writeAndroidManifest()
 */
// CreateManifest
@BuildType(apk = true, aab = true)
public class CreateManifest implements AndroidTask {
  private static final String NEARFIELD_COMPONENT =
      "com.google.appinventor.components.runtime.NearField";
  private static final String GOOGLECLOUDMESSAGING_COMPONENT =
      "com.google.appinventor.components.runtime.GoogleCloudMessaging";
  private static final String PCTMESSAGING_COMPONENT =
      "com.google.appinventor.components.runtime.PctMessaging";
  private static final String GOOGLEMAP_COMPONENT =
      "com.google.appinventor.components.runtime.GoogleMap";

  @Override
  public TaskResult execute(AndroidCompilerContext context) {
    context.getPaths().setManifest(new File(context.getPaths().getBuildDir(),
        "AndroidManifest.xml"));

    // Create AndroidManifest.xml
    context.getReporter().info("Reading project specs...");
    String mainClass = context.getProject().getMainClass();
    String packageName = Signatures.getPackageName(mainClass);
    String className = Signatures.getClassName(mainClass);
    String projectName = context.getProject().getProjectName();
    String versionCode = context.getProject().getVCode();
    String versionName = cleanName(context.getProject().getVName());
    if (context.isIncludeDangerousPermissions()) {
      versionName += "u";
    }
    String appName = cleanName(context.getProject().getAName());
    String googleMapsApiKey = context.getProject().getMapsKey() == null ? "" : context.getProject().getMapsKey();
    context.getReporter().log("VCode: " + context.getProject().getVCode());
    context.getReporter().log("VName: " + context.getProject().getVName());

    // TODO(user): Use com.google.common.xml.XmlWriter
    try (Writer out = new BufferedWriter(new OutputStreamWriter(
        Files.newOutputStream(context.getPaths().getManifest().toPath()),
        StandardCharsets.UTF_8))) {
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      // TODO(markf) Allow users to set versionCode and versionName attributes.
      // See http://developer.android.com/guide/publishing/publishing.html for
      // more info.
      out.write("<manifest "
          + "xmlns:android=\"http://schemas.android.com/apk/res/android\" "
          + "package=\"" + packageName + "\" "
          + "android:versionCode=\"" + versionCode + "\" "
          + "android:versionName=\"" + versionName + "\" "
          + ">\n");

      // If we are building the Wireless Debugger (AppInventorDebugger) add the uses-feature tag
      // which is used by the Google Play store to determine which devices the app is available
      // for. By adding these lines we indicate that we use these features BUT THAT THEY ARE NOT
      // REQUIRED so it is ok to make the app available on devices that lack the feature. Without
      // these lines the Play Store makes a guess based on permissions and assumes that they are
      // required features.
      if (context.isForCompanion()) {
        out.write("  <uses-feature android:name=\"android.hardware.bluetooth\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.location\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.telephony\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.location.network\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.location.gps\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.microphone\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.touchscreen\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.camera\" "
            + "android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.camera.autofocus\" "
            + "android:required=\"false\" />\n");
        if (context.isForEmulator()) {
          out.write("  <uses-feature android:name=\"android.hardware.wifi\" "
              + "android:required=\"false\" />\n"); // We actually require wifi
        } else {
          // We actually require wifi
          out.write("  <uses-feature android:name=\"android.hardware.wifi\" />\n");
        }
      }

      final Map<String, Set<String>> queriesNeeded = context.getComponentInfo().getQueriesNeeded();
      if (!queriesNeeded.isEmpty()) {
        out.write("  <queries>\n");
        for (Map.Entry<String, Set<String>> componentSubElSetPair : queriesNeeded.entrySet()) {
          Set<String> subelementSet = componentSubElSetPair.getValue();
          for (String subelement : subelementSet) {
            // replace %packageName% with the actual packageName
            out.write(subelement.replace("%packageName%", packageName));
          }
        }
        out.write("  </queries>\n");
      }

      int minSdk = AndroidBuildUtils.computeMinSdk(context);
      context.getReporter().log("Min SDK " + minSdk);

      // make permissions unique by putting them in one set
      Set<String> permissions = Sets.newHashSet();
      for (Set<String> compPermissions :
          context.getComponentInfo().getPermissionsNeeded().values()) {
        permissions.addAll(compPermissions);
      }
      if (context.usesLegacyFileAccess() || context.usesSharedFileAccess()) {
        permissions.add("android.permission.READ_EXTERNAL_STORAGE");
        permissions.add("android.permission.WRITE_EXTERNAL_STORAGE");
      }
      if (context.isForCompanion() || context.usesSharedFileAccess()) {
        permissions.add("android.permission.READ_MEDIA_AUDIO");
        permissions.add("android.permission.READ_MEDIA_IMAGES");
        permissions.add("android.permission.READ_MEDIA_VIDEO");
      }

      // Remove Google's Forbidden Permissions
      // This code is crude because we had to do this on short notice
      // List of permissions taken from
      // https://support.google.com/googleplay/android-developer/answer/9047303#intended
      if (context.isForCompanion() && !context.isIncludeDangerousPermissions()) {
        // Default SMS handler
        permissions.remove("android.permission.READ_SMS");
        permissions.remove("android.permission.RECEIVE_MMS");
        permissions.remove("android.permission.RECEIVE_SMS");
        permissions.remove("android.permission.RECEIVE_WAP_PUSH");
        permissions.remove("android.permission.SEND_SMS");
        permissions.remove("android.permission.WRITE_SMS");
        // Default Phone handler
        permissions.remove("android.permission.PROCESS_OUTGOING_CALLS");
        permissions.remove("android.permission.CALL_PHONE");
        permissions.remove("android.permission.READ_CALL_LOG");
        permissions.remove("android.permission.WRITE_CALL_LOG");
      }

      Multimap<String, PermissionConstraint<?>> permissionConstraints = HashMultimap.create();
      for (Map<String, Set<PermissionConstraint<?>>> constraints :
          context.getComponentInfo().getPermissionConstraintsNeeded().values()) {
        for (Map.Entry<String, Set<PermissionConstraint<?>>> entry : constraints.entrySet()) {
          permissionConstraints.putAll(entry.getKey(), entry.getValue());
        }
      }

      for (String permission : permissions) {
        if ("android.permission.WRITE_EXTERNAL_STORAGE".equals(permission)) {
          out.write("  <uses-permission android:name=\"" + permission + "\"");

          // we don't need these permissions post KitKat, but we do need them for the companion
          if (!context.isForCompanion() && !context.usesLegacyFileAccess() && minSdk < 29) {
            out.write(" android:maxSdkVersion=\"29\"");
          }

        } else {
          out.write("  <uses-permission android:name=\""
              // replace %packageName% with the actual packageName
              + permission.replace("%packageName%", packageName)
              + "\"");
        }
        outputPermissionConstraints(out, permissionConstraints, permission);
        out.write(" />");
      }

      // Google Cloud Messaging
      if (context.getSimpleCompTypes().contains(GOOGLECLOUDMESSAGING_COMPONENT)
          || context.getSimpleCompTypes().contains(PCTMESSAGING_COMPONENT)) {
        out.write("<permission android:name=\"com.google.appinventor.aiphoneapp.permission.C2D_MESSAGE\" android:protectionLevel=\"signature\" />\n");
        out.write("<uses-permission android:name=\"com.google.appinventor.aiphoneapp.permission.C2D_MESSAGE\" />\n");
        out.write("<permission android:name=\"" + packageName + ".permission.C2D_MESSAGE\" android:protectionLevel=\"signature\" />\n");
        out.write("<uses-permission android:name=\"" + packageName + ".permission.C2D_MESSAGE\" />\n");
      }
      // add permission, and uses-permission, uses-feature specifically for Google Map
      // as stated here https://developers.google.com/maps/documentation/android/start
      if (context.getSimpleCompTypes().contains(GOOGLEMAP_COMPONENT)) {
        out.write(" <permission ");
        out.write(" android:name=\"" + packageName +".permission.MAPS_RECEIVE\" ");
        out.write(" android:protectionLevel=\"signature\" /> \n");
        out.write(" <uses-permission android:name=\"" + packageName + ".permission.MAPS_RECEIVE\" />\n");
        out.write(" <uses-feature android:glEsVersion=\"0x00020000\" android:required=\"true\" />\n");
      }

      if (context.isForCompanion()) {
        // This is so ACRA can do a logcat on phones older then Jelly Bean
        out.write("  <uses-permission android:name=\"android.permission.READ_LOGS\" />\n");
      }

      // TODO(markf): Change the minSdkVersion below if we ever require an SDK beyond 1.5.
      // The market will use the following to filter apps shown to devices that don't support
      // the specified SDK version.  We right now support building for minSDK 4.
      // We might also want to allow users to specify minSdk version or targetSDK version.
      out.write("  <uses-sdk android:minSdkVersion=\"" + minSdk + "\" android:targetSdkVersion=\""
          + YaVersion.TARGET_SDK_VERSION + "\" />\n");

      out.write("  <application ");

      // TODO(markf): The preparing to publish doc at
      // http://developer.android.com/guide/publishing/preparing.html suggests removing the
      // 'debuggable=true' but I'm not sure that our users would want that while they're still
      // testing their packaged apps.  Maybe we should make that an option, somehow.
      // TODONE(jis): Turned off debuggable. No one really uses it and it represents a security
      // risk for App Inventor App end-users.
      out.write("android:debuggable=\"false\" ");
      // out.write("android:debuggable=\"true\" "); // DEBUGGING
      if (appName.isEmpty()) {
        out.write("android:label=\"" + projectName + "\" ");
      } else {
        out.write("android:label=\"" + appName + "\" ");
      }
      out.write("android:networkSecurityConfig=\"@xml/network_security_config\" ");
      out.write("android:requestLegacyExternalStorage=\"true\" ");  // For SDK 29 (Android Q)
      if (YaVersion.TARGET_SDK_VERSION >= 30) {
        out.write("android:preserveLegacyExternalStorage=\"true\" ");  // For SDK 30 (Android R)
      }
      out.write("android:icon=\"@mipmap/ic_launcher\" ");
      out.write("android:roundIcon=\"@mipmap/ic_launcher\" ");
      if (context.isForCompanion()) {              // This is to hook into ACRA
        out.write("android:name=\"com.google.appinventor.components.runtime.ReplApplication\" ");
      } else {
        out.write("android:name="
            + "\"com.google.appinventor.components.runtime.multidex.MultiDexApplication\" ");
      }
      // Write theme info if we are not using the "Classic" theme (i.e., no theme)
      //      if (!"Classic".equalsIgnoreCase(project.getTheme())) {
      out.write("android:theme=\"@style/AppTheme\" ");
      out.write(">\n");

      out.write("<uses-library android:name=\"org.apache.http.legacy\" "
          + "android:required=\"false\" />");

      for (Project.SourceDescriptor source : context.getProject().getSources()) {
        String formClassName = source.getQualifiedName();
        context.getReporter().info("Writing screen '" + formClassName + "'");
        // String screenName = formClassName.substring(formClassName.lastIndexOf('.') + 1);
        boolean isMain = formClassName.equals(mainClass);

        if (isMain) {
          // The main activity of the application.
          out.write("    <activity android:name=\"." + className + "\" ");
        } else {
          // A secondary activity of the application.
          out.write("    <activity android:name=\"" + formClassName + "\" ");
        }

        // This line is here for NearField and NFC.   It keeps the activity from
        // restarting every time NDEF_DISCOVERED is signaled.
        // TODO:  Check that this doesn't screw up other components.  Also, it might be
        // better to do this programmatically when the NearField component is created, rather
        // than here in the manifest.
        if (context.getSimpleCompTypes().contains(NEARFIELD_COMPONENT)
            && !context.isForCompanion() && isMain) {
          out.write("android:launchMode=\"singleTask\" ");
        } else if (isMain && context.isForCompanion()) {
          out.write("android:launchMode=\"singleTop\" ");
        }
        // The line below is required for Android 12+
        out.write("android:exported=\"true\" ");
        out.write("android:screenOrientation=\"");
        out.write(context.getFormOrientations().get(source.getSimpleName()));
        out.write("\" ");

        out.write("android:windowSoftInputMode=\"stateHidden\" ");

        // The keyboard option prevents the app from stopping when a external (bluetooth)
        // keyboard is attached.
        out.write("android:configChanges=\"orientation|screenSize|keyboardHidden|keyboard|"
            + "screenLayout|smallestScreenSize\">\n");

        out.write("      <intent-filter>\n");
        out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
        if (isMain) {
          out.write("        <category android:name=\"android.intent.category.LAUNCHER\" />\n");
        }
        out.write("      </intent-filter>\n");
        if (context.isForCompanion()) {
          out.write("<intent-filter>\n");
          out.write("<action android:name=\"android.intent.action.VIEW\" />\n");
          out.write("<category android:name=\"android.intent.category.DEFAULT\" />\n");
          out.write("<category android:name=\"android.intent.category.BROWSABLE\" />\n");
          out.write("<data android:scheme=\"aicompanion\" android:host=\"comp\" />\n");
          out.write("</intent-filter>\n");
        }

        if (context.getSimpleCompTypes().contains(NEARFIELD_COMPONENT)
            && !context.isForCompanion() && isMain) {
          //  make the form respond to NDEF_DISCOVERED
          //  this will trigger the form's onResume method
          //  For now, we're handling text/plain only,but we can add more and make the Nearfield
          // component check the type.
          out.write("      <intent-filter>\n");
          out.write("        <action android:name=\"android.nfc.action.NDEF_DISCOVERED\" />\n");
          out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
          out.write("        <data android:mimeType=\"text/plain\" />\n");
          out.write("      </intent-filter>\n");
        }
        out.write("    </activity>\n");

        Set<Map.Entry<String, Set<String>>> metadataElements =
            context.getComponentInfo().getActivityMetadataNeeded().entrySet();

        // If any component needs to register additional activity metadata,
        // insert them into the manifest here.
        if (!metadataElements.isEmpty()) {
          for (Map.Entry<String, Set<String>> metadataElementSetPair : metadataElements) {
            Set<String> metadataElementSet = metadataElementSetPair.getValue();
            for (String metadataElement : metadataElementSet) {
              out.write(
                  // replace %packageName% with the actual packageName
                  metadataElement
                      .replace("%packageName%", packageName)
              );
            }
          }
        }

        // Companion display a splash screen... define it's activity here
        if (isMain && context.isForCompanion()) {
          out.write("    <activity ");
          out.write("android:name=\"com.google.appinventor.components.runtime.SplashActivity\" ");
          out.write("android:exported=\"false\" ");
          out.write("android:screenOrientation=\"behind\" ");
          out.write("android:configChanges=\"keyboardHidden|orientation\">\n");
          out.write("      <intent-filter>\n");
          out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
          out.write("      </intent-filter>\n");
          out.write("    </activity>\n");
        }
      }

      // Add the Google Cloud Messaging service
      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.GoogleCloudMessaging")
          || context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.PctMessaging")) {
        // Declare and use a custom permission so only this application can receive GCM messages:
        out.write("<service android:name=\"com.google.appinventor.components.runtime.GCMIntentService\"></service>\n");
        out.write("<receiver android:name=\"com.google.appinventor.components.runtime.GCMBroadcastReceiver\" android:permission=\"com.google.android.c2dm.permission.SEND\" android:exported=\"true\">\n");
        out.write("    <intent-filter>");
        out.write("        <action android:name=\"com.google.android.c2dm.intent.RECEIVE\" />\n");
        out.write("        <action android:name=\"com.google.android.c2dm.intent.REGISTRATION\" />\n");
        out.write("        <category android:name=\""+packageName+"\" />\n");
        out.write("    </intent-filter>");
        out.write("</receiver>");
      }

      //add UploadServices and DataBaseService
      out.write("<service android:name=\"edu.mit.media.funf.storage.NameValueDatabaseService\"></service> \n");
      out.write("<service android:name=\"com.google.appinventor.components.runtime.util.HttpsUploadService\"></service> \n");
      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.Dropbox")) {
        out.write("<service android:name=\"com.google.appinventor.components.runtime.DropboxUploadService\"></service> \n");
      }
      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.GoogleDrive")) {
        out.write("<service android:name=\"com.google.appinventor.components.runtime.GoogleDriveUploadService\"></service> \n");
      }

      // try the same thing here for TimerManager (Disabled for now)
      // BroadcastReceiver for TimerManager
      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.Timer")) {
        out.write("<service android:name=\"com.google.appinventor.components.runtime.util.TimerManager\" android:enabled=\"true\" android:exported=\"false\">\n");
        out.write(" </service>\n");
        out.write("<receiver android:name=\"com.google.appinventor.components.runtime.util.TimerLauncher\" android:enabled=\"true\" android:exported=\"true\">\n");
        out.write("    <intent-filter>\n");
        out.write("        <action android:name=\"android.intent.action.BATTERY_CHANGED\" />\n");
        out.write("        <action android:name=\"android.intent.action.BOOT_COMPLETED\" />\n");
        out.write("        <action android:name=\"android.intent.action.DOCK_EVENT\" />\n");
        out.write("        <action android:name=\"android.intent.action.ACTION_SCREEN_ON\" />\n");
        out.write("        <action android:name=\"android.intent.action.USER_PRESENT\" />\n");
        out.write("    </intent-filter>\n");
        out.write("</receiver>");
      }

      // Add the FUNF probe services
      // out.write("<service android:name=\"edu.mit.media.funf.probe.builtin.BatteryProbe\"></service>\n");
      // out.write("<service android:name=\"edu.mit.media.funf.probe.builtin.MagneticFieldSensorProbe\"></service>\n");
      // out.write("<service android:name=\"edu.mit.media.funf.probe.builtin.ProximitySensorProbe\"></service>\n");
      // out.write("<service android:name=\"edu.mit.media.funf.probe.builtin.BluetoothProbe\"></service>\n");

      // new version of configurations to include in the manifest.xml. Now need not include each probe individually,
      // but just include the FunfManager service
      // Broadcast receiver for all funf related component
      for (String type : context.getSimpleCompTypes()) {
        Set<String> libraries = context.getComponentInfo().getLibsNeeded().get(type);
        if (libraries != null && libraries.contains("funf.jar")) {
          out.write("<service android:name=\"edu.mit.media.funf.FunfManager\" android:enabled=\"true\" android:exported=\"false\">\n");
          out.write(" </service>\n");
          out.write("<receiver android:name=\"edu.mit.media.funf.Launcher\" android:enabled=\"true\" android:exported=\"true\">\n");
          out.write("    <intent-filter>\n");
          out.write("        <action android:name=\"android.intent.action.BATTERY_CHANGED\" />\n");
          out.write("        <action android:name=\"android.intent.action.BOOT_COMPLETED\" />\n");
          out.write("        <action android:name=\"android.intent.action.DOCK_EVENT\" />\n");
          out.write("        <action android:name=\"android.intent.action.ACTION_SCREEN_ON\" />\n");
          out.write("        <action android:name=\"android.intent.action.USER_PRESENT\" />\n");
          out.write("    </intent-filter>\n");
          out.write("</receiver>\n");
          //add UploadServices and DataBaseService
          out.write("<service android:name=\"edu.mit.media.funf.storage.NameValueDatabaseService\"></service> \n");
          out.write("<service android:name=\"com.google.appinventor.components.runtime.util.HttpsUploadService\"></service> \n");
          break;
        }
      }

      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.Dropbox")) {
        out.write("<service android:name=\"com.google.appinventor.components.runtime.DropboxUploadService\"></service> \n");
      }
      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.GoogleDrive")) {
        out.write("<service android:name=\"com.google.appinventor.components.runtime.GoogleDriveUploadService\"></service> \n");
      }

      // try the same thing here for TimerManager (Disabled for now)
      // BroadcastReceiver for TimerManager
      if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.Timer")) {
        out.write("<service android:name=\"com.google.appinventor.components.runtime.util.TimerManager\" android:enabled=\"true\" android:exported=\"false\">\n");
        out.write(" </service>\n");
        out.write("<receiver android:name=\"com.google.appinventor.components.runtime.util.TimerLauncher\" android:enabled=\"true\" android:exported=\"true\">\n");
        out.write("    <intent-filter>\n");
        out.write("        <action android:name=\"android.intent.action.BATTERY_CHANGED\" />\n");
        out.write("        <action android:name=\"android.intent.action.BOOT_COMPLETED\" />\n");
        out.write("        <action android:name=\"android.intent.action.DOCK_EVENT\" />\n");
        out.write("        <action android:name=\"android.intent.action.ACTION_SCREEN_ON\" />\n");
        out.write("        <action android:name=\"android.intent.action.USER_PRESENT\" />\n");
        out.write("    </intent-filter>\n");
        out.write("</receiver>");
      }

      // Collect any additional <application> subelements into a single set.
      Set<Map.Entry<String, Set<String>>> subelements = Sets.newHashSet();
      subelements.addAll(context.getComponentInfo().getActivitiesNeeded().entrySet());
      subelements.addAll(context.getComponentInfo().getBroadcastReceiversNeeded().entrySet());
      subelements.addAll(context.getComponentInfo().getContentProvidersNeeded().entrySet());
      subelements.addAll(context.getComponentInfo().getMetadataNeeded().entrySet());
      subelements.addAll(context.getComponentInfo().getServicesNeeded().entrySet());


      // If any component needs to register additional activities,
      // broadcast receivers, services or content providers, insert
      // them into the manifest here.
      if (!subelements.isEmpty()) {
        for (Map.Entry<String, Set<String>> componentSubElSetPair : subelements) {
          Set<String> subelementSet = componentSubElSetPair.getValue();
          for (String subelement : subelementSet) {
            if (context.isForCompanion() && !context.isIncludeDangerousPermissions()
                && subelement.contains("android.provider.Telephony.SMS_RECEIVED")) {
              continue;
            }
            out.write(
                // replace %packageName% with the actual packageName
                subelement
                    .replace("%packageName%", packageName)
            );
          }
        }
      }

      // TODO(Will): Remove the following legacy code once the deprecated
      //             @SimpleBroadcastReceiver annotation is removed. It should
      //             should remain for the time being because otherwise we'll break
      //             extensions currently using @SimpleBroadcastReceiver.

      // Collect any legacy simple broadcast receivers
      Set<String> simpleBroadcastReceivers = Sets.newHashSet();
      for (String componentType :
          context.getComponentInfo().getComponentBroadcastReceiver().keySet()) {
        simpleBroadcastReceivers.addAll(
            context.getComponentInfo().getComponentBroadcastReceiver().get(componentType));
      }

      // The format for each legacy Broadcast Receiver in simpleBroadcastReceivers is
      // "className,Action1,Action2,..." where the class name is mandatory, and
      // actions are optional (and as many as needed).
      for (String broadcastReceiver : simpleBroadcastReceivers) {
        String[] brNameAndActions = broadcastReceiver.split(",");
        if (brNameAndActions.length == 0) {
          continue;
        }
        // Remove the SMS_RECEIVED broadcast receiver if we aren't including dangerous permissions
        if (context.isForCompanion() && !context.isIncludeDangerousPermissions()) {
          boolean skip = false;
          for (String action : brNameAndActions) {
            if (action.equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED")) {
              skip = true;
              break;
            }
          }
          if (skip) {
            continue;
          }
        }
        out.write(
            "<receiver android:name=\"" + brNameAndActions[0] + "\" android:exported=\"true\">\n");
        if (brNameAndActions.length > 1) {
          out.write("  <intent-filter>\n");
          for (int i = 1; i < brNameAndActions.length; i++) {
            out.write("    <action android:name=\"" + brNameAndActions[i] + "\" />\n");
          }
          out.write("  </intent-filter>\n");
        }
        out.write("</receiver> \n");
      }
      // adds the google maps v2 api key
      if (context.getSimpleCompTypes().contains(GOOGLEMAP_COMPONENT)) {
        System.out.println("Android Manifest: including Google Maps key:" + googleMapsApiKey);
        out.write("<meta-data android:name=\"com.google.android.maps.v2.API_KEY\" ");
        out.write("android:value=\"" + googleMapsApiKey + "\"/>");
      }

      // Add the FileProvider because in Sdk >=24 we cannot pass file:
      // URLs in intents (and in other contexts)

      out.write("      <provider\n");
      out.write("         android:name=\"androidx.core.content.FileProvider\"\n");
      out.write("         android:authorities=\"" + packageName + ".provider\"\n");
      out.write("         android:exported=\"false\"\n");
      out.write("         android:grantUriPermissions=\"true\">\n");
      out.write("         <meta-data\n");
      out.write("            android:name=\"android.support.FILE_PROVIDER_PATHS\"\n");
      out.write("            android:resource=\"@xml/provider_paths\"/>\n");
      out.write("      </provider>\n");

      out.write("  </application>\n");
      out.write("</manifest>\n");
      out.close();
    } catch (IOException e) {
      return TaskResult.generateError(e);
    }

    return TaskResult.generateSuccess();
  }

  private void outputPermissionConstraints(Writer out,
      Multimap<String, PermissionConstraint<?>> permissionConstraints, String permission)
      throws IOException {
    Collection<PermissionConstraint<?>> constraints = permissionConstraints.get(permission);
    if (constraints == null) {
      return;
    }

    Multimap<String, PermissionConstraint<?>> aggregates = HashMultimap.create();
    for (PermissionConstraint<?> constraint : constraints) {
      aggregates.put(constraint.getAttribute(), constraint);
    }

    for (Map.Entry<String, Collection<PermissionConstraint<?>>> entry :
        aggregates.asMap().entrySet()) {
      String attribute = entry.getKey();
      // TODO(ewpatton): Figure out a more generic way of doing this.
      String value;
      if ("maxSdkVersion".equals(attribute)) {
        PermissionConstraint.Reducer<Integer> reducer = new PermissionConstraint.MaxReducer();
        for (PermissionConstraint<?> constraint : entry.getValue()) {
          constraint.as(Integer.class).apply(reducer);
        }
        value = reducer.toString();
      } else if ("usesPermissionFlags".equals(attribute)) {
        PermissionConstraint.Reducer<String> reducer = new PermissionConstraint.UnionReducer<>();
        for (PermissionConstraint<?> constraint : entry.getValue()) {
          constraint.as(String.class).apply(reducer);
        }
        value = reducer.toString();
      } else {
        throw new IllegalArgumentException("Unrecognized permission constraint: " + attribute);
      }
      out.write(" android:" + attribute + "=\"" + value + "\"");
    }
  }

  private String cleanName(String name) {
    return name.replace("&", "and");
  }
}
