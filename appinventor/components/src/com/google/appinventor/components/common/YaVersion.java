// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.common;

/**
 * Contains constants related to Young Android system, blocks, and component
 * version numbers.
 *
 */
public class YaVersion {
  private YaVersion() {
  }

  // ............................ Young Android System Version Number .............................

  // YOUNG_ANDROID_VERSION must be incremented when either the blocks language or a component
  // changes.
  // TODO(lizlooney) - should this version number be generated so that it is automatically
  // incremented when the blocks language or a component changes?

  // For YOUNG_ANDROID_VERSION 2:
  // - The Logger component was removed. The Notifier component should be used instead.
  // - TINYWEBDB_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 3:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 4:
  // - The LegoNxtConnection component was added.
  // For YOUNG_ANDROID_VERSION 5:
  // - The Camera component was added.
  // For YOUNG_ANDROID_VERSION 6:
  // - FORM_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 7:
  // - The Bluetooth component was added.
  // For YOUNG_ANDROID_VERSION 8:
  // - PLAYER_COMPONENT_VERSION was incremented to 2.
  // - SOUND_COMPONENT_VERSION was incremented to 2.
  // - VIDEOPLAYER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 9:
  // - The LegoNxtConnection component was removed without backwards compatibility.
  // - The LegoMindstormsNxtDirect component was added.
  // - The LegoMindstormsNxtDrive component was added.
  // - The Bluetooth component was removed without backwards compatibility.
  // - The BluetoothClient component was added.
  // - The BluetoothServer component was added.
  // For YOUNG_ANDROID_VERSION 10:
  // - ACTIVITYSTARTER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 11:
  // - BLUETOOTHCLIENT_COMPONENT_VERSION was incremented to 2.
  // - BLUETOOTHSERVER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION_12:
  // - TWITTER_COMPONENT_VERSION was incremented to 2.
  // - The Twitter component was changed to support OAuth authentication.
  // For YOUNG_ANDROID_VERSION 13:
  // - The LegoMindstormsNxtTouchSensor component was added.
  // - The LegoMindstormsNxtLightSensor component was added.
  // - The LegoMindstormsNxtSoundSensor component was added.
  // - The LegoMindstormsNxtUltrasonicSensor component was added.
  // For YOUNG_ANDROID_VERSION 14:
  // - LegoMindstormsNXT* components were renamed.
  // For YOUNG_ANDROID_VERSION 15:
  // - TEXTBOX_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 16:
  // - FORM_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 17:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 18:
  // - ACTIVITYSTARTER_COMPONENT_VERSION was incremented to 3.
  // - BLUETOOTHCLIENT_COMPONENT_VERSION was incremented to 3.
  // - BLUETOOTHSERVER_COMPONENT_VERSION was incremented to 3.
  // - FORM_COMPONENT_VERSION was incremented to 4.
  // - PLAYER_COMPONENT_VERSION was incremented to 3.
  // - SOUND_COMPONENT_VERSION was incremented to 3.
  // - VIDEOPLAYER_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 19:
  // - The NxtColorSensor component was added.
  // For YOUNG_ANDROID_VERSION 20:
  // - The SoundRecorder component was added.
  // For YOUNG_ANDROID_VERSION 21:
  // - BUTTON_COMPONENT_VERSION was incremented to 2.
  // - CHECKBOX_COMPONENT_VERSION was incremented to 2.
  // - CONTACTPICKER_COMPONENT_VERSION was incremented to 2.
  // - EMAILPICKER_COMPONENT_VERSION was incremented to 2.
  // - IMAGEPICKER_COMPONENT_VERSION was incremented to 2.
  // - LABEL_COMPONENT_VERSION was incremented to 2.
  // - LISTPICKER_COMPONENT_VERSION was incremented to 2.
  // - PASSWORDTEXTBOX_COMPONENT_VERSION was incremented to 2.
  // - PHONENUMBERPICKER_COMPONENT_VERSION was incremented to 2.
  // - TEXTBOX_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 22:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 4.
  // - CANVAS_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 23:
  // - IMAGESPRITE_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 24:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 25:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 6.
  // For YOUNG_ANDROID_VERSION 26:
  // - In .scm files, values for asset, BluetoothClient, component, lego_nxt_sensor_port,
  // and string properties no longer contain leading and trailing quotes.
  // For YOUNG_ANDROID_VERSION 27:
  // - BLUETOOTHCLIENT_COMPONENT_VERSION was incremented to 4.
  // - BLUETOOTHSERVER_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 28:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 7.
  // For YOUNG_ANDROID_VERSION 29:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 8.
  // For YOUNG_ANDROID_VERSION 30:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 9.
  // For YOUNG_ANDROID_VERSION 31:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 10.
  // For YOUNG_ANDROID_VERSION 32:
  // - LISTPICKER_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 33:
  // - CANVAS_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 34:
  // - IMAGESPRITE_COMPONENT_VERSION was incremented to 3.
  // - BALL_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 35:
  // - FORM_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 36:
  // - FusiontablesControl component was added
  // - BLOCKS_LANGUAGE_VERSION was incremented to 11 (CSV-related list functions)
  // For YOUNG_ANDROID_VERSION 37:
  // - CANVAS_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 38:
  // - CONTACTPICKER_COMPONENT_VERSION was incremented to 3.
  // - IMAGEPICKER_COMPONENT_VERSION was incremented to 3.
  // - LISTPICKER_COMPONENT_VERSION was incremented to 4.
  // - PHONENUMBERPICKER_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 39:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 12
  // For YOUNG_ANDROID_VERSION 40:
  // - BUTTON_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 41:
  // - FORM_COMPONENT_VERSION was incremented to 6.
  // - BLOCKS_LANGUAGE_VERSION was incremented to 13
  // For YOUNG_ANDROID_VERSION 42:
  // - The Web component was added.
  // For YOUNG_ANDROID_VERSION 43:
  // - BALL_COMPONENT_VERSION was incremented to 3.
  // - IMAGESPRITE_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 44:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 14
  // For YOUNG_ANDROID_VERSION 45:
  // - ORIENTATIONSENSOR_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 46:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 15.
  // For YOUNG_ANDROID_VERSION 47:
  // - WebViewer component was added
  // For YOUNG_ANDROID_VERSION 48:
  // - WEB_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 49:
  // - WEBVIEWER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 50:
  // - TEXTBOX_COMPONENT_VERSION was incremented to 4:
  // For YOUNG_ANDROID_VERSION 51:
  // - CANVAS_VERSION was incremented to 5.
  // - BLOCKS_LANGUAGE_VERSION was incremented to 16.
  // For YOUNG_ANDROID_VERSION 52:
  // - BLOCKS_LANGUAGE_VERSION was incremented to 17.
  // For YOUNG_ANDROID_VERSION 53:
  // - BLUETOOTHCLIENT_COMPONENT_VERSION was incremented to 5.
  // - BLUETOOTHSERVER_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 54:
  // - BUTTON_COMPONENT_VERSION was incremented to 4.
  // - CONTACTPICKER_COMPONENT_VERSION was incremented to 4.
  // - IMAGEPICKER_COMPONENT_VERSION was incremented to 4.
  // - LISTPICKER_COMPONENT_VERSION was incremented to 5.
  // - PHONENUMBERPICKER_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 55:
  // - ACCELEROMETERSENSOR_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 56
  // - LOCATIONSENSOR_COMPONENT_VERSION was incremented to 2
  // For YOUNG_ANDROID_VERSION 57:
  // - PLAYER_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 58:
  // - FORM_COMPONENT_VERSION was incremented to 7.
  // For YOUNG_ANDROID_VERION 59:
  // - The Camcorder component was added.
  // For YOUNG_ANDROID_VERSION 60:
  // - VIDEOPLAYER_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 61:
  // - HORIZONTALARRANGEMENT_COMPONENT_VERSION was incremented to 2
  // - VERTICALARRANGEMENT_COMPONENT_VERSION was incremented to 2
  // - FORM_COMPONENT_VERSION was incremented to 8
  // For YOUNG_ANDROID_VERSION 62:
  // - BALL_COMPONENT_VERSION was incremented to 4.
  // - CANVAS_COMPONENT_VERSION was incremented to 6.
  // - IMAGESPRITE_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 63:
  // - ACTIVITYSTARTER_COMPONENT_VERSION was incremented to 4.
  // - FORM_COMPONENT_VERSION was incremented to 9.
  // - LISTPICKER_COMPONENT_VERSION was incremented to 6.
  // For YOUNG_ANDROID_VERSION 64:
  // - FUSIONTABLESCONTROL_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 65:
  // - BALL_COMPONENT_VERSION was incremented to 5.
  // - CANVAS_COMPONENT_VERSION was incremented to 7.
  // - IMAGESPRITE_COMPONENT_VERSION was incremented to 6.
  // For YOUNG_ANDROID_VERSION 66:
  // - FORM_COMPONENT_VERSION was incremented to 10.
  // For YOUNG_ANDROID_VERSION 67:
  // - TEXTING_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 68:
  // - Phone Status Block was added.
  // For YOUNG_ANDROID_VERSION 69:
  // - IMAGEPICKER_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 70:
  // - TEXTING_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 71:
  // - NOTIFIER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 72:
  // - WEBVIEWER_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 73:
  // - BUTTON_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 74:
  // - SLIDER_COMPONENT_VERSION was incremented to 1.
  // For YOUNG_ANDROID_VERSION 75:
  // - WEBVIEWER_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 76:
  // - PLAYER_COMPONENT_VERSION was incremented to 5
  // For YOUNG_ANDROID_VERSION 77:
  // - TWITTER_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 78:
  // - NEARFIELD_COMPONENT_VERSION was incremented to 1
  // For YOUNG_ANDROID_VERSION 79:
  // - GOOGLECLOUDMESSAGING_COMPONENT_VERSION was incremented to 1
  // For YOUNG_ANDROID_VERSION 80:
  // - LISTPICKER_COMPONENT_VERSION was incremented to 7.
  // For YOUNG_ANDROID_VERSION 81:
  // - NOTIFIER_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 82:
  // - ACCELEROMETERSENSOR_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 83:
  // - LISTPICKER_COMPONENT_VERSION was incremented to 8.
  // - FORM_COMPONENT_VERSION was incremented to 11.
  // For YOUNG_ANDROID_VERSION 84;
  // - The Google Map component was added
  // For YOUNG_ANDROID_VERSION 85;
  // - The Wifi Sensor component was added
  // For YOUNG_ANDROID_VERSION 86;
  // - The proximity sensor component was added
  // For YOUNG_ANDROID_VERSION 87;
  // - The social proximity sensor component was added
  // For YOUNG_ANDROID_VERSION 88;
  // - The activity probe sensor component was added
  // For YOUNG_ANDROID_VERSION 89;
  // - The batter probe sensor component was added
  // For YOUNG_ANDROID_VERSION 90;
  // - The calllog probe component was added
  // For YOUNG_ANDROID_VERSION 91;
  // - The celltower probe component was added
  // FOR YOUNG_ANDROID_VERSION 91;
  // - The light sensor component was added
  // FOR YOUNG_ANDROID_VERSION 93;
  // - The location probe sensor component was added
  // FOR YOUNG_ANDROID_VERSION 94;
  // - THe pedometer sensor component was added
  // FOR YOUNG_ANDROID_VERSION 95;
  // - The Running application probe component was added
  // FOR YOUNG_ANDROID_VERSION 96;
  // - The Screen probe component was added
  // FOR YOUNG_ANDROID_VRESION 97;
  // - The SMS log probe component was added
  // FOR YOUNG_ANDROID_VERSION 98
  // - The Telephony probe component was added
  // FOR YOUNG_ANDROID_VERSION 99
  // - The Timer component was added
  // FOR YOUNG_ANDROID_VERSION 100
  // - The Dropbox component was added
  // FOR YOUNG_ANDROID_VERSION 101
  // - The SensorDB component was added
  // FOR YOUNG_ANDROID_VERSION 102
  // - The Survey component was added
  // FOR YOUNG_ANDROID_VERSION 103
  // - The Google Drive component was added
  // FOR YOUNG_ANDROID_VERSION 104
  // - The Contact info component was added
  // For YOUNG_ANDROID_VERSION 105:
  // - SEMANTIC_FORM_COMPONENT_VERSION was added.
  // - SEMANTIC_WEB_LISTPICKER_COMPONENT_VERSION was added.
  // - TEXTBOX_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 106:
  // - EMAILPICKER_COMPONENT_VERSION was incremented to 3.
  // - PASSWORDTEXTBOX_COMPONENT_VERSION was incremented to 3.
  // - SEMANTIC_FORM_COMPONENT_VERSION was incremented to 2.
  // - TEXTBOX_COMPONENT_VERSION was incremented to 6.
  // For YOUNG_ANDROID_VERSION 107:
  // - SemanticForm Component is renamed to LinkedDataForm Component
  // - SEMANTIC_FORM_COMPONENT_VERSION is renamed to LINKED_DATA_FORM_COMPONENT_VERSION
  // - SemanticWeb Component is renamed to LinkedData Component
  // - SEMANTIC_WEB_COMPONENT_VERSION is renamed to LINKED_DATA_COMPONENT_VERSION
  // - SemanticListPicker Component is renamed to LinkedDataListPicker Component
  // - SEMANTIC_LISTPICKER_COMPONENT_VERSION is renamed to LINKED_DATA_LISTPICKER_COMPONENT_VERSION
  // - The pebbleSmartWatch component was added.
  // For YOUNG_ANDROID_VERSION 108:
  // - LINKED_DATA_LISTPICKER_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 109:
  // - LINKED_DATA_COMPONENT_VERSION was incremented to 3.
  
