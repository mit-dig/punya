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

  /**
   * Sets the position of the autocomplete menu.
   * @see http://jqueryui.com/position/
   * @param position A jQueryUI position configuration.
   */
  public final native void setPosition(AutoCompletePosition position) /*-{
    this.position = position;
  }-*/;

  public final native void cancelNullValueSelection() /*-{
    this.select = function( e, ui ) {
      var cancel = ui.item.value == "";
      if ( cancel ) e.preventDefault();
      return !cancel;
    };
    this._renderItem = function( ul, item ) {
      console.log("in _renderItem");
      return $( "<li>" )
          .attr( "data-value", item.value )
          .css( "font-style", item.value == "" ? "italic" : "normal" )
          .append( $( "<a>" ).text( item.label ) )
          .appendTo( ul );
    };
    this._renderMenu = function( ul, items ) {
      console.log("in _renderMenu");
      var that = this;
      $.each( items, function( index, item ) {
        that._renderItemData( ul, item );
      });
      if ( items.length == 1 && items[0].value == '' ) {
        $( ul ).find( "li" ).css("font-style", "italic");
      }
    };
  }-*/;
}
