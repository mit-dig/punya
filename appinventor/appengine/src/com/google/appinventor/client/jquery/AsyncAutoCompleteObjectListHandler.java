package com.google.appinventor.client.jquery;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.xedge.jquery.client.js.JSHelper;
import com.xedge.jquery.client.js.JavaScriptObjectArray;
import com.xedge.jquery.ui.client.model.LabelValuePair;

/**
 * This class provides an alternative to the synchronous handlers provided
 * by gwt-jquery.
 * 
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public abstract class AsyncAutoCompleteObjectListHandler {
  /**
   * Holds a reference to the response function provided by the jQueryUI
   * autocomplete object.
   * 
   * @author Evan W. Patton <ewpatton@gmail.com>
   *
   */
  public static final class ResponseCallback {
    private final JavaScriptObject response;

    protected ResponseCallback(JavaScriptObject response) {
      this.response = response;
    }

    /**
     * JSNI implementation to fire the jQueryUI response handler to 
     * complete the request.
     * @param data A JavaScript array of objects, e.g. [{'label': '...', 'value': '...'}, ...]
     */
    private final native void doFinish(JavaScriptObjectArray<LabelValuePair> data) /*-{
      var response = this.@com.google.appinventor.client.jquery.AsyncAutoCompleteObjectListHandler$ResponseCallback::response;
      response(data);
    }-*/;

    /**
     * Used to pass data back to the jQueryUI autocomplete once the
     * asynchronous operation has completed. This <em>must</em> be called
     * even in the event of a failure, in which case passing an array of
     * zero length is sufficient.
     * @param data
     */
    public final void finish(List<LabelValuePair> data) {
      doFinish(JSHelper.convertObjectListToJSArray(data));
    }
  }

  /**
   * Implemented to perform the search feature of the autocomplete.
   * @param value
   * @param callback
   */
  public abstract void getData(String value, ResponseCallback callback);

  /**
   * Fires a handler in response to the user interacting with a jQueryUI
   * autocomplete object.
   * @param callback AsyncAutoCompleteObjectListHandler to call
   * @param value Value of the text box when the event fired
   * @param func The callback function supplied by the autocomplete where
   * data must be reported on completion of the operation.
   */
  static public void fire(AsyncAutoCompleteObjectListHandler callback, String value, JavaScriptObject func) {
    ResponseCallback response = new ResponseCallback(func);
    callback.getData(value, response);
  }
}