  // For YOUNG_ANDROID_VERSION 110:
  // - FORM_COMPONENT_VERSION was incremented to 12.
  // For YOUNG_ANDROID_VERSION 111:
  // - CAMERA_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 112:
  // - VIDEOPLAYER_COMPONENT_VERSION was incremented to 5.
  // - The Sharing Component was added
  // For YOUNG_ANDROID_VERSION 113:
  // - WEBVIEWER_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 114:
  // - SPINNER_COMPONENT_VERSION was incremented to 1
  // For YOUNG_ANDROID_VERSION 115:
  // - LISTVIEW_COMPONENT_VERSION was incremented to 1.
  // For YOUNG_ANDROID_VERSION 116:
  // - TEXTTOSPEECH_COMPONENT_VERSION was incremented to 2
  // For YOUNG_ANDROID_VERSION 117:
  // - DATEPICKER_COMPONENT_VERSION was incremented to 1.
  // For YOUNG_ANDROID_VERSION 118:
  // - TIMEPICKER_COMPONENT_VERSION was incremented to 1
  // For YOUNG_ANDROID_VERSION 119:
  // - FILE_COMPONENT_VERSION was incremented to 1.
  // For YOUNG_ANDROID_VERSION 120:
  // - YANDEX_COMPONENT_VERSION was incremented to 1.
  // For YOUNG_ANDROID_VERSION 121:
  // - BUTTON_COMPONENT_VERSION was incremented to 6.
  // For YOUNG_ANDROID_VERSION 122:
  // - LinkedDataStreamingClient was added.
  // For YOUNG_ANDROID_VERSION 123:
  // - Update Twitter uploadImage (removed the twitpick).
  // For YOUNG_ANDROID_VERSION 124 (mit-cml/master YaVersion 96):
  // - TIMEPICKER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 125 (mit-cml/master YaVersion 97):
  // - PLAYER_COMPONENT_VERSION was incremented to 6
  // For YOUNG_ANDROID_VERSION 126 (mit-cml/master YaVersion 98):
  // - PHONECALL_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 127 (mit-cml/master YaVersion 99):
  // - CONTACTPICKER_COMPONENT_VERSION was incremented to 5
  // For YOUNG_ANDROID_VERSION 128 (mit-cml/master YaVersion 100):
  // - DATEPICKER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 129 (mit-cml/master YaVersion 101):
  // - FORM_COMPONENT_VERSION was incremented to 13.
  // For YOUNG_ANDROID_VERSION 130 (mit-cml/master YaVersion 102):
  // - FUSIONTABLESCONTROL_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 131 (mit-cml/master YaVersion 103):
  // - LISTVIEW_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 132 (mit-cml/master YaVersion 104):
  // - TWITTER_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 133 (mit-cml/master YaVersion 105):
  // - WEB_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 134:
  // - TWITTER_COMPONENT_VERSION was incremented to 5.
  // For YOUNG_ANDROID_VERSION 135 (mit-cml/master YaVersion 106):
  // - LISTVIEW_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 136 (mit-cml/master YaVersion 107):
  // - WEBVIEWER_COMPONENT_VERSION was incremented to 5
  // For YOUNG_ANDROID_VERSION 137 (mit-cml/master YaVersion 108):
  // - New Obsfucate Text Block was added and BLOCKS_LANGUAGE_VERSION incremented to 18
  // For YOUNG_ANDROID_VERION 138 (mit-cml/master YaVersion 109):
  // - Added PROXIMITYSENSOR_COMPONENT_VERSION
  // For YOUNG_ANDROID_VERSION 139 (mit-cml/master YaVersion 110):
  // - LABEL_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 140 (mit-cml/master YaVersion 111):
  // - BARCODESCANNER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 141 (mit-cml/master YaVersion 112):
  // First test of new upgrading architecture: Dave Wolber's Canvas name changes
  // For YOUNG_ANDROID_VERSION 142 (mit-cml/master YaVersion 113):
  // Second test of new upgrading architecture: Evan Thomas's fillCircle argument for Canvas.DrawCircle
  // - CANVAS_COMPONENT_VERSION was incremented to 9.
  // For YOUNG_ANDROID_VERSION 143 (mit-cml/master YaVersion 114):
  // - FORM_COMPONENT_VERSION was incremented to 14.
  // For YOUNG_ANDROID_VERSION 144 (mit-cml/master YaVersion 115):
  // - CANVAS_COMPONENT_VERSION was incremented to 10.
  // For YOUNG_ANDROID_VERSION 145 (mit-cml/master YaVersion 116):
  // - LISTPICKER_COMPONENT_VERSION was incremented to 9.
  // For YOUNG_ANDROID_VERSION 146 (mit-cml/master YaVersion 117):
  // - LISTVIEW_COMPONENT_VERSION was incremented to 4.
  // For YOUNG_ANDROID_VERSION 147 (mit-cml/master YaVersion 118):
  // - SOUND_RECORDER_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 148 (mit-cml/master YaVersion 119):
  // - TEXTBOX_COMPONENT_VERSION was incremented to 5
  // - WEBVIEWER_COMPONENT_VERSION was incremented to 6
  // For YOUNG_ANDROID_VERSION 149 (mit-cml/master YaVersion 120):
  // - SLIDER_COMPONENT_VERSION was incremented to 2
  // For YOUNG_ANDROID_VERSION 150 (mit-cml/master YaVersion 121):
  // - NOTIFIER_COMPONENT_VERSION was incremented to 4
  // For YOUNG_ANDROID_VERSION 151 (mit-cml/master YaVersion 122):
  // - EMAILPICKER_COMPONENT_VERSION was incremented to 3
  // - PASSWORDTEXTBOX_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 152 (mit-cml/master YaVersion 123):
  // - TEXTTOSPEECH_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 153 (mit-cml/master YaVersion 124):
  // - FORM_COMPONENT_VERSION was incremented to 15.
  // For YOUNG_ANDROID_VERSION 154 (mit-cml/master YaVersion 125):
  // - LISTVIEW_COMPONENT_VERSION was incremented to 5
  // For YOUNG_ANDROID_VERSION 155 (mit-cml/master YaVersion 126):
  // - ACTIVITYSTARTER_COMPONENT_VERSION was incremented to 5
  // For YOUNG_ANDROID_VERSION 156 (mit-cml/master YaVersion 127):
  // - FORM_COMPONENT_VERSION was incremented to 16.
  // For YOUNG_ANDROID_VERSION 157 (mit-cml/master YaVersion 128):
  // - BLOCKS_LANGUAGE_VERSION was incremented to 19
  // For YOUNG_ANDROID_VERSION 158 (mit-cml/master YaVersion 129):
  // - CLOCK_COMPONENT_VERSION was incremented to 2
  // For YOUNG_ANDROID_VERSION 159 (mit-cml/master YaVersion 130):
  // - TEXTTOSPEECH_COMPONENT_VERSION was incremented to 4
  // For YOUNG_ANDROID_VERSION 160 (mit-cml/master YaVersion 131):
  // - CONTACTPICKER_COMPONENT_VERSION was incremented to 6.
  // For YOUNG_ANDROID_VERSION 161 (mit-cml/master YaVersion 132):
  // - TEXTTOSPEECH_COMPONENT_VERSION was incremented to 5
  // For YOUNG_ANDROID_VERSION 162 (mit-cml/master YaVersion 133):
  // - FILE_COMPONENT_VERSION was incremented to 2
  // For YOUNG_ANDROID_VERSION 163 (mit-cml/master YaVersion 134):
  // - DATEPICKER_COMPONENT_VERSION was incremented to 3
  // - TIMEPICKER_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 164 (mit-cml/master YaVersion 135):
  // - ACTIVITYSTARTER_COMPONENT_VERSION was incremented to 6
  // For YOUNG_ANDROID_VERSION 165 (mit-cml/master YaVersion 136):
  // - FORM_COMPONENT_VERSION was incremented to 17.
  // For YOUNG_ANDROID_VERSION 166 (mit-cml/master YaVersion 137):
  // - FORM_COMPONENT_VERSION was incremented to 18.
  // For YOUNG_ANDROID_VERSION 167 (mit-cml/master YaVersion 138):
  // - MEDIASTORE_COMPONENT_VERSION was incremented to 1
  // For YOUNG_ANDROID_VERSION 168 (mit-cml/master YaVersion 139):
  // - Reserved for FIRST Tech Challenge.
  // For YOUNG_ANDROID_VERSION 169 (mit-cml/master YaVersion 140):
  // - HORIZONTALARRANGEMENT_COMPONENT_VERSION was incremented to 3.
  // - VERTICALARRANGEMENT_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 170 (mit-cml/master YaVersion 141):
  // - Reserved for FIRST Tech Challenge.
  // For YOUNG_ANDROID_VERSION 171 (mit-cml/master YaVersion 142):
  // - FORM_COMPONENT_VERSION was incremented to 19.
  // For YOUNG_ANDROID_VERSION 172 (mit-cml/master YaVersion 143):
  // - GyroscopeSensor component was added
  // For YOUNG_ANDROID_VERSION 173 (mit-cml/master YaVersion 144):
  // - Reserved for FIRST Tech Challenge.
  // For YOUNG_ANDROID_VERSION 174 (mit-cml/master YaVersion 145):
  // - Spelling of "obsfucate" was corrected to obfuscate and BLOCKS_LANGUAGE_VERSION incremented to 20
  // For YOUNG_ANDROID_VERSION 175 (mit-cml/master YaVersion 146):
  // - CAMERA_COMPONENT_VERSION was incremented to 3.
  // For YOUNG_ANDROID_VERSION 176 (mit-cml/master YaVersion 147):
  // - IMAGE_COMPONENT_VERSION was incremented to 2.
  // For YOUNG_ANDROID_VERSION 177 (mit-cml/master YaVersion 148):
  // - FIREBASE_COMPONENT_VERSION was incremented to 1
  // For YOUNG_ANDROID_VERSION 178 (mit-cml/master YaVersion 149):
  // - CLOCK_COMPONENT_VERSION was incremented to 3
  // For YOUNG_ANDROID_VERSION 179 (mit-cml/master YaVersion 150):
  // - IMAGE_COMPONENT_VERSION was incremented to 3

