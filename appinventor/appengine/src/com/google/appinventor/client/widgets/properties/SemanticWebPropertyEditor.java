package com.google.appinventor.client.widgets.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.jquery.AsyncAutoCompleteObjectListHandler;
import com.google.appinventor.client.jquery.AsyncAutoCompleteOptions;
import com.google.appinventor.shared.rpc.semweb.SemWebServiceAsync;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TextBox;
import com.xedge.jquery.client.JQEvent;
import com.xedge.jquery.client.JQuery;
import com.xedge.jquery.ui.client.JQueryUI;
import com.xedge.jquery.ui.client.handlers.AutoCompleteUIEventResultWithItemHandler;
import com.xedge.jquery.ui.client.model.LabelValuePair;

/**
 * SemanticWebPropertyEditor provides a user interface element supported
 * by jQueryUI and the SemWebService to provide search for class and property
 * information in configured ontologies. This information is used to
 * generate linked data from fields implementing the {@link LDComponent} interface.
 * 
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class SemanticWebPropertyEditor extends PropertyEditor {

  /**
   * Enum used to represent the type of search the editor will
   * make against the server-side ontologies.
   * @author Evan W. Patton <ewpatton@gmail.com>
   *
   */
  public static enum SemanticWebSearchType {
    CONCEPT_URI,
    PROPERTY_URI
  }

  /**
   * Text box widget used in the interface
   */
  protected final TextBox textEdit;

  /**
   * URI for the field
   */
  protected String uri = "";

  /**
   * Search type executed by this editor
   */
  protected final SemanticWebSearchType type;

  /**
   * Constructs a new property editor for the given search type.
   * @param type
   */
  public SemanticWebPropertyEditor(SemanticWebSearchType type) {
    this.type = type;

    // configure widget
    textEdit = new TextBox();
    initWidget(textEdit);
    setHeight("2em");

    // set up jQuery and jQueryUI
    final JQueryUI ui = JQueryUI.getJQueryUI(JQuery.select(textEdit.getElement()));
    AsyncAutoCompleteOptions options = AsyncAutoCompleteOptions.create();
    options.setAsyncSourceObjectListHandler(new AsyncAutoCompleteObjectListHandler() {
      @Override
      public void getData(String value, final ResponseCallback callback) {
        // continuation for when the service returns data
        AsyncCallback<List<Map<String, String>>> continuation = new AsyncCallback<List<Map<String, String>>>() {
          @Override
          public void onFailure(Throwable arg0) {
            // jQueryUI's autocomplete feature must be called even if there
            // is an error to ensure consistent state
            callback.finish(new ArrayList<LabelValuePair>());
          }
          @Override
          public void onSuccess(List<Map<String, String>> arg0) {
            // convert the maps into LabelValuePairs to satisfy gwt-jquery.
            List<LabelValuePair> pairs = new ArrayList<LabelValuePair>();
            for(Map<String, String> i : arg0) {
              String label = i.get("label");
              String value = i.get("value");
              pairs.add(LabelValuePair.create(label, value));
            }
            callback.finish(pairs);
          }
        };
        // get a reference to the SemWebService and make the appropriate search
        SemWebServiceAsync service = Ode.getInstance().getSemanticWebService();
        if(SemanticWebPropertyEditor.this.type == SemanticWebSearchType.CONCEPT_URI) {
          service.searchClasses(value, continuation);
        } else {
          service.searchProperties(value, continuation);
        }
      }
    });
    options.setSelectHandler(new AutoCompleteUIEventResultWithItemHandler() {
      @Override
      public boolean eventComplete(JQEvent event, JQuery currentJQuery,
          LabelValuePair item) {
        uri = item.getValue();
        property.setValue(uri);
        return true;
      }
    });
    options.setMinLength(3);
    ui.autocomplete(options);
  }

  @Override
  protected void updateValue() {
    textEdit.setText(property.getValue());
  }

}
