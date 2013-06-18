package com.google.appinventor.client.jquery;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Provides a mechanism for setting the position of a jQuery UI autocomplete
 * menu relative to its parent field.
 * @author ewpatton
 *
 */
public class AutoCompletePosition extends JavaScriptObject {

  /**
   * Protected constructor required by GWT.
   */
  protected AutoCompletePosition() {
    
  }

  /**
   * Creates a new AutoCompletePosition.
   * @see http://jqueryui.com/position/
   * @param my
   * @param at
   * @param collision
   * @return
   */
  public static final native AutoCompletePosition create(String my, String at, String collision)/*-{
    return {"my": my, "at": at, "collision": collision};
  }-*/;

}