  public static final int YOUNG_ANDROID_VERSION = 179;

  // ............................... Blocks Language Version Number ...............................

  // NOTE(lizlooney,user) - when the blocks language changes:
  // 1. Increment YOUNG_ANDROID_VERSION above.
  // 2. Increment BLOCKS_LANGUAGE_VERSION here
  // 3. ***Add code in yacodeblocks.BlockSaveFile#upgradeLanguage to upgrade the .blk file contents
  // 4. Add code in YoungAndroidFormUpgrader to upgrade the source file
  // *** BlockSaveFile is no longer used in App Inventor 2 (Feb. 2014)
 

  // For BLOCKS_LANGUAGE_VERSION 2:
  // - Allow arguments of different procedures and events to have the same names.
  // For BLOCKS_LANGUAGE_VERSION 3:
  // - Some String operations were added: text<, text=, text>, trim, upcase, downcase
  // For BLOCKS_LANGUAGE_VERSION 4:
  // Added: replace all, copy list, insert list item, for each in range
  // For BLOCKS_LANGUAGE_VERSION 5:
  // - The Math trigonometry functions' formal parameter names were changed, and two
  //   blocks (degrees-to-radians and radians-to-degrees) were added.
  // For BLOCKS_LANGUAGE_VERSION 6:
  // - Text blocks, comments, and complaints are encoded on save and decoded on load to
  // preserve international characters.
  // For BLOCKS_LANGUAGE_VERSION 7:
  // - Corrupted character sequences in comments are replaced with * when .blk files are upgraded.
  // For BLOCKS_LANGUAGE_VERSION 8:
  // - Socket labels of some text blocks were changed.
  // For BLOCKS_LANGUAGE_VERSION 9:
  // - Socket labels for degrees-to-radians and radians-to-degrees were fixed.
  // For BLOCKS_LANGUAGE_VERSION 10:
  // - Added not-equal block.  Add "as" descriptor to def block.
  // For BLOCKS_LANGUAGE_VERSION 11:
  // - CSV-related list functions were added (list to csv row, list to csv table,
  //   list from csv row, list from csv table)
  // For BLOCKS_LANGUAGE_VERSION 12:
  // - Changed multiply symbol from star to times; change subtract symbol from hyphen to minus
  // For BLOCKS_LANGUAGE_VERSION 13:
  // - Added open-screen and open-screen-with-start-text.
  // For BLOCKS_LANGUAGE_VERSION 14:
  // - Added generated blocks for component object methods and properties.
  // For BLOCKS_LANGUAGE_VERSION 15:
  // - Added "is text empty?" to Text drawer.
  // For BLOCKS_LANGUAGE_VERSION 16:
  // - Added make-color and split-color to Color drawer.
  // For BLOCKS_LANGUAGE_VERSION 17:
  // - Changed open-screen to open-another-screen
  // - Changed open-screen-with-start-text to open-another-screen-with-start-value
  // - Marked get-startup-text as a bad block
  // - Added get-start-value
  // - Added get-plain-start-text
  // - Marked close-screen-with-result as a bad block
  // - Added close-screen-with-value
  // - Added close-screen-with-plain-text
  // For BLOCKS_LANGUAGE_VERSION 18:
  // - New Obsfucate Text Block was added
  // For BLOCKS_LANGUAGE_VERSION 19:
  // The is-number block was modified to include dropdowns for base10, hex, and binary
  // The number-convert blocks was added
  // For BLOCKS_LANGUAGE_VERSION 20:
  // - Spelling of "Obsfucate" was corrected to Obfuscate in Text Block
  public static final int BLOCKS_LANGUAGE_VERSION = 20;

