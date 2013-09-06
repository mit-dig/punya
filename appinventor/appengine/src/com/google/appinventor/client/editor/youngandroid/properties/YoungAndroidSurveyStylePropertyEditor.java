// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.client.editor.youngandroid.properties;

import com.google.appinventor.client.widgets.properties.ChoicePropertyEditor;
import com.google.appinventor.components.common.ComponentConstants;

/**
 * Property editor for selecting the style of survey
 *
 * @author fuming@mit.edu
 */
public class YoungAndroidSurveyStylePropertyEditor extends ChoicePropertyEditor {

 
  public static final String TEXTBOX = ComponentConstants.SURVEY_STYLE_TEXTBOX + "";
 
  public static final String TEXTAREA = ComponentConstants.SURVEY_STYLE_TEXTAREA + "";
 
  public static final String MULTIPLECHOICE = ComponentConstants.SURVEY_STYLE_MULTIPLECHOICE + "";
  
  public static final String CHOOSELIST = ComponentConstants.SURVEY_STYLE_CHOOSELIST + "";
  
  public static final String SCALE= ComponentConstants.SURVEY_STYLE_SCALE + "";
  
  public static final String YESNO = ComponentConstants.SURVEY_STYLE_YESNO + "";
  
  public static final String CHECKBOX = ComponentConstants.SURVEY_STYLE_CHECKBOX+ "";
  

  private static final Choice[] tchoices = new Choice[] {
    new Choice("Textbox", TEXTBOX),
    new Choice("Textarea", TEXTAREA),
    new Choice("Multiple Choice", MULTIPLECHOICE),
    new Choice("Choose List", CHOOSELIST),
    new Choice("Checkbox", CHECKBOX),
    new Choice("Scale", SCALE),
    new Choice("Yesno", YESNO)
  };

  public YoungAndroidSurveyStylePropertyEditor() {
    super(tchoices);
  }
}
