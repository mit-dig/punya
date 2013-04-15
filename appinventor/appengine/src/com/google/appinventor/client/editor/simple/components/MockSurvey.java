package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

public final class MockSurvey extends MockVisibleComponent{


	  /**
	   * Component type name.
	   */
	  public static final String TYPE = "Survey";

	// Large icon image for use in designer. Smaller version is in the palette.
	private final Image largeImage = new Image(images.surveybig());

	/**
	 * Creates a new MockSurvey component.
	 * 
	 * @param editor
	 *            editor of source file the component belongs to
	 */
	public MockSurvey(SimpleEditor editor) {
		super(editor, TYPE, images.survey());

		// Initialize mock Survey UI
		SimplePanel surveyWidget = new SimplePanel();
		surveyWidget.setStylePrimaryName("ode-SimpleMockContainer");
		surveyWidget.addStyleDependentName("centerContents");
		surveyWidget.setWidget(largeImage);
		initComponent(surveyWidget);
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