  // ................................. Component Version Numbers ..................................

  // NOTE(lizlooney,user) - when a new component is added:
  // 1. Increment YOUNG_ANDROID_VERSION above.
  // 2. Add the version number for the new component below
  // 3. Add documentation to the appropriate html file in docs/reference/components.

  // NOTE(lizlooney,user) - when a component changes:
  // 1. Increment YOUNG_ANDROID_VERSION above.
  // 2. Increment the version number for that component below
  // 3. Add code in com.google.appinventor.client.youngandroid.YoungAndroidFormUpgrader#
  //    upgradeComponentProperties to upgrade the .scm file contents
  // *** OBSOLETE 4. Add code in openblocks.yacodeblocks.BlockSaveFile#upgradeComponentBlocks to
  // *** OBSOLETE upgrade the .blk file contents (not used in AI 2)
  // 4. For AI2, update the table in blocklyeditor/src/versioning.js
  // 5. Update documentation in the appropriate html file in docs/reference/components.


  // Note added after internationalization (8/25/2014)
  // If you add any properties, events or methods to a component you *must*:

  //   Add an entry for each new property/event/method into
  //   OdeMessages.java iff a property with that name doesn't already
  //   exist (so if you are adding a property that has the same name as
  //   another property in a different component, you don't do it a
  //   second time). To add the "Foo" property you would add:

  //         @defaultMessage("Foo")
  //         @description("")
  //         String FooProperties();

  //   If you edit the description of a component (but not yet a
  //   property,method or event of that component) you must also find and
  //   update the description in OdeMessages.java



  //For ACCELEROMETERSENSOR_COMPONENT_VERSION 2:
  // - AccelerometerSensor.MinimumInterval property was added.
  // - AccelerometerSensor.AccelerationChanged method was modified to wait for
  //   the minimum interval to elapse before calling a shaking event when necessary.
  //For ACCELEROMETERSENSOR_COMPONENT_VERSION 3:
  // - AccelerometerSensor.Sensitivty property was added.
  public static final int ACCELEROMETERSENSOR_COMPONENT_VERSION = 3;

