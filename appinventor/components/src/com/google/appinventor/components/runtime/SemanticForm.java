package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ViewUtil;

import android.app.Activity;
import android.view.View;

/**
 * Semantic Form provides a layout in which contained form elements will be
 * used to generate structured data. This form is used in conjunction with
 * the SemanticWeb and LinkedData components.
 * 
 * @see SemanticWeb
 * @see LinkedDataStore
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
@DesignerComponent(version = YaVersion.SEMANTIC_WEB_COMPONENT_VERSION,
    description = "A layout that provides semantic enhancement of captured data.",
    category = ComponentCategory.ARRANGEMENTS)
@SimpleObject
public class SemanticForm extends AndroidViewComponent implements Component,
    ComponentContainer {
  /**
   * Stores a reference to the parent activity.
   */
  private final Activity context;

  /**
   * Linear layout used for arranging the contents of this form.
   */
  private final LinearLayout layout;

  /**
   * String storing the URI of the concept used to type instances created with this form.
   */
  private String concept;

  /**
   * Creates a new semantic form in the specified container.
   * @param container
   */
  public SemanticForm(ComponentContainer container) {
    super(container);
    context = container.$context();
    layout = new LinearLayout(context,
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL,
        ComponentConstants.EMPTY_HV_ARRANGEMENT_WIDTH,
        ComponentConstants.EMPTY_HV_ARRANGEMENT_HEIGHT);
    concept = "";
  }

  @Override
  public Activity $context() {
    return context;
  }

  @Override
  public Form $form() {
    return container.$form();
  }

  @Override
  public void $add(AndroidViewComponent component) {
    layout.add(component);
  }

  @Override
  public void setChildWidth(AndroidViewComponent component, int width) {
    ViewUtil.setChildWidthForVerticalLayout(component.getView(), width);
  }

  @Override
  public void setChildHeight(AndroidViewComponent component, int height) {
    ViewUtil.setChildHeightForVerticalLayout(component.getView(), height);
  }

  @Override
  public View getView() {
    return layout.getLayoutManager();
  }

  /**
   * Sets the concept URI to type objects encoded by this form.
   * @param uri
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CONCEPT_URI,
      defaultValue = "")
  @SimpleProperty
  public void ConceptURI(String uri) {
    concept = uri;
  }

  /**
   * Returns the concept URI for this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Specifies the class of objects created on the Semantic Web using the contents of this form.")
  public String ConceptURI() {
    return concept;
  }
}
