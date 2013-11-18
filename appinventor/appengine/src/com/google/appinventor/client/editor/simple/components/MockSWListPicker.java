package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;

/**
 * Provides a mock Semantic Web list picker for the App Inventor editor.
 *
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class MockSWListPicker extends MockButtonBase {

  /**
   * Component type name.
   */
  public static final String TYPE = "SemanticWebListPicker";

  /**
   * Creates a new MockSWListPicker component.
   * 
   * @param editor  editor of source file the component belogs to
   */
  public MockSWListPicker(SimpleEditor editor) {
    super(editor, TYPE, images.listpicker());
  }
//
//  @Override
//  public void onPropertyChange(String propertyName, String newValue) {
//    super.onPropertyChange(propertyName, newValue);
//
//    if (propertyName.equals(PROPERTY_NAME_CONCEPT_URI)) {
//      setConceptUri(newValue);
//    }
//  }
}