  // For ACTIVITYSTARTER_COMPONENT_VERSION 2:
  // - The ActivityStarter.DataType, ActivityStarter.ResultType, and ActivityStarter.ResultUri
  //   properties were added.
  // - The ActivityStarter.ResolveActivity method was added.
  // - The ActivityStarter.ActivityError event was added.
  // For ACTIVITYSTARTER_COMPONENT_VERSION 3:
  // - The ActivityStarter.ActivityError event was marked userVisible false and is no longer used.
  // For ACTIVITYSTARTER_COMPONENT_VERSION 4:
  // - The ActivityStarter.StartActivity was edited to use the parent Form's open screen
  //   animation to transition to next activity.
  // For ACTIVITYSTARTER_COMPONENT_VERSION 5:
  // - The ActivityStarter.ActivityCanceled event was added.
  // For ACTIVITYSTARTER_COMPONENT_VERSION 6:
  // - Extras property was added to accept a list of key-value pairs to put to the intent
  public static final int ACTIVITYSTARTER_COMPONENT_VERSION = 6;

  // For BALL_COMPONENT_VERSION 2:
  // - The PointTowards method was added (for all sprites)
  // - The heading property was changed from int to double (for all sprites
  // For BALL_COMPONENT_VERSION 3:
  // - The Z property was added (also for ImageSprite)
  // For BALL_COMPONENT_VERSION 4:
  // - The TouchUp, TouchDown, and Flung events were added. (for all sprites)
  // For BALL_COMPONENT_VERSION 5:
  // - Callback parameters speed and heading were added to Flung. (for all sprites)
  public static final int BALL_COMPONENT_VERSION = 5;

  public static final int BARCODESCANNER_COMPONENT_VERSION = 1;

  // For BLUETOOTHCLIENT_COMPONENT_VERSION 2:
  // - The BluetoothClient.Enabled property was added.
  // For BLUETOOTHCLIENT_COMPONENT_VERSION 3:
  // - The BluetoothClient.BluetoothError event was marked userVisible false and is no longer used.
  // For BLUETOOTHCLIENT_COMPONENT_VERSION 4:
  // - The BluetoothClient.DelimiterByte property was added.
  // For BLUETOOTHCLIENT_COMPONENT_VERSION 5:
  // - The BluetoothClient.Secure property was added.
  public static final int BLUETOOTHCLIENT_COMPONENT_VERSION = 5;

  // For BLUETOOTHSERVER_COMPONENT_VERSION 2:
  // - The BluetoothServer.Enabled property was added.
  // For BLUETOOTHSERVER_COMPONENT_VERSION 3:
  // - The BluetoothServer.BluetoothError event was marked userVisible false and is no longer used.
  // For BLUETOOTHSERVER_COMPONENT_VERSION 4:
  // - The BluetoothServer.DelimiterByte property was added.
  // For BLUETOOTHSERVER_COMPONENT_VERSION 5:
  // - The BluetoothServer.Secure property was added.
  public static final int BLUETOOTHSERVER_COMPONENT_VERSION = 5;

  // For BUTTON_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For BUTTON_COMPONENT_VERSION 3:
  // - The LongClick event was added.
  // For BUTTON_COMPONENT_VERSION 4:
  // - The Shape property was added.
 
  // For BUTTON_COMPONENT_VERSION 5:
  // - The ShowFeedback property was added.
  // For BUTTON_COMPONENT_VERSION 6:
  // - Added TouchUp and TouchDown events
  // - FontSize, FontBold, FontItalic properties made visible in block editor
  public static final int BUTTON_COMPONENT_VERSION = 6;
 
  public static final int CAMCORDER_COMPONENT_VERSION = 1;

  // For CAMERA_COMPONENT_VERSION 2:
  // - The UseFront property was added.
  // For CAMERA_COMPONENT_VERSION 3:
  // - The UseFront property was removed :-( .
   public static final int CAMERA_COMPONENT_VERSION = 3;

  // For CANVAS_COMPONENT_VERSION 2:
  // - The LineWidth property was added.
  // For CANVAS_COMPONENT_VERSION 3:
  // - The FontSize property was added.
  // - The TextAlignment property was added.
  // - The DrawText method was added.
  // - The DrawTextAtAngle method was added.
  // For CANVAS_COMPONENT_VERSION 4:
  // - Added Save and SaveAs methods
  // For CANVAS_COMPONENT_VERSION 5:
  // - Added GetBackgroundPixelColor, GetPixelColor, and SetBackgroundPixelColor methods.
  // For CANVAS_COMPONENT_VERSION 6:
  // - Added TouchDown, TouchUp, and Flung events.
  // For CANVAS_COMPONENT_VERSION 7:
  // - Callback parameters speed and heading were added to Flung. (for all sprites)
  // For CANVAS_COMPONENT_VERSION 8:
  // Dave Wolber's Canvas name changes:
  // - DrawCircle parameter names changed to centerx,centery, radius
  // - Touched parameter touchedSprite name changed to touchedAnySprite
  // - Dragged parameter draggedSprite name changed to draggedAnySprite
  // For CANVAS_COMPONENT_VERSION 9:
  // - DrawCircle has new fourth parameter (for isFilled), due to Evan Thomas
  // For CANVAS_COMPONENT_VERSION 10:
  // - The default value of the TextAlignment property was changed to Component.ALIGNMENT_CENTER
  public static final int CANVAS_COMPONENT_VERSION = 10;

  // For CHECKBOX_COMPONENT_VERSION 2:
  // - The Value property was renamed to Checked.
  public static final int CHECKBOX_COMPONENT_VERSION = 2;

  // For CLOCK_COMPONENT_VERSION 2:
  // - The pattern parameter was added to the FormatDate and FormatDateTime.
  // - Add Duration Support
  public static final int CLOCK_COMPONENT_VERSION = 3;

  // For CONTACTPICKER_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For CONTACTPICKER_COMPONENT_VERSION 3:
  // - The method Open was added.
  // For CONTACTPICKER_COMPONENT_VERSION 4:
  // - The Shape property was added.
  // For CONTACTPICKER_COMPONENT_VERSION 5:
  // - Added PhoneNumber, PhoneNumberList, and EmailAddressList to ContactPicker.
  // - For Eclair and up, we now use ContactsContract instead of the deprecated Contacts.
  // For CONTACTPICKER_COMPONENT_VERSION 6:
  // - The ContactUri property was added
  public static final int CONTACTPICKER_COMPONENT_VERSION = 6;

  // For EMAILPICKER_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For EMAILPICKER_COMPONENT_VERSION 3:
  // - The ConceptURI property was renamed to ObjectType
  // - RequestFocus function was added (via TextBoxBase)
  public static final int EMAILPICKER_COMPONENT_VERSION = 3;

  // For DATEPICKER_COMPONENT_VERSION 2:
  // The datepicker dialog was updated to show the current date
  // instead of the last set date by default.
  // The SetDateToDisplay and LaunchPicker methods were added to
  // give the user more control of what time is displayed in the
  // datepicker dialog.
  // For DATEPICKER_COMPONENT_VERSION 3:
  // - SetDateToDisplayFromInstant, and Instant property are added.
  public static final int DATEPICKER_COMPONENT_VERSION = 3;

  // For FILE_COMPONENT_VERSION 2:
  // - The AfterFileSaved event was added.
  public static final int FILE_COMPONENT_VERSION = 2;

