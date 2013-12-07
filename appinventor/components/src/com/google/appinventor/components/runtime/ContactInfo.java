package com.google.appinventor.components.runtime;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;

import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.ContactProbe;



@DesignerComponent(version = YaVersion.CONTACTINFO_COMPONENT_VERSION, 
description = "Return information of a user's contacts" , 
category = ComponentCategory.SENSORS, nonVisible = true, iconName = "images/contactInfo.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.READ_CONTACTS")
@UsesLibraries(libraries = "funf.jar")
public class ContactInfo extends ProbeBase{
  
  private final String CONTACTINFO_PROBE = "edu.mit.media.funf.probe.builtin.COntactProbe";
  JsonParser jsonParser = new JsonParser();
  Context context;
  private final String TAG = "ContactInfo";
  private ContactProbe probe;
	//default settings for schedule 
  private final int SCHEDULE_INTERVAL = 43200; //read contact every 43200 seconds (every 12 hours)
  private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime
  
  //fields for contact
  // TODO: can be more
  private String email = "";
  private String emailType = "";
  private String phoneNum = "";
  private String phoneType = "";
  private String givenName = "";
  private String familyName = "";
  private String address = "";
  private String addressType = "";
  private String organizationName = "";
  private String notes = ""; 
  private String website = "";
  private String displayName = "";
 
  public ContactInfo(ComponentContainer container) {
	super(container);
	// TODO Auto-generated constructor stub
	form.registerForOnDestroy(this);
	context =  container.$context();
	mainUIThreadActivity = container.$context();
	Log.i(TAG, "Before create probe");
	JsonParser parse = new JsonParser();
	gson = new GsonBuilder().registerTypeAdapterFactory(
			FunfManager.getProbeFactory(mainUIThreadActivity)).create();
	JsonObject config = new JsonObject();

	probe = gson.fromJson(config, ContactProbe.class);

	interval = SCHEDULE_INTERVAL;
	duration = SCHEDULE_DURATION;

  }
  
  final Handler myHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {

	  IJsonObject data = (IJsonObject) msg.obj;
//	  Log.i(TAG, "Update component's varibles.....");

	  // doesn't make sense to hide contact data
/*	  if (privacySafe) {
	  }
*/	   
	  JsonArray contactDataArr = data.get("contactData").getAsJsonArray();
	  
	  for (JsonElement info: contactDataArr){
		String mimeType = info.getAsJsonObject().get("mimetype").getAsString();
//		Log.i(TAG, "mimeType:" + mimeType);
		if (mimeType.equals(Email.CONTENT_ITEM_TYPE))	  
		  getEmailInfo(info);
		if (mimeType.equals(Nickname.CONTENT_ITEM_TYPE))
		  getNicknameInfo(info);	  
		if (mimeType.equals(Phone.CONTENT_ITEM_TYPE))
		  getPhoneInfo(info);
		if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE))
		  getNameInfo(info);
		if (mimeType.equals(Organization.CONTENT_ITEM_TYPE))
		  getOrganizationInfo(info);
		if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE))
		  getAddressInfo(info);
		if (mimeType.equals(Website.CONTENT_ITEM_TYPE))
		  getWebsiteInfo(info);
