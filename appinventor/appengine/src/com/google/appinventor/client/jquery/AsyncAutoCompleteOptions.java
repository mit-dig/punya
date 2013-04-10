package com.google.appinventor.client.jquery;

import com.xedge.jquery.ui.client.model.AutoCompleteOptions;

/**
 * This class provides the ability to handle an asynchronous request
 * to the jQueryUI Autocomplete, especially when a call to a GWT service
 * may be required to fulfill the request.
 * 
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class AsyncAutoCompleteOptions extends AutoCompleteOptions {
  /**
   * Protected constructor per JavaScriptObject in GWT
   */
  protected AsyncAutoCompleteOptions() {
  }

  /**
   * Creates a new AsyncAutoCompleteOptions object
   */
  public native static AsyncAutoCompleteOptions create() /*-{
    return {};
  }-*/;

  /**
   * Sets an asynchronous handler for the AutoCompleteOptions
   * @param callback
   */
  public final native void setAsyncSourceObjectListHandler(AsyncAutoCompleteObjectListHandler callback) /*-{
    this.source = function( request, response ) {
      @com.google.appinventor.client.jquery.AsyncAutoCompleteObjectListHandler::fire(Lcom/google/appinventor/client/jquery/AsyncAutoCompleteObjectListHandler;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(callback,request.term,response);
    };
  }-*/;
}