  // For FORM_COMPONENT_VERSION 2:
  // - The Screen.Scrollable property was added.
  // For FORM_COMPONENT_VERSION 3:
  // - The Screen.Icon property was added.
  // For FORM_COMPONENT_VERSION 4:
  // - The Screen.ErrorOccurred event was added.
  // For FORM_COMPONENT_VERSION 5:
  // - The Screen.ScreenOrientation property and Screen.ScreenOrientationChanged event were added.
  // For FORM_COMPONENT_VERSION 6:
  // - The SwitchForm and SwitchFormWithArgs methods were removed and the OtherScreenClosed event
  // was added.
  // For FORM_COMPONENT_VERSION 7:
  // - The VersionCode and VersionName properties were added.
  // For FROM_COMPONENT_VERSION 8:
  // - The AlignHorizontal property was added
  // - The AlignVertical property was added
  // For FORM_COMPONENT_VERSION 9:
  // - The OpenScreenAnimation property was added
  // - The CloseScreenAnimation property was added
  // For FORM_COMPONENT_VERSION 10:
  // - The BackPressed event was added.
  // For FORM_COMPONENT_VERSION 11:
  // - OpenScreenAnimation and CloseScreenAnimation are now properties.
  // For FORM_COMPONENT_VERSION 12:
  // - AboutScreen property was added
  // For FORM_COMPONENT_VERSION 13:
  // - The Screen.Scrollable property was set to False by default
  // For FORM_COMPONENT_VERSION 14:
  // - The Screen1.AppName was added and no block need to be changed.
  // For FORM_COMPONENT_VERSION 15:
  // - The Screen.ShowStatusBar was added.
  // For FORM_COMPONENT_VERSION 16:
  // - TitleVisible property was added
  // For FORM_COMPONENT_VERSION 17:
  // - The Screen.CompatibilityMode property was added
  // For FORM_COMPONENT_VERSION 18:
  // - Screen.CompatibilityMode property morphed into the
  //   Sizing property
  // For FORM_COMPONENT_VERSION 19:
  // - Added HideKeyboard method
  public static final int FORM_COMPONENT_VERSION = 19;


  // For FUSIONTABLESCONTROL_COMPONENT_VERSION 2:
  // - The Fusiontables API was migrated from SQL to V1
  // For FUSIONTABLESCONTROL_COMPONENT_VERSION 3:
  // - InsertRow, GetRows and GetRowsWithConditions was added.
  // - KeyFile, UseServiceAuthentication and ServiceAccountEmail
  //   were added.
  public static final int FUSIONTABLESCONTROL_COMPONENT_VERSION = 3;

  public static final int GAMECLIENT_COMPONENT_VERSION = 1;

  public static final int GYROSCOPESENSOR_COMPONENT_VERSION = 1;

  // For HORIZONTALARRANGEMENT_COMPONENT_VERSION 2:
  // - The AlignHorizontal property was added
  // - The AlignVertical property was added
  // - Added background color & image
  public static final int HORIZONTALARRANGEMENT_COMPONENT_VERSION = 3;

  // For IMAGE_COMPONENT_VERSION 2:
  // - The RotationAngle property was added.
  // For IMAGE_COMPONENT_VERSION 3:
  // - Scaling Property added, but hidden for now
  public static final int IMAGE_COMPONENT_VERSION = 3;

  // For IMAGEPICKER_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For IMAGEPICKER_COMPONENT_VERSION 3:
  // - The method Open was added.
  // For IMAGEPICKER_COMPONENT_VERSION 4:
  // - The Shape property was added.
  // For IMAGEPICKER_COMPONENT_VERSION 5:
  // - The ImagePath property was changed to Selection, and now returns a file path to
  //   external storage

  public static final int IMAGEPICKER_COMPONENT_VERSION = 5;

  // For IMAGESPRITE_COMPONENT_VERSION 2:
  // - The Rotates property was added.
  // For IMAGESPRITE_COMPONENT_VERSION 3:
  // - The PointTowards method was added (for all sprites)
  // - The heading property was changed from int to double (for all sprites)
  // For IMAGESPRITE_COMPONENT_VERSION 4:
  // - The Z property was added (also for Ball)
  // For IMAGESPRITE_COMPONENT_VERSION 5:
  // - The TouchUp, TouchDown, and Flung events were added. (for all sprites)
  // For IMAGESPRITE_COMPONENT_VERSION 6:
  // - Callback parameters speed and heading were added to Flung. (for all sprites)
  public static final int IMAGESPRITE_COMPONENT_VERSION = 6;

  // For LABEL_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For LABEL_COMPONENT_VERSION 3:
  // - The HasMargins property was added
  public static final int LABEL_COMPONENT_VERSION = 3;

  // For LISTPICKER_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For LISTPICKER_COMPONENT_VERSION 3:
  // - The SelectionIndex read-write property was added.
  // For LISTPICKER_COMPONENT_VERSION 4:
  // - The method Open was added.
  // For LISTPICKER_COMPONENT_VERSION 5:
  // - The Shape property was added.
  // For LISTPICKER_COMPONENT_VERSION 6:
  // - The getIntent method was modified to provide the ListPickerActivity
  //   with the parent Form's open screen animation.
  // For LISTPICKER_COMPONENT_VERSION 7:
  // - Added ShowFilterBar property
  // For LISTPICKER_COMPONENT_VERSION 8:
  // - Added title property
  // For LISTPICKER_COMPONENT_VERSION 9:
  // - Added ItemTextColor, ItemBackgroundColor
  public static final int LISTPICKER_COMPONENT_VERSION = 9;

  // For LISTVIEW_COMPONENT_VERSION 1:
  // - Initial version.
  // For LISTVIEW_COMPONENT_VERSION 2:
  // - Added Elements property
  // For LISTVIEW_COMPONENT_VERSION 3:
  // - Added BackgroundColor Property
  // - Added TextColor Property
  // For LISTVIEW_COMPONENT_VERSION 4:
  // - Added TextSize Property
  // For LISTVIEW_COMPONENT_VERSION 5:
  // - Added SelectionColor Property
  public static final int LISTVIEW_COMPONENT_VERSION = 5;

  // For LOCATIONSENSOR_COMPONENT_VERSION 2:
  // - The TimeInterval and DistanceInterval properties were added.
  public static final int LOCATIONSENSOR_COMPONENT_VERSION = 2;

  // For NEARFIELD_COMPONENT_VERSION 1:
  public static final int NEARFIELD_COMPONENT_VERSION = 1;

  // For NOTIFIER_COMPONENT_VERSION 2:
  // - To ShowChooseDialog and ShowTextDialog, new arg was added to indicate if dialog is cancelable
  // For NOTIFIER_COMPONENT_VERSION 3:
  // - Added NotifierColor, TextColor and NotifierLength options
  // For NOTIFIER_COMPONENT_VERSION 4:
  // - Added a ShowProgressDialog method, and a DismissProgressDialog method
  public static final int NOTIFIER_COMPONENT_VERSION = 4;

  public static final int NXT_COLORSENSOR_COMPONENT_VERSION = 1;

  public static final int NXT_DIRECT_COMMANDS_COMPONENT_VERSION = 1;

  public static final int NXT_DRIVE_COMPONENT_VERSION = 1;

  public static final int NXT_LIGHTSENSOR_COMPONENT_VERSION = 1;

  public static final int NXT_SOUNDSENSOR_COMPONENT_VERSION = 1;

  public static final int NXT_TOUCHSENSOR_COMPONENT_VERSION = 1;

  public static final int NXT_ULTRASONICSENSOR_COMPONENT_VERSION = 1;

  // For ORIENTATIONSENSOR_COMPONENT_VERSION = 2:
  // - The Yaw property was renamed to Azimuth.
  // - The yaw parameter to OrientationChanged was renamed to azimuth.
  public static final int ORIENTATIONSENSOR_COMPONENT_VERSION = 2;
  
  public static final int SOCIALPROXIMITYSENSOR_COMPONENT_VERSION = 1;
  
