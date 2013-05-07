package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

public class MockLDVisualization extends MockVisibleComponent{

  /**
   * Component type name. (this has to be the same as the component name)
   */
  public static final String TYPE = "LinkedDataViz";

// Large icon image for use in designer. Smaller version is in the palette.
private final Image largeImage = new Image(images.ldchartbig());

/**
 * Creates a new MockLDVisualization component. Visualization for Linked Data
 * 
 * @param editor
 *            editor of source file the component belongs to
 */
public MockLDVisualization(SimpleEditor editor) {
  super(editor, TYPE, images.ldchart());

  // Initialize mock Survey UI
  SimplePanel ldChartWidget = new SimplePanel();
  ldChartWidget.setStylePrimaryName("ode-SimpleMockContainer");
  ldChartWidget.addStyleDependentName("centerContents");
  ldChartWidget.setWidget(largeImage);
  initComponent(ldChartWidget);
}

@Override
public int getPreferredWidth() {
  return largeImage.getWidth();
}

@Override
public int getPreferredHeight() {
  return largeImage.getHeight();
}

// override the width and height hints, so that automatic will in fact be
// fill-parent
@Override
int getWidthHint() {
  int widthHint = super.getWidthHint();
  if (widthHint == LENGTH_PREFERRED) {
    widthHint = LENGTH_FILL_PARENT;
  }
  return widthHint;
}

@Override
int getHeightHint() {
  int heightHint = super.getHeightHint();
  if (heightHint == LENGTH_PREFERRED) {
    heightHint = LENGTH_FILL_PARENT;
  }
  return heightHint;
}
}
