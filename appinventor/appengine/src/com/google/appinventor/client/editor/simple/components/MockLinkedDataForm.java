package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.gwt.user.client.ui.AbsolutePanel;

/**
 * Provides a mock component for modeling a semantic form in the app inventor's
 * client interface. 
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class MockSemanticForm extends MockContainer {
  public static final String PROPERTY_NAME_CONCEPTURI = "ConceptURI";
  public static final String TYPE = "SemanticForm";
  
  protected MockHVLayout myLayout;
  
  protected final AbsolutePanel layoutWidget;
  
  protected boolean initialized = false;
  protected String conceptUri = "";

  /**
   * Creates a new mock SemanticForm for the AI client interface.
   * @param editor
   */
  public MockSemanticForm(SimpleEditor editor) {
    super(editor, TYPE, images.semanticForm(), 
        MockHVArrangementHelper.makeLayout(ComponentConstants.LAYOUT_ORIENTATION_VERTICAL));
    myLayout = MockHVArrangementHelper.getLayout();
    rootPanel.setHeight("100%");
    layoutWidget = new AbsolutePanel();
    layoutWidget.setStylePrimaryName("ode-SimpleMockContainer");
    layoutWidget.add(rootPanel);
    
    initComponent(layoutWidget);
    initialized = true;
  }

  /**
   * Sets the concept URI for the mock form.
   * @param uri
   */
  public void setConceptUri(String uri) {
    conceptUri = uri;
  }

  @Override
  public void onPropertyChange(String propertyName, String newValue) {
    super.onPropertyChange(propertyName, newValue);
    
    if(propertyName.equals(PROPERTY_NAME_CONCEPTURI)) {
      setConceptUri(newValue);
    }
  }
}
