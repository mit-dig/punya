// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.components.runtime;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import android.os.Handler;
import android.os.Environment;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentConstants;


import org.json.JSONObject;

import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
 
import com.google.appinventor.components.common.YaVersion;

 
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.HttpsUploadService;
import com.google.appinventor.components.runtime.util.SdkLevel;
import com.google.appinventor.components.runtime.util.SensorDbUtil;
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
        category = ComponentCategory.SOCIAL,
        description = "Component for displaying surveys. This component makes use " +
        		"of Android WebView to display survey question. AI Developer " +
        		"could customize survey question and survey types. After the questions " +
        		"are answered, they will be saved into local database and upload to remote server." +
                "We suggest to put survey component in a standalone screen. The survey can be " +
                "triggered by two ways, 1) through the user interaction on the main screens, " +
                "2) through the user triggers the screen that contains this survey by tapping the " +
                "notification.")
@SimpleObject
@UsesLibraries(libraries = "funf.jar")
@UsesAssets(fileNames = "jquery.mobile.min.css," +
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
	
	public static final String SURVEY_HEADER = "edu.mit.csail.dig.survey";
    private static final String SURVEY_DBNAME = "__SURVEY_DB__";

    private static Form mainUI;
	private SharedPreferences prefs;
	private String exportRoot; // The exportRoot is the same with NameValueDatabaseService
	private final WebView webview;
	private final static String TEXTBOX = "textbox";
	private final static String TEXTAREA = "textarea";
	private final static String MULTIPLECHOICE = "multipleChoice"; //it's actually radio button
	private final static String CHOOSELIST = "chooselist"   ;
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

    private String exportFormat = NameValueDatabaseService.EXPORT_CSV;

    /*
     * We store the template in the App Inventor's assets and point WebViewer
     * to it (file:///android_asset/"). The templates used by the Survey components
     * are stored in buildserver/src/com/google/appinventor/buildserver/resources
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
    mainUI = container.$form();
    exportRoot =  new File(Environment.getExternalStorageDirectory(), mainUI.getPackageName()) +
                File.separator + "export";

    JsonParser parse = new JsonParser();
    webview = new WebView(container.$context());
    webview.getSettings().setJavaScriptEnabled(true);
    webview.setFocusable(true);
		webview.setVerticalScrollBarEnabled(true);
 
		container.$add(this);

		

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
		style =  TEXTBOX; //default style
		
		// see if the Survey is created by someone tapping on a notification! 
		// create the Survey and load it in the webviewer
		/* e.g. 
		 * value = {
		 * "style": "multipleChoice", 
		 * "question": "What is your favorite food"
		 * "options": ["apple", "banana", "strawberry", "orange"],
		 * "surveyGroup": "MIT-food-survey"
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

			Log.i(TAG, "Survey component got created");
		}

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
	

	/*
	 * This will load up the survey in the WebView. Need to set style, set question, and set options 
	 * use webView.loadData() to load from an HTML string
	 * 
	 */
	@SimpleFunction(description = "Set survey style, set question before" +  
			 "call LoadSurvey")
	public void LoadSurvey() throws IOException{
        Log.i(TAG,  "Before load data" );
		this.htmlContent = genSurvey();
		Log.i(TAG,  "HTML: " + this.htmlContent );
		//before loading we bind webview with SaveSurvey inner class to interface with js

		
		this.webview.addJavascriptInterface(new SaveSurvey(this.container.$form(), this.surveyGroup, 
											    this.style, this.question, this.options), "saveSurvey");
		
		//see http://pivotallabs.com/users/tyler/blog/articles/1853-android-webview-loaddata-vs-loaddatawithbaseurl-
		//http://myexperiencewithandroid.blogspot.com/2011/09/android-loaddatawithbaseurl.html
		this.webview.loadDataWithBaseURL("file:///android_asset/component/", this.htmlContent, "text/html", "UTF-8", null);

		Log.i(TAG,  "After load data" );
		
		
	}
	
	@SimpleProperty()
	public void SetSurveyGroup(String surveyGroup){
		this.surveyGroup = surveyGroup;
		
	}
	/**
	 * Sets the style of the survey
	 * @param question
	 */
	
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_SURVEY_STYLE, defaultValue = "1")
	@SimpleProperty(description = "Set the style of the survey with integer. 1 = textbox, 2 = textarea, " +
			"3 = multiplechoice, 4 = chooselist, 5 = checkbox, 6 = scale, 7 = yesno")
	public void SetStyle(int style) {
		Log.i(TAG, "the style is: " + style);
		switch (style) {	
		case ComponentConstants.SURVEY_STYLE_TEXTBOX:
			this.style = TEXTBOX;
			break;
		case ComponentConstants.SURVEY_STYLE_TEXTAREA:
			this.style = TEXTAREA;
			break;
		case ComponentConstants.SURVEY_STYLE_MULTIPLECHOICE:
			this.style = MULTIPLECHOICE; 
			break;
		case ComponentConstants.SURVEY_STYLE_CHOOSELIST:
			this.style = CHOOSELIST;
			break;
		case ComponentConstants.SURVEY_STYLE_CHECKBOX:
			this.style = CHECKBOX;
			break;
		case ComponentConstants.SURVEY_STYLE_SCALE:
			this.style = SCALE;
			break;
		case ComponentConstants.SURVEY_STYLE_YESNO:
			this.style = YESNO;
			break;
		default:
			this.style = TEXTBOX;

		}

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

    // in the html files for survey templates
	private static void close(String result){
		Log.i(TAG, "before closing the Activity" );

		mainUI.finishActivityWithTextResult(result);
	}
	
	/*
	 * use webView.loadData() to load from an HTML string
	 * 1. fetch the template according to survey style
	 * 2. replace survey's question and survey's options in the template
	 */
	private String genSurvey() throws IOException{
		Log.i(TAG, "the style is: " + style);
//		String templatePath  = "component" + File.separator + getTemplatePath(this.style);
		String templatePath  = "component" + File.separator + surveyTemplate.get(this.style);

		
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
    
    	int insertQuestionPos = sb.indexOf("Question", insertPos);
    	
    	sb.replace(insertQuestionPos, insertQuestionPos + "Question".length(), this.question);
    	
    	Log.i(TAG, "after replace question:" + sb.toString());
    	// B. generate options
    	// 1. find options block (depends on style)
    	// only "multipleChoice", "checkBox", and "chooseList" need to replace with options; 
    	// 2. For "scale" style, the first three options will be specifying the 
    	// min, max and default value of the scale
    	
    	if (this.style.equals(this.MULTIPLECHOICE) || this.style.equals(this.CHECKBOX)){
    		
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
		private String databasename = SURVEY_DBNAME; //use the same database as funf data

		private String surveyGroup;
		Context mContext;
		private String style;
		private String question;
		private ArrayList<String> options;

		public SaveSurvey(Context context, String surveyGroup, String style, 
										String question, ArrayList options){
			mContext = context;
			this.surveyGroup = surveyGroup;
			this.style = style;
			this.question = question;
			this.options = options;
 
		}

		private void closeApp(String result){
			Log.i(TAG, "Closing the app");
			Survey.close(result);
			
		}
		
		private void archiveData(){
			
			Intent i = new Intent(mContext, NameValueDatabaseService.class);
			Log.i(TAG, "archiving data....");
			i.setAction(DatabaseService.ACTION_ARCHIVE);
			i.putExtra(DatabaseService.DATABASE_NAME_KEY, databasename);
			mContext.startService(i);
			
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
			((JsonObject) surveyData).addProperty("probe", SURVEY_HEADER);
			((JsonObject) surveyData).add("timezoneOffset", new JsonPrimitive(localOffsetSeconds));
			((JsonObject) surveyData).addProperty("timestamp", System.currentTimeMillis()/1000);
				

			// write to DB
	     
			Bundle b = new Bundle();
			b.putString(NameValueDatabaseService.DATABASE_NAME_KEY,
					databasename);
			b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
			b.putString(NameValueDatabaseService.NAME_KEY, SURVEY_HEADER);

			b.putString(NameValueDatabaseService.VALUE_KEY,
					surveyData.toString());
			Intent i = new Intent(mContext, NameValueDatabaseService.class);
			i.setAction(DatabaseService.ACTION_RECORD);
			i.putExtras(b);
			mContext.startService(i);

            // we close the open activity with returned result
            closeApp(answer);
		}

		
	}

    /**
     *
     * @return
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public String DBName(){
        return SURVEY_DBNAME;

    }
    
    /*
     * Exporting Survey DB to External Storage
     */
    @SimpleFunction(description = "Export Survey results database as CSV files or JSON files." +
            "Input \"csv\" or \"json\" for exporting format.")
    public void Export(String format){
        if (format == NameValueDatabaseService.EXPORT_JSON)
            this.exportFormat = NameValueDatabaseService.EXPORT_JSON;
        else
            this.exportFormat = NameValueDatabaseService.EXPORT_CSV;
        //if the user input the wrong format, we will just use csv by default
        Log.i(TAG, "exporting data...at: " + System.currentTimeMillis());

        Bundle b = new Bundle();
        b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, SURVEY_DBNAME);
        b.putString(NameValueDatabaseService.EXPORT_KEY, format);
        Intent i = new Intent(mainUI, NameValueDatabaseService.class);
        i.setAction(DatabaseService.ACTION_EXPORT);
        i.putExtras(b);
        mainUI.startService(i);

    }

    @SimpleProperty(category = PropertyCategory.BEHAVIOR, 
    		description = "Get the export path for survey results")
    public String ExportFolderPath(){
        // the real export path is exportPath + "/" + exportformat
        return this.exportRoot + File.separator + this.exportFormat;

        
    }
    
    @SimpleFunction(description ="This will clean up the survey database on the smartphone")
    public void DeleteSurveyDB(){
        Intent i = new Intent(mainUI, NameValueDatabaseService.class);
        Log.i(TAG, "archiving data...at: " + System.currentTimeMillis());
        i.setAction(DatabaseService.ACTION_ARCHIVE);
        i.putExtra(DatabaseService.DATABASE_NAME_KEY, SURVEY_DBNAME);
        mainUI.startService(i);
    }


	  
	  
}