  public static final int WIFISENSOR_COMPONENT_VERSION = 1;
  
  public static final int LOCATIONPROBESENSOR_COMPONENT_VERSION = 1;
  
  public static final int CELLTOWERPROBESENSOR_COMPONENT_VERSION = 1;
  
  public static final int ACTIVITYPROBESENSOR_COMPONENT_VERSION = 1;
  
  public static final int RUNNINGAPPLICATIONS_COMPONENT_VERSION = 1;
  
  public static final int SCREENSTATUS_COMPONENT_VERSION = 1;
  
  public static final int LIGHTSENSOR_COMPONENT_VERSION = 1;
  
  public static final int CALLLOGHISTORY_COMPONENT_VERSION = 1;
  
  public static final int TIMER_COMPONENT_VERSION = 1;
  
  public static final int BATTERYSENSOR_COMPONENT_VERSION = 1;
  public static final int PEDOMETERSENSOR_COMPONENT_VERSION = 1;
  public static final int SURVEY_COMPONENT_VERSION = 1;
  public static final int TELEPHONY_COMPONENT_VERSION = 1;

  // For PASSWORDTEXTBOX_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For PASSWORDTEXTBOX_COMPONENT_VERSION 3
  // - The ConceptURI property was renamed to ObjectType.
  // - Added RequestFocus Function (via TextBoxBase)
  public static final int PASSWORDTEXTBOX_COMPONENT_VERSION = 3;

  public static final int PEDOMETER_COMPONENT_VERSION = 1;

  // For PHONECALL_COMPONENT_VERSION 2:
  // - The PhoneCallStarted event was added.
  // - The PhoneCallEnded event was added.
  // - The IncomingCallAnswered event was added.
  public static final int PHONECALL_COMPONENT_VERSION = 2;

  // For PHONENUMBERPICKER_COMPONENT_VERSION 2:
  // - The Alignment property was renamed to TextAlignment.
  // For PHONENUMBERPICKER_COMPONENT_VERSION 3:
  // - The method Open was added.
  // For PHONENUMBERPICKER_COMPONENT_VERSION 4:
  // - The Shape property was added.
  public static final int PHONENUMBERPICKER_COMPONENT_VERSION = 4;

  public static final int PHONESTATUS_COMPONENT_VERSION = 1;

  // For PLAYER_COMPONENT_VERSION 2:
  // - The Player.PlayerError event was added.
  // For PLAYER_COMPONENT_VERSION 3:
  // - The Player.PlayerError event was marked userVisible false and is no longer used.
  // For PLAYER_COMPONENT_VERSION 4:
  // - The Player.Completed event was added.
  // - The IsLooping property was added.
  // - The Volume property was added.
  // - The IsPlaying method was added.
  // For PLAYER_COMPONENT_VERSION 5:
  // - The IsLooping property was renamed to Loop.
  // For PLAYER_COMPONENT_VERSION 6:
  // - The PlayInForeground property was added.
  // - The OtherPlayerStarted event was added.

  public static final int PLAYER_COMPONENT_VERSION = 6;

  public static final int SHARING_COMPONENT_VERSION = 1;

  // For SLIDER_COMPONENT_VERSION 1:
  // - Initial version.
  // For SLIDER_COMPONENT_VERSION 2:
  // - Added the property to allow for the removal of the Thumb Slider
  public static final int SLIDER_COMPONENT_VERSION = 2;

  // For SPINNER_COMPONENT_VERSION 1:
  public static final int SPINNER_COMPONENT_VERSION = 1;

  // For SOUND_COMPONENT_VERSION 2:
  // - The Sound.SoundError event was added.
  // For SOUND_COMPONENT_VERSION 3:
  // - The Sound.SoundError event was marked userVisible false and is no longer used.
  public static final int SOUND_COMPONENT_VERSION = 3;

  // For SOUND_RECORDER_COMPONENT_VERSION 2:
  // - The SavedRecording property was added.
  public static final int SOUND_RECORDER_COMPONENT_VERSION = 2;

  public static final int SPEECHRECOGNIZER_COMPONENT_VERSION = 1;

  public static final int TABLEARRANGEMENT_COMPONENT_VERSION = 1;

  // For TEXTBOX_COMPONENT_VERSION 2:
  // - The TextBox.NumbersOnly property was added.
  // For TEXTBOX_COMPONENT_VERSION 3:
  // - The Alignment property was renamed to TextAlignment.
  // For TEXTBOX_COMPONENT_VERSION 4:
  // - The HideKeyboard method was added.
  // - The MultiLine property was added.
  // For TEXTBOX_COMPONENT_VERSION 5:
  // - Implementation of LDComponent was added.
  // For TEXTBOX_COMPONENT_VERSION 6:
  // - The ConceptURI property was renamed to ObjectType.
  // - RequestFocus method was added
  public static final int TEXTBOX_COMPONENT_VERSION = 6;

  // For TEXTING_COMPONENT_VERSION 2:
  // Texting over Wifi was implemented using Google Voice
  // This works only on phones with 2.0 (Eclair) or higher.
  // It requires that the user has a Google Voice account
  // and has the mobile Voice app installed on the phone.
  // Access to Google Voice is controlled through OAuth 2.
  // For TEXTING_COMPONENT_VERISON 3:
  // - receivingEnabled is now an integer in the range 1-3
  //   instead of a boolean
  public static final int TEXTING_COMPONENT_VERSION = 3;

  // For TEXTTOSPEECH_COMPONENT_VERSION 2:
  // - added speech pitch and rate
  // For TEXTTOSPEECH_COMPONENT_VERSION 3:
  // - the AvailableLanguages property was added
  // - the AvailableCountries property was added
  // For TEXTTOSPEECH_COMPONENT_VERSION 4:
  // - the Country designer property was changed to use a ChoicePropertyEditor
  // - the Language designer property was changed to use a ChoicePropertyEditor
  // For TEXTTOSPEECH_COMPONENT_VERSION 5:
  // - default value was added to the Country designer property
  // - default value was added to the Language designer property
  public static final int TEXTTOSPEECH_COMPONENT_VERSION = 5;

  // For TIMEPICKER_COMPONENT_VERSION 2:
  // After feedback from the forum, the timepicker dialog was updated
  // to show the current time instead of the last set time by default.
  // The SetTimeToDisplay and LaunchPicker methods were added to
  // give the user more control of what time is displayed in the
  // timepicker dialog.
  // For TIMEPICKER_COMPONENT_VERSION 3:
  // - SetTimeToDisplayFromInstant, and Instant property are added.
  public static final int TIMEPICKER_COMPONENT_VERSION = 3;

  public static final int TINYDB_COMPONENT_VERSION = 1;

  // For TINYWEBDB_COMPONENT_VERSION 2:
  // - The TinyWebDB.ShowAlert method was removed. Notifier.ShowAlert should be used instead.
  public static final int TINYWEBDB_COMPONENT_VERSION = 2;

  // // For FIREBASE_COMPONENT_VERSION 1:
  // - FirebaseDB component introduced
  public static final int FIREBASE_COMPONENT_VERSION = 1;