//		if (mimeType.equals(GroupMembership.CONTENT_ITEM_TYPE))
//		  getGroupInfo(info);
		if (mimeType.equals(Note.CONTENT_ITEM_TYPE))
		  getNoteInfo(info);
		}
	  
 /* 
  *   // for clear text
	  {"contactData":[{"_id":14851,
		"data1":"wen2896@ms29.hinet.net",
		"data2":3,"data_version":1,"is_primary":1,"is_super_primary":0,
		"mimetype":"vnd.android.cursor.item/email_v2","raw_contact_id":2227},
		{"_id":14849,"data2":0,"data_version":0,"is_primary":0,"is_super_primary":0,
		 "mimetype":"vnd.android.cursor.item/nickname","raw_contact_id":2227},
		{"_id":14852,"data1":"Yale","data2":2,"data_version":0,"is_primary":0,
		"is_super_primary":0,"mimetype":"vnd.android.cursor.item/organization","raw_contact_id":2227},

		{"_id":14854,"data1":"20361231762","data2":2,"data_version":0,
		 "is_primary":0,"is_super_primary":0,"mimetype":"vnd.android.cursor.item/phone_v2","raw_contact_id":2227},

		{"_id":14847,"data1":"John Chen","data10":2,"data11":0,"data2":"John","data3":"Chen","data_version":0,"is_primary":0,"is_super_primary":0,"mimetype":"vnd.android.cursor.item/name","raw_contact_id":2227},

		{"_id":14846,"data_version":0,"is_primary":0,"is_super_primary":0,"mimetype":"vnd.android.cursor.item/photo","raw_contact_id":2227},

		{"_id":14853,"data1":6,"data_version":0,"is_primary":0,"is_super_primary":0,"mimetype":"vnd.android.cursor.item/group_membership","raw_contact_id":2227},

		{"_id":14848,"data1":"","data_version":0,"is_primary":0,"is_super_primary":0,"mimetype":"vnd.android.cursor.item/note","raw_contact_id":2227},

		{"_id":14850,"data8":4,"data9":5,"data_version":0,"is_primary":0,"is_super_primary":0,
		"mimetype":"vnd.com.google.cursor.item/
        contact_misc","raw_contact_id":2227}],
		
		"contact_id":2210,"display_name":"John Chen","in_visible_group":1,"last_time_contacted":-2023980349,"lookup":"4073i95","photo_id":0,
		"send_to_voicemail":0,"starred":0,"times_contacted":8,"timestamp":1379908694.689}*/
	  ContactInfoReceived(givenName, familyName, displayName, phoneNum, phoneType, email, emailType, address,
          addressType, organizationName, website, notes);

	}
  };	
  

  private DataListener listener = new DataListener() {
	@Override
	public void onDataCompleted(IJsonObject completeProbeUri, JsonElement arg1) {
	  // do nothing, CallLogProbe does not generate onDataCompleted event
	}

	@Override
	public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
//	  Log.i(TAG, "receive data of contactInfo");
	  //debug here.
//	  Log.i(TAG, "DATA: " + data.toString());
  	  
	  	
	  // save data to DB is enabledSaveToDB is true
	  if (enabledSaveToDB) {

		saveToDB(completeProbeUri, data);
	  }
	  Message msg = myHandler.obtainMessage();
	  msg.obj = data;
	  myHandler.sendMessage(msg);
	}
  };


  @SimpleFunction(description = "Enable contact information sensor to run once")
  @Override
  public void Enabled(boolean enabled) {
	// TODO Auto-generated method stub
	// TODO Auto-generated method stub
	JsonObject newConfig = null;
	if (this.enabled != enabled)
		this.enabled = enabled;
	newConfig = new JsonObject();
	if (enabled) {

		newConfig.addProperty("hideSensitiveData", privacySafe);
		probe = gson.fromJson(newConfig, ContactProbe.class);

		probe.registerListener(listener);

		Log.i(TAG, "run-once config for Contact:" + newConfig);
	} else {
		probe.unregisterListener(listener);
	}
  }

  private void getNicknameInfo(JsonElement info) {
	// TODO Auto-generated method stub
	
	
  }

  private void getEmailInfo(JsonElement info) {
	// TODO Auto-generated method stub
/*	{"_id":14851,
	  "data1":"someone@gmail.com",
	  "data2":3,"data_version":1,"is_primary":1,"is_super_primary":0,
	  "mimetype":"vnd.android.cursor.item/email_v2","raw_contact_id":2227}*/
//	Log.i(TAG, "Here at Email");
	this.email = info.getAsJsonObject().get(Email.ADDRESS) == null ? "" : 
	  			info.getAsJsonObject().get(Email.ADDRESS).getAsString();
	Integer type = info.getAsJsonObject().get(Email.TYPE) == null ? 0 :
	  			info.getAsJsonObject().get(Email.TYPE).getAsInt();
	this.emailType = getEmailTypeName(type);
  }
  

  private void getAddressInfo(JsonElement info){
//	Log.i(TAG, "Here at Address");
	this.address = info.getAsJsonObject().get(StructuredPostal.FORMATTED_ADDRESS) == null ? "":
	  info.getAsJsonObject().get(StructuredPostal.FORMATTED_ADDRESS).getAsString();
	
	Integer type = info.getAsJsonObject().get(StructuredPostal.TYPE) == null ? 0 :
	  info.getAsJsonObject().get(StructuredPostal.TYPE).getAsInt();
	
	this.addressType = getAddressTypeName(type);
  }
  
  private void getPhoneInfo(JsonElement info){
//	Log.i(TAG, "Here at PhoneInfo");
	this.phoneNum = info.getAsJsonObject().get(Phone.NUMBER) == null ? "" :
	  info.getAsJsonObject().get(Phone.NUMBER).getAsString();
	
	Integer type = info.getAsJsonObject().get(Phone.TYPE) == null ? 0 :
	  info.getAsJsonObject().get(Phone.TYPE).getAsInt();
	this.phoneType = getPhoneTypeName(type);
	
  }

  private void getNoteInfo(JsonElement info){
	this.notes = info.getAsJsonObject().get(Note.NOTE) == null ? "" :
	  info.getAsJsonObject().get(Note.NOTE).getAsString();
	
  }
  
  private void getOrganizationInfo(JsonElement info){
	/*	{"_id":14852,"data1":"Yale","data2":2,"data_version":0,"is_primary":0,
	  "is_super_primary":0,
	  "mimetype":"vnd.android.cursor.item/organization","raw_contact_id":2227},*/	
//	Log.i(TAG, "Here at Organization");
	this.organizationName = info.getAsJsonObject().get(Organization.COMPANY) == null ? "":
	  info.getAsJsonObject().get(Organization.COMPANY).getAsString();
	
  }
  
  private void getNameInfo(JsonElement info){
/*	{"_id":14847,"data1":"John Chen","data10":2,"data11":0,
	  "data2":"John","data3":"Chen","data_version":0,"is_primary":0,
	  "is_super_primary":0,"mimetype":"vnd.android.cursor.item/name","raw_contact_id":2227},*/	
//	Log.i(TAG, "Here at NameInfo");
	this.givenName = info.getAsJsonObject().get(StructuredName.GIVEN_NAME) == null ? "" :
	  info.getAsJsonObject().get(StructuredName.GIVEN_NAME).getAsString();
	this.familyName = info.getAsJsonObject().get(StructuredName.FAMILY_NAME) == null ? "" :
	  info.getAsJsonObject().get(StructuredName.FAMILY_NAME).getAsString();
	this.displayName = info.getAsJsonObject().get(StructuredName.DISPLAY_NAME) == null ? "" :
	  info.getAsJsonObject().get(StructuredName.DISPLAY_NAME).getAsString();
  }
  
  private void getWebsiteInfo(JsonElement info){
	this.website = info.getAsJsonObject().get(Website.URL) == null ? "" :
	  info.getAsJsonObject().get(Website.URL).getAsString();
  }
  /**
   * Indicates that the contact info has been received.
   */
  @SimpleEvent
  public void ContactInfoReceived(final String firstName, final String familyName,
                                  final String displayName, final String phoneNum,
                                  final String phoneType, final String email,
                                  final String emailType, final String address,
                                  final String addressType, final String organizationName,
                                  final String website, final String notes) {
	if (enabled || enabledSchedule) {

	  mainUIThreadActivity.runOnUiThread(new Runnable() {
		public void run() {
//		  Log.i(TAG, "ContactInfoReceived() is called");
		  EventDispatcher.dispatchEvent(ContactInfo.this, "ContactInfoReceived",
              firstName, familyName, displayName, phoneNum, phoneType, email, emailType,
              address, addressType, organizationName, website, notes);
		}
	  });

	}
  }

  @Override
  public void unregisterDataRequest() {
	// TODO Auto-generated method stub
	Log.i(TAG, "Unregistering contact info data requests.");
	//mBoundFunfManager.stopForeground(true);
	mBoundFunfManager.unrequestAllData2(listener);
	
  }

  @Override
  public void registerDataRequest(int interval, int duration) {
	
	Log.i(TAG, "Registering contact info requests.");
	JsonElement dataRequest = null;

	dataRequest = getDataRequest(interval, duration, CONTACTINFO_PROBE);

	((JsonObject) dataRequest).addProperty("hideSensitiveData", privacySafe);

	Log.i(TAG, "CallLog request: " + dataRequest.toString());

	mBoundFunfManager.requestData(listener, dataRequest);

  }
  
