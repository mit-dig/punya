// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONObject;

import com.google.appinventor.components.annotations.DesignerComponent;
 
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesTemplates;
import com.google.appinventor.components.common.ComponentCategory;
 
import com.google.appinventor.components.common.YaVersion;

 
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.HttpsUploadService;
import com.google.appinventor.components.runtime.util.YailList;
 
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.time.DecimalTimeUnit;
import java.math.BigDecimal;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.util.Log;


/**
 * Component for displaying surveys
 * This component makes use of Android WebView to display survey question. AI Developer could
 * customize survey question and survey types. After the questions are answered, 
 * they will be saved into local database and upload to remote server (Trust Framework).
 * For simplicity, there's only one question in one survey component
 *
 * @author fuming@mit.mit (Fuming Shih)
 */

@DesignerComponent(version = YaVersion.SURVEY_COMPONENT_VERSION,
        category = ComponentCategory.FUNF,
        description = "Component for displaying surveys. This component makes use " +
        		"of Android WebView to display survey question. AI Developer " +
        		"could customize survey question and survey types. After the questions " +
        		"are answered, they will be saved into local database and upload to remote server.")
@SimpleObject
@UsesTemplates(templateNames = "jquery.mobile.min.css," +
		"jquery.min.js," +
		"jquery.mobile.min.js," +
		"checkbox.html," +
		"chooselist.html," +
		"multipleChoice.html," +
		"scale.html," +
		"textarea.html," +
		"textbox.html," +
		"yesno.html")
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, " +
									"android.permission.INTERNET")
public class Survey extends AndroidViewComponent{
	
	public static final String SURVEY_HEADER = "edu.mit.csail.dig.esm.";
	
	private static Activity mainUI;
	private SharedPreferences prefs;
	
	private final WebView webview;
	private final static String TEXTBOX = "textbox";
	private final static String TEXTAREA = "textarea";
	private final static String MULTIPLECHOICE = "multipleChoice"; //it's actually radio button
	private final static String CHOOSELIST = "chooselist";
	private final static String CHECKBOX = "checkbox";
	private final static String SCALE = "scale";
	private final static String YESNO = "yesno";
	
	private static final String TAG = "Survey";
	
	private static final boolean DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY = true;
	
	private  String style = "";
	private  String htmlContent = "";
	private  String question = "";
	private  String surveyGroup = "";
	private  ArrayList<String> options = new ArrayList<String>();
	private  String initValues = "";
	
	// for data uploading config.
  private Calendar calendar = Calendar.getInstance(Locale.getDefault());
  private BigDecimal localOffsetSeconds = BigDecimal.valueOf(
      calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET),
      DecimalTimeUnit.MILLI);
	
	//store mapping to survey template (survey templates are store current in the asset folder)
	private static final HashMap <String, String> surveyTemplate = new HashMap<String, String>();
	static{
		surveyTemplate.put(TEXTBOX, getTemplatePath(TEXTBOX));
		surveyTemplate.put(YESNO, getTemplatePath(YESNO));
		surveyTemplate.put(TEXTAREA, getTemplatePath(TEXTAREA));
		surveyTemplate.put(MULTIPLECHOICE, getTemplatePath(MULTIPLECHOICE));
		surveyTemplate.put(CHOOSELIST, getTemplatePath(CHOOSELIST));
		surveyTemplate.put(CHECKBOX, getTemplatePath(CHECKBOX));
		surveyTemplate.put(SCALE, getTemplatePath(SCALE));
		
	}
	/*
	 * We store the template in the App Inventor's asset and point WebViewer to it (file:///android_asset/")
	 * However we require the user to manually upload the templates to as assets, including html, css, and js files. 
	 * TODO: could we put all the file in a folder that will end up in the resource folder of an Android project?
	 */
	private static String getTemplatePath(String name){
		return name + ".html"; 
		//return "file:///android_asset/" + name + ".html";
	}
	
	/**
	 * Creates a new Survey component.
	 * 
	 * @param container
	 *            container the component will be placed in
	 * @throws IOException 
	 */
	public Survey(ComponentContainer container) throws IOException {
		super(container);
		mainUI = container.$context();
		JsonParser parse = new JsonParser();
		webview = new WebView(container.$context());
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setFocusable(true);
		webview.setVerticalScrollBarEnabled(true);
 
		container.$add(this);
		prefs = getSystemPrefs(mainUI);
		

		webview.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus()) {
						v.requestFocus();
					}
					break;
				}
				return false;
			}
		});

		// set the initial default properties. Height and Width
		// will be fill-parent, which will be the default for the web viewer.

		Width(LENGTH_FILL_PARENT);
		Height(LENGTH_FILL_PARENT);
		
		//set default survey style
		style =  MULTIPLECHOICE; //default style
		
		// see if the Survey is created by someone tapping on a notification! 
		// create the Survey and load it in the webviewer
		/* e.g. 
		 * value = {
		 * "style": "multipleChoice", 
		 * "question": "What is your favorite food"
		 * "options": ["apple", "banana", "strawberry", "orange"],
		 * "surveyGroup": "MIT-food-survey"
		 * 
		 * 
		 * }
		 * 
		 */
		initValues = container.$form().getSurveyStartValues();
		Log.i(TAG, "startVal Suvey:" + initValues.toString());
		if (initValues!=""){
			
			JsonObject values = (JsonObject)parse.parse(initValues);          
			this.style = values.get("style").getAsString();
			this.question = values.get("question").getAsString();
			this.surveyGroup = values.get("surveyGroup").getAsString();
			ArrayList<String> arrOptions = new ArrayList<String>();
			JsonArray _options = values.get("options").getAsJsonArray();
			for (int i = 0; i < _options.size(); i++){
				arrOptions.add(_options.get(i).getAsString());
			}
			
			this.options = arrOptions;
			
			LoadSurvey();
			
		}
		
		