  // For TWITTER_COMPONENT_VERSION 2:
  // - The Authorize method and IsAuthorized event handler were added to support
  //   OAuth authentication (now requred by Twitter). These
  //   should be used instead of Login and IsLoggedIn. Login is still there but
  //   calling Login pops up a notification to use Authorize. IsLoggedIn will
  //   be changed to IsAuthorized when the blocks file is upgraded.
  // - Added CheckAuthorized method to check whether the user is already
  //   logged in and call IsAuthorized if so. We save the accessToken across app
  //   invocations, so it is possible that the app already has authorization
  //   when it starts up.
  // - Added DeAuthorize method to forget authorization token (logout, effectively).
  // - Added ConsumerKey and ConsumerSecret designer properties (required for
  //   Authorize)
  // - Added Username read-only property that returns the Twitter username when
  //   the user is logged in.
  // - The friend timeline was changed to be a list of tuples (lists), where
  //   each sub-list is (username message). The old format was just a list
  //   of messages and didn't include the username associated with each message.
  // For TWITTER_COMPONENT_VERSION 3:
  // - The 'SetStatus' procedure has been changed to 'Tweet' to be more intuitive.
  // - Added 'TweetWithImage' which uploads an image to TwitPic and adds it to
  //   a tweet to allow a user to tweet with a picture. This requires a TwitPic_API_Key
  //   property.
  // For TWITTER_COMPONENT_VERSION 4:
  // - Modified 'TweetWithImage' to upload images to Twitter directly because of the shutdown of
  //   TwitPic. The TwitPic_API_Key property is now deprecated and hidden.
  // For TWITTER_COMPONENT_VERSION 5:
  // - Added 'ImageUploaded' event to report when an image is uploaded to Twitter and the
  //   associated URL for that image.
  public static final int TWITTER_COMPONENT_VERSION = 5;

  // For VERTICALARRANGEMENT_COMPONENT_VERSION 2:
  // - The AlignHorizontal property was added
  // - The AlignVertical property was added
  // - Added background color & image
  public static final int VERTICALARRANGEMENT_COMPONENT_VERSION = 3;

  // For VIDEOPLAYER_COMPONENT_VERSION 2:
  // - The VideoPlayer.VideoPlayerError event was added.
  // For VIDEOPLAYER_COMPONENT_VERSION 3:
  // - The VideoPlayer.VideoPlayerError event was marked userVisible false and is no longer used.
  // For VIDEOPLAYER_COMPONENT_VERSION 4:
  // - The VideoPlayer.width and VideoPlayer.height variables were marked as user visible.
  // - The FullScreen property was added to the VideoPlayer.
  // For VIDEOPLAYER_COMPONENT_VERSION 5:
  // - The Volume property (setter only) was added to the VideoPlayer.
  public static final int VIDEOPLAYER_COMPONENT_VERSION = 5;

  public static final int VOTING_COMPONENT_VERSION = 1;

  // For WEB_COMPONENT_VERSION 2:
  // - The RequestHeaders and AllowCookies properties were added.
  // - The BuildPostData and ClearCookies methods were added.
  // - The existing PostText method was renamed to PostTextWithEncoding, and a new PostText
  //   method was added.
  // For WEB_COMPONENT_VERSION 3:
  // - PUT and DELETE Actions added (PutText, PutTextWithEncoding, PutFile, and Delete).
  // For WEB_COMPONENT_VERSION 4:
  // - Added method XMLTextDecode
  public static final int WEB_COMPONENT_VERSION = 4;

  // For WEBVIEWER_COMPONENT_VERSION 2:
  // - The CanGoForward and CanGoBack methods were added
  // For WEBVIEWER_COMPONENT_VERSION 3:
  // - Add UsesLocation property to set location permissions
  // For WEBVIEWER_COMPONENT_VERSION 4:
  // - Add WebViewString
  // For WEBVIEWER_COMPONENT_VERSION 5:
  // - IgnoreSslError property added
  // For WEBVIEWER_COMPONENT_VERSION 6:
  // - ClearCaches method was added
  public static final int WEBVIEWER_COMPONENT_VERSION = 6;
 
  public static final int DROPBOX_COMPONENT_VERSION = 1;
  // For LINKED_DATA_COMPONENT_VERSION_2:
  // - Renamed SemanticWeb component to LinkedData component
  // For LINKED_DATA_COMPONENT_VERSION 3:
  // - Removed BaseURL property eclipsed by FormID on Linked Data Form
  public static final int LINKED_DATA_COMPONENT_VERSION = 3;

  public static final int LD_COMPONENT_VERSION = 1;
  // For MEDIASTORE_COMPONENT_VERSION 1:
  // - Initial Version.
  public static final int MEDIASTORE_COMPONENT_VERSION = 1;

  // For YANDEX_COMPONENT_VERSION 1:
  // - Initial version.
  public static final int YANDEX_COMPONENT_VERSION = 1;

  //For PROXIMITYSENSOR_COMPONENT_VERSION: Initial Version
  public static final int PROXIMITYSENSOR_COMPONENT_VERSION = 1;

  // Rendezvous Server Location

  public static final String RENDEZVOUS_SERVER = "rendezvous.appinventor.mit.edu";

  // Companion Versions and Update Information

  // The PREFERRED_COMPANION is displayed to the end-user if
  // they ask (via the Help->About menu) and if they are told
  // that they need to update their companion
  //
  // ACCEPTABLE_COMPANIONS is a list of Companion VersionNames
  // which are usable with this version of the system.
  //
  // COMPANION_UPDATE_URL is the URL used by the Companion
  // Update Mechanism to find the Companion to download.
  // Note: This new Companion needs to be signed by the same
  // key as the Companion it is replacing, as the Package Manager
  // is invoked from the running Companion.

  public static final String PREFERRED_COMPANION = "2.36punya1";
  public static final String COMPANION_UPDATE_URL = "";
  public static final String COMPANION_UPDATE_URL1 = "";
  public static final String [] ACCEPTABLE_COMPANIONS = { "2.36punya1" };

  // Splash Screen Values
  public static final int SPLASH_SURVEY = 1;

  // For GOOGLECLOUDMESSAGING_COMPONENT_VERSION 1:
  // - Initial version.
  public static final int GOOGLECLOUDMESSAGING_COMPONENT_VERSION = 1;

  // For LINKED_DATA_LISTPICKER_COMPONENT_VERSION_2:
  // - Renamed SemanticWebListPicker to LinkedDataListPicker
  // For LINKED_DATA_LISTPICKER_COMPONENT_VERSION_3:
  // - Added RelationToObject property
  public static final int LINKED_DATA_LISTPICKER_COMPONENT_VERSION = 3;

  // For SEMANTIC_FORM_COMPONENT_VERSION 1:
  // - Initial version.
  // For SEMANTIC_FORM_COMPONENT_VERSION 2:
  // - Renamed BaseURI to Form ID
  // - Renamed ConceptURI to Object Type
  // For LINKED_DATA_FORM_COMPONENT_VERSION 3:
  // - Renamed Semantic Form to Linked Data Form

  public static final int LINKED_DATA_FORM_COMPONENT_VERSION = 3;
  
  public static final int GOOGLE_DRIVE_COMPONENT_VERSION = 1;

  // For GOOGLEMAP_COMPONENT_VERSION 1:
  // - Initial version.
  public static final int GOOGLE_MAP_COMPONENT_VERSION = 1;

  public static final int DATA_VIS_COMPONENT_VERSION = 2;
  
  public static final int SMSLOGHISTORY_COMPONENT_VERSION = 1;

  public static final int  SENSORDB_COMPONENT_VERSION = 1;
    
  public static final int CONTACTINFO_COMPONENT_VERSION = 1;	

  public static final int PEBBLESMARTWATCH_COMPONENT_VERSION = 1;	
  
  // For LINKEDDATASTREAMING_COMPONENT_VERSION 1:
  public static final int LINKEDDATASTREAMING_COMPONENT_VERSION = 1;

}