//  /**
//   * The phone number of the contact.
//   */
//  @SimpleProperty(description = "The phone number of the contact.")
//  public String PhoneNumber() {
//	Log.i(TAG, "returning the phone number of a contact: " + this.phoneNum);
//	return this.phoneNum;
//  }
//
//  /**
//   * The phone number type of the contact.
//   */
//  @SimpleProperty(description = "The phone number of the contact.")
//  public String PhoneNumberType() {
//	Log.i(TAG, "returning the phone number type of a contact: " + this.phoneType);
//	return this.phoneType;
//  }
//
//  /**
//   * The family name of a contact
//   */
//  @SimpleProperty(description = "The family name of a contact.")
//  public String FamilyName() {
//	Log.i(TAG, "The family name of a contact" + this.familyName);
//	return this.familyName;
//  }
//
//  /**
//   * The display name of a contact
//   */
//  @SimpleProperty(description = "The display name of a contact.")
//  public String DisplayName() {
//	Log.i(TAG, "The display name of a contact" + this.displayName);
//	return this.displayName;
//  }
//
//  /**
//   * The first name of a contact
//   */
//  @SimpleProperty(description = "The first name of a contact.")
//  public String FirstName() {
//	Log.i(TAG, "The first name of a contact" + this.givenName);
//	return this.givenName;
//  }
//
//  /**
//   * The email of a contact
//   */
//  @SimpleProperty(description = "The email of a contact.")
//  public String Email() {
//	Log.i(TAG, "The email of a contact" + this.email);
//	return this.email;
//  }
//
//  /**
//   * The email type of a contact
//   */
//  @SimpleProperty(description = "The email type of a contact.")
//  public String EmailType() {
//	Log.i(TAG, "The email type of a contact" + this.emailType);
//	return this.emailType;
//  }
//
//
//  /**
//   * The address of a contact
//   */
//  @SimpleProperty(description = "The address of a contact.")
//  public String Address() {
//	Log.i(TAG, "The address of a contact" + this.address);
//	return this.address;
//  }
//
//  /**
//   * The organization of a contact
//   */
//  @SimpleProperty(description = "The organization of a contact.")
//  public String Organization() {
//	Log.i(TAG, "The organization of a contact" + this.organizationName);
//	return this.organizationName;
//  }
//
//  /**
//   * The notes about a contact
//   */
//  @SimpleProperty(description = "The notes of a contact.")
//  public String Notes() {
//	Log.i(TAG, "The organization of a contact" + this.notes);
//	return this.notes;
//  }
//
//  /**
//   * The website of a contact
//   */
//  @SimpleProperty(description = "The notes of a contact.")
//  public String Website() {
//	Log.i(TAG, "The website of a contact" + this.website);
//	return this.website;
//  }
//
//  /**
//   * The address type of a contact
//   */
//  @SimpleProperty(description = "The address type of a contact.")
//  public String AddressType() {
//	Log.i(TAG, "The address of a contact" + addressType);
//	return addressType;
//  }

  private String getEmailTypeName(int emailTypeConst) {
	String type = "";
	switch (emailTypeConst) {
	case ContactsContract.CommonDataKinds.Email.TYPE_HOME://5
	  type = "HOME";
	  break;
	case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE://4
	  type = "MOBILE";
	  break;
	case ContactsContract.CommonDataKinds.Email.TYPE_WORK://2
	  type = "WORK";
	  break;
	case ContactsContract.CommonDataKinds.Email.TYPE_OTHER://3
	  type = "OTHER";
	  break;
	case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM://0
	  type = "CUSTOM";
	  break;

  }

	return type;
  }
  
  private String getAddressTypeName(int addressTypeConst) {
	String type = "";
	switch (addressTypeConst) {
	case StructuredPostal.TYPE_HOME:
	  type = "HOME";
	  break;
	case StructuredPostal.TYPE_WORK: 
	  type = "WORK";
	  break;
	case StructuredPostal.TYPE_OTHER:
	  type = "OTHER";
	  break;
   }
	return type;
  }

  private String getPhoneTypeName(int phoneTypeConst) {
	String type = "";
	switch (phoneTypeConst) {
	case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
	  type = "HOME";
	  break;
	case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
	  type = "MOBILE";
	  break;
	case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
	  type = "WORK";
	  break;
	case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
	  type = "FAX_WORK";
	  break;
	case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
	  type = "FAX_HOME";
	  break;  
	case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
	  type = "OTHER";
	break;    
	case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
	  type = "CUSTOM";
	  break;

	}
	return type;
  }
  
}