//		webview.loadData(data, mimeType, encoding);
	}
	@Override
	public View getView() {
		// TODO Auto-generated method stub
		return webview;
	}
	
	
	// Components don't normally override Width and Height, but we do it here so
	// that
	// the automatic width and height will be fill parent.
	@Override
	@SimpleProperty()
	public void Width(int width) {
		if (width == LENGTH_PREFERRED) {
			width = LENGTH_FILL_PARENT;
		}
		super.Width(width);
	}

	@Override
	@SimpleProperty()
	public void Height(int height) {
		if (height == LENGTH_PREFERRED) {
			height = LENGTH_FILL_PARENT;
		}
		super.Height(height);
	}
	
	
	private void prepareComponents(){
		/*
		 * let's try something 
		 */
		
		VerticalArrangement verticalArr = new VerticalArrangement(container);
		verticalArr.Width(Component.LENGTH_FILL_PARENT); // at ViewUtil.java

		container.$add(verticalArr);
		
		Label qLabel = new Label(verticalArr);
		qLabel.BackgroundColor(Component.COLOR_BLACK); // color is in Component interface
		qLabel.Width(Component.LENGTH_FILL_PARENT);
		qLabel.Text("");
		verticalArr.$add(qLabel);
		com.google.appinventor.components.runtime.Button btn = new Button(verticalArr);
		
//		btn.getView().setOnClickListener(ocl); // this is my own btn onclick

		
		
	}
	
	
	
	
	/*
	 * This will load up the survey in the WebView. Need to set style, set question, and set options 
	 * use webView.loadData() to load from an HTML string
	 * 
	 */
	@SimpleFunction(description = "Need to set survey style, set question before" +  
			 "call LoadSurvey")
	public void LoadSurvey() throws IOException{
		
		this.htmlContent = genSurvey();
		Log.i(TAG,  "HTML: " + this.htmlContent );
		//before loading we bind webview with SaveSurvey inner class to interface with js
		
		/*
		 *  SaveSurvey(Context context, String surveyGroup, String style, 
										String question, ArrayList options){
		 */
		
		this.webview.addJavascriptInterface(new SaveSurvey(this.container.$form(), this.surveyGroup, 
											    this.style, this.question, this.options), "saveSurvey");
		
		//see http://pivotallabs.com/users/tyler/blog/articles/1853-android-webview-loaddata-vs-loaddatawithbaseurl-
		//http://myexperiencewithandroid.blogspot.com/2011/09/android-loaddatawithbaseurl.html
		this.webview.loadDataWithBaseURL("file:///android_asset/", this.htmlContent, "text/html", "UTF-8", null);
		
		Log.i(TAG,  "After load data" );
		
		
	}
	
	@SimpleProperty()
	public void SetSurveyGroup(String surveyGroup){
		this.surveyGroup = surveyGroup;
		
	}
	
	
	@SimpleProperty()
	public void SetStyle(String style){
		this.style = style;
		
	}
	
	@SimpleProperty()
	public void SetQuestion(String question){
		this.question = question;
		
	}
	/*
	 * This is for survey that is of type: MultipleChoice, ChooseList, CheckBox and Scale
	 * Note that for Scale, only three options will 
	 */
	@SimpleFunction(
			description = "For survey style MultipleChoice, ChooseList, CheckBox and ScalePass" +
					"use this to pass in options for survey answers. Note for Scale, " +
					"only three options should be passed in and in the order of \"min\", \"max\", " +
					"\"default\" value of the scale")
	public void SetOptions(YailList options){
		String[] objects = options.toStringArray();
		
	    for (int i = 0; i < objects.length; i++) {
	    	this.options.add(objects[i]);
	    }

	}
	
	private String genOptions(){
		/*
		 * For multiplechoice
		 * <input id="radio1" name="" value="" type="radio" />
         *     <label for="radio1">
         *      Streeful
         *     </label>
         * For checkBox:
         * <input id="checkbox1" name="" type="checkbox" />
         *      <label for="checkbox1">
         *       Apple
         *      </label>
         * 
         * For chooselist:
         * 
         * <option value="option1">
         *   Option 1
         * </option>
		 */
		StringBuilder optHtml = new StringBuilder();
		String optMFormat = "<input id=\"radio%d\" name=\"ans\" value=\"%s\" type=\"radio\" />\n";
		String optMLableFormat = "<label for=\"radio%d\"> %s </label> \n";

		if (this.options.isEmpty()){
			return optHtml.toString(); 
			
		}
		if (this.style.equals(this.MULTIPLECHOICE)||this.style.equals(this.YESNO)) {
			int i = 1;
			for (String option : this.options) {
				optHtml.append(String.format(optMFormat, i, option));
				optHtml.append(String.format(optMLableFormat, i, option));
				i++;
			}

		}

		String optCFormat = "<input id=\"checkbox%d\" name=\"ans\" value=\"%s\" type=\"checkbox\" />\n";
		String optCLableFormat = "<label for=\"checkbox%d\"> %s </label> \n";
		Log.i(TAG, "before entering checkbox");
		if (this.style.equals(this.CHECKBOX)) {
			int j = 1;
			for (String option : this.options) {
				optHtml.append(String.format(optCFormat, j, option));
				optHtml.append(String.format(optCLableFormat, j, option));
				j++;
			}

		}
		String optLFormt = "<option value=\"option%d\"> %s </option>";
		if (this.style.equals(this.CHOOSELIST)) {
			int k = 1;
			for (String option : this.options) {
				optHtml.append(String.format(optLFormt, k, option));

				k++;
			}

		}
		Log.i(TAG, "gen options for:(" + this.style + ")" + optHtml.toString());
		return optHtml.toString();
		
	}
	
	private static void close(){
		Log.i(TAG, "before closing the Activity" );
		mainUI.finish();
	}
	
	/*
	 * use webView.loadData() to load from an HTML string
	 * 1. fetch the template according to survey style
	 * 2. replace survey's question and survey's options in the template
	 */
	private String genSurvey() throws IOException{
		String templatePath  = getTemplatePath(this.style);
		
		BufferedInputStream in = new BufferedInputStream(container.$context().getAssets().open(templatePath));


		//BufferedInputStream in = new BufferedInputStream(MediaUtil.openMedia(container.$form(), templatePath));
		
		//read it with BufferedReader
    	BufferedReader br
        	= new BufferedReader(new InputStreamReader(in));
		
		StringBuilder sb = new StringBuilder();
		String line;
    	while ((line = br.readLine()) != null) {
    		sb.append(line);
    	}
    	
    	Log.i(TAG,"Before replace:" + sb.toString());
    	
    	//A. generate question
    	//find question block start with <h1 id="_txt_qt">
    	String questionBlk = "<h1 id=\"_txt_qt\">";
    	int insertPos = sb.indexOf(questionBlk) + questionBlk.length();
    
    	int insertQuestionPos = sb.indexOf("Question_content", insertPos);
    	
    	sb.replace(insertQuestionPos, insertQuestionPos + "Question_content".length(), this.question);
    	
    	Log.i(TAG, "after replace question:" + sb.toString());
    	// B. generate options
    	// 1. find options block (depends on style)
    	// only "multipleChoice", "checkBox", and "chooseList" need to replace with options; 
    	// 2. For "scale" style, the first three options will be specifying the 
    	// min, max and default value of the scale
    	
    	if (this.style.equals(this.MULTIPLECHOICE) || this.style.equals(this.CHECKBOX)|| this.style.equals(this.YESNO)){
    		
    		int startPos = sb.indexOf("</legend>") + "</legend>".length();
    		int endPos = sb.indexOf("</fieldset>");
    		Log.i(TAG, "before replace options:" );
    		sb.replace(startPos, endPos, genOptions()); //replace with the filled-in options
    		
    	}
    	if (this.style.equals(this.CHOOSELIST)){
    		int startPos = sb.indexOf("<select name=\"\">") + "<select name=\"\">".length();
    		int endPos = sb.indexOf("</select>");
    		
    		sb.replace(startPos, endPos, genOptions());
    	}
    	
    	if (this.style.equals(this.SCALE)){
    		
    		if(!this.options.isEmpty() && this.options.size() == 3){
    			//replace min
				int sliderPos = sb.indexOf("input name=\"slider\"");
				int startPosOfMin = sb.indexOf("min=\"1\"", sliderPos
						+ "input name=\"slider\"".length());
				// example: min="1" 
				sb.replace(startPosOfMin, startPosOfMin + 7, "min=\"" + this.options.get(0) + "\"");  
				// replace max
				// example: replace max="10" to max="100"
				int startPosOfMax = sb.indexOf("max=\"10\"", sliderPos
						+ "input name=\"slider\"".length());
				
				sb.replace(startPosOfMax, startPosOfMax + 8, "max=\"" + this.options.get(1) + "\"");
				
				// replace default initial scale
				// example: value="5"
				int startPosOfDefault = sb.indexOf("value=\"5\"", sliderPos
						+ "input name=\"slider\"".length());
				
				sb.replace(startPosOfDefault, startPosOfDefault + 9, "value=\"" + this.options.get(2) + "\"");
				
				
    		}else{
    			; //do nothing, use the template
    		}
    		
    	}

    	return sb.toString();
 
	}
	
	private SharedPreferences getSystemPrefs(Context context) {
		// TODO Currently survey is using the same sharedPreference file to get accessToken saved by the probes
		// this won't work if the Survey component is not in the same app as the probe component
		// We rely on probe component to get accessToken through ProbeBase.Authorize()
		return context.getSharedPreferences(ProbeBase.class.getName()
				+ "_system", android.content.Context.MODE_PRIVATE);
	}
	
 
	
	/*
	 * 1. This inner class is to create an interface between javascript in WebView and Android
	 * We assume that for most of the cases, the survey will be used in experience sampling. 
	 * So survey results are stored together in the same db as the sensor probe. The results will 
	 * be archived and uploaded to remote DB according how the user configure the Probe. 
	 * 
	 * 2. We also assume that other probes on the app has already obtained accessToken 
	 *    and saved locally (in prefs).
	 */
	public class SaveSurvey{
		private String databasename = ProbeBase.PROBE_BASE_NAME; //use the same database as funf data

		private String surveyGroup;
		Context mContext;
		private String style;
		private String question;
		private ArrayList<String> options;
		private String answer;
		private String accessToken;
		//TODO: for testing, will make this a parament to SaveSurvey in the future
		private final String UPLOAD_URL = "http://air.csail.mit.edu:8002/connectors/set_funf_data";
		public SaveSurvey(Context context, String surveyGroup, String style, 
										String question, ArrayList options){
			mContext = context;
			this.surveyGroup = surveyGroup;
			this.style = style;
			this.question = question;
			this.options = options;
 
		}
		
		
		public void closeApp(){
			Log.i(TAG, "Closing the app");
			Survey.close();
			
		}
		
		private void archiveData(){
			
			Intent i = new Intent(mContext, NameValueDatabaseService.class);
			Log.i(TAG, "archiving data....");
			i.setAction(DatabaseService.ACTION_ARCHIVE);
			i.putExtra(DatabaseService.DATABASE_NAME_KEY, databasename);
			mContext.startService(i);
			
		}
		
		private void uploadData(boolean wifiOnly) {
			archiveData();
			String archiveName = databasename;
			// we also get the uploadURL from other probe component
			String uploadUrl = prefs.getString("uploadURL", UPLOAD_URL);
			Intent i = new Intent(mContext, HttpsUploadService.class);
			i.putExtra(UploadService.ARCHIVE_ID, archiveName);
			i.putExtra(UploadService.REMOTE_ARCHIVE_ID, uploadUrl);
			i.putExtra(UploadService.NETWORK,
					(wifiOnly) ? UploadService.NETWORK_WIFI_ONLY
							: UploadService.NETWORK_ANY);
			mContext.startService(i);

			// getSystemPrefs().edit().putLong(LAST_DATA_UPLOAD,
			// System.currentTimeMillis()).commit();
		}

		public void saveResponse(String answer) {
			Log.i(TAG, "saveResponse is called");
			final long timestamp = System.currentTimeMillis() / 1000;
			final String style = this.style;
			final String surveyGroup = this.surveyGroup;
			final String question = this.question;
			final ArrayList<String> options = this.options;

			 
			Log.i(TAG, "survey answer:" + answer.toString());

			// prepare the json object for survey value 

			JsonElement surveyData = new JsonObject();

			((JsonObject) surveyData).addProperty("style", style);
			((JsonObject) surveyData).addProperty("surveyGroup", surveyGroup);
			((JsonObject) surveyData).addProperty("question", question);
			((JsonObject) surveyData).addProperty("style", style);

			JsonElement optionsData = new JsonArray();

			for (String option : options) {
				JsonPrimitive p = new JsonPrimitive(option);
				((JsonArray) optionsData).add(p);
			}
			((JsonObject) surveyData).add("options", optionsData);

			
			// if the answer is from checkbox style, then we have multiple selections of answers
			if (style.equals(Survey.CHECKBOX)) {
				JsonElement surveyAnswers = new JsonArray();

				for (String ans : answer.split(",")) {
					JsonPrimitive p = new JsonPrimitive(ans);
					((JsonArray) surveyAnswers).add(p);
				}

				((JsonObject) surveyData).add("answer", surveyAnswers);

			} else {
				((JsonObject) surveyData).addProperty("answer", answer);

			}
			((JsonObject) surveyData).addProperty("probe", SURVEY_HEADER + style);
			((JsonObject) surveyData).add("timezoneOffset", new JsonPrimitive(localOffsetSeconds));
			((JsonObject) surveyData).addProperty("timestamp", System.currentTimeMillis()/1000);
				

			// write to DB
	     
      Bundle b = new Bundle();
      b.putString(NameValueDatabaseService.DATABASE_NAME_KEY,
          databasename);
      b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
      b.putString(NameValueDatabaseService.NAME_KEY, SURVEY_HEADER
          + style);
    
			b.putString(NameValueDatabaseService.VALUE_KEY,
					surveyData.toString());
			Intent i = new Intent(mContext, NameValueDatabaseService.class);
			i.setAction(DatabaseService.ACTION_RECORD);
			i.putExtras(b);
			mContext.startService(i);
			

			// let's test this with uploading the data to trust server.
			// To be able to upload the app needs to obtain the "accessToken" first and save it in SharedPreferences
			// Survey component should not worry about how to obtain the accessToken. 
			// 1. Check if the app has accessToken in SharedPreferences
			// 2. Try to upload 
			accessToken = prefs.getString("accessToken", "");
			
			Log.i(TAG, "accessToken:" + accessToken);
			if(accessToken != ""){
				uploadData(DEFAULT_DATA_UPLOAD_ON_WIFI_ONLY);
			}

		}
		
		
		
		
	}
 
	  
	  
	  
}
