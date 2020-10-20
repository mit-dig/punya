package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesActivities;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.SWListActivity.LabeledUri;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.RdfUtil;
import com.google.appinventor.components.runtime.util.RdfUtil.Solution;
import com.hp.hpl.jena.query.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Provides a list picker backed by the results of a SPARQL query.
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
@DesignerComponent(version = YaVersion.LINKED_DATA_LISTPICKER_COMPONENT_VERSION,
    category = ComponentCategory.LINKEDDATA,
    description = "Provides a list picker backed by the results of a SPARQL query, limit to first 100 results.")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "xercesImpl.jar," + "slf4j-android.jar," + 
    "jena-iri.jar," + "jena-core.jar," + "jena-arq.jar")
@UsesActivities(activities = {
    @ActivityElement(name = "com.google.appinventor.components.runtime.SWListActivity",
        configChanges = "orientation|keyboardHidden",
        screenOrientation = "behind")
})
public class LinkedDataListPicker extends Picker implements ActivityResultListener, Deleteable, LDComponent {

  private static final String LOG_TAG = LinkedDataListPicker.class.getSimpleName();
  private static final String SWLIST_ACTIVITY_CLASS = SWListActivity.class.getName();
  static final String SWLIST_ACTIVITY_ARG_NAME = SWLIST_ACTIVITY_CLASS + ".list";
  static final String SWLIST_ACTIVITY_RESULT_URI = SWLIST_ACTIVITY_CLASS + ".selectionUri";
  static final String SWLIST_ACTIVITY_RESULT_LABEL = SWLIST_ACTIVITY_CLASS + ".selectionLabel";
  static final String SWLIST_ACTIVITY_RESULT_INDEX = SWLIST_ACTIVITY_CLASS + ".index";
  static final String SWLIST_ACTIVITY_ANIM_TYPE = SWLIST_ACTIVITY_CLASS + ".anim";
  private String endpointUrl;
  private String selectionUri;
  private String selectionLabel;
  private int selectionIndex;
  private boolean isSubject;
  private String conceptUri;
  private String propertyUri;
  private String relationUri;
  private List<LabeledUri> items;
  private final Form form;
  private volatile boolean initialized = false;

  public LinkedDataListPicker(ComponentContainer container) {
    super(container);
    Log.d(LOG_TAG, "Constructing SemanticWebListPicker");
    selectionUri = "";
    selectionLabel = "";
    selectionIndex = 0;
    isSubject = false;
    conceptUri = "";
    propertyUri = "";
    relationUri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    items = new ArrayList<LabeledUri>();
    form = container.$form();
  }

  // ActivityResultListener implementation

  @Override
  public void resultReturned(int requestCode, int resultCode, Intent data) {
    if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {
      if (data.hasExtra(SWLIST_ACTIVITY_RESULT_URI)) {
        selectionUri = data.getStringExtra(SWLIST_ACTIVITY_RESULT_URI);
        selectionLabel = data.getStringExtra(SWLIST_ACTIVITY_RESULT_LABEL);
      } else {
        selectionUri = "";
        selectionLabel = "";
      }
      selectionIndex = data.getIntExtra(SWLIST_ACTIVITY_RESULT_INDEX, 0);
      AfterPicking();
    }
  }

  // Deleteable implementation

  @Override
  public void onDelete() {
    container.$form().unregisterForActivityResult(this);
  }

  @Override
  protected Intent getIntent() {
    Log.d(LOG_TAG, "Creating intent");
    Intent intent = new Intent();
    intent.setClassName(container.$context(), SWLIST_ACTIVITY_CLASS);
    Bundle bundle = new Bundle();
    bundle.putBinder("binder", new SWListActivity.DataSource() {
      @Override
      void performQuery(String query, Completion completion) {
        ArrayList<LabeledUri> filteredItems = new ArrayList<>();
        int i = 0;
        boolean first = true;
        while (i < items.size()) {
          for (; i < items.size(); i++) {
            if (items.get(i).getLabel().contains(query)) {
              filteredItems.add(items.get(i));
            }
            if (filteredItems.size() % 100 == 0) {
              break;
            }
          }
          Log.d(LOG_TAG, "Sending batch of " + filteredItems.size() + " uris to activity.");
          completion.onResultsAvailable(filteredItems, first);
          first = false;
          filteredItems.clear();
        }
        completion.done();
      }
    });
    intent.putExtra(".source", bundle);
    //intent.putExtra(SWLIST_ACTIVITY_ARG_NAME, items.toArray(new LabeledUri[] {}));
    String openAnim = container.$form().getOpenAnimType();
    intent.putExtra(SWLIST_ACTIVITY_ANIM_TYPE, openAnim);
    return intent;
  }

  /**
   * Sets the endpoint URL to find instances to populate the list picker.
   * @param url
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_TEXT,
      defaultValue = "")
  @SimpleProperty
  public void EndpointURL(final String url) {
    this.endpointUrl = url;
    Log.d(LOG_TAG, "Setting endpoint URL");
    if(conceptUri != null && !conceptUri.isEmpty() && !initialized) {
      final Runnable call = new Runnable() {
        public void run() { populateItemsList(url, conceptUri); }
      };
      BeforeQuery();
      Log.d(LOG_TAG, "Preparing to populate items list.");
      AsynchUtil.runAsynchronously(call);
    }
  }

  /**
   * Gets the endpoint URL for the list picker.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
      description = "<p>Use the Endpoint URL field to specify a Uniform "
          + "Resource Locator (URL) of a SPARQL endpoint to query for objects "
          + "used to populate the list picker.</p><p>For example, DBpedia "
          + "provides a SPARQL endpoint at "
          + "<code>http://dbpedia.org/sparql</code> that can be used to query "
          + "content extracted from Wikipedia.</p>")
  public String EndpointURL() {
    return this.endpointUrl;
  }

  // LDComponent implementation

  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
      description = "<p>Object Type specifies a Uniform Resource Identifier "
          + "(URI) for a type used to identify objects that should appear in "
          + "the list picker.</p><p>For example, "
          + "<code>http://xmlns.com/foaf/0.1/Person</code> (foaf:Person), is a "
          + "commonly used class for identifying people. Specifying it here "
          + "would generate a list of people from the Endpoint URL.</p>")
  @Override
  public String ObjectType() {
    return conceptUri;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CONCEPT_URI,
      defaultValue = "")
  @SimpleProperty
  @Override
  public void ObjectType(final String uri) {
    conceptUri = uri;
    Log.d(LOG_TAG, "Setting concept uri");
    if(endpointUrl != null && !endpointUrl.isEmpty()) {
      final Runnable call = new Runnable() {
        public void run() { populateItemsList(endpointUrl, uri); }
      };
      BeforeQuery();
      Log.d(LOG_TAG, "Preparing to populate items list.");
      AsynchUtil.runAsynchronously(call);
    }
  }

  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
      description = "<p>RelationToObjectType specifies the relationship "
          + "between objects presented by the list picker and the Object Type. "
          + "This field defaults to <code>rdf:type</code>, but other useful "
          + "values may include <code>skos:narrower</code> or "
          + "<code>schema:additionalType</code>. The correct property to use "
          + "will be dependent on the data available in Endpoint URL.</p>")
  public String RelationToObject() {
    return relationUri;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_PROPERTY_URI,
      defaultValue = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
  @SimpleProperty
  public void RelationToObject(final String uri) {
    relationUri = uri;
  }

  private void populateItemsList(final String endpoint, final String conceptUri) {
    Log.d(LOG_TAG, "Populating item list for semantic list picker");
    final String query = "PREFIX dc: <http://purl.org/dc/terms/> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
        "SELECT DISTINCT ?uri (SAMPLE(?lbl) AS ?label) WHERE { " +
        "?uri <" + relationUri + "> <" + conceptUri + "> " +
        "{ ?uri rdfs:label ?lbl }" +
        " UNION { ?uri skos:prefLabel ?lbl } " +
        "UNION { ?uri foaf:name ?lbl } UNION { ?uri dc:title ?lbl } " +
        "FILTER(lang(?lbl) = \"\" || langMatches(lang(?lbl), \"" +
        Locale.getDefault().getLanguage() + "\"))" +
        "} GROUP BY ?uri ORDER BY ?label";
    Log.d(LOG_TAG, "The Query is " + query);
    Collection<Solution> solutions = null;
    try {
      ResultSet results = RdfUtil.executeSELECT(endpoint, query);
      Log.d(LOG_TAG, "Received results; parsing JSON to collection.");
      solutions = RdfUtil.resultSetAsCollection(results);
    } catch(final Exception e) {
      Log.w(LOG_TAG, "Unable to retrieve SPARQL contents due to exception.", e);
      form.runOnUiThread(new Runnable() {
        public void run() {
          UnableToRetrieveContent(e.getLocalizedMessage());
        }
      });
      return;
    }
    items.clear();
    Log.d(LOG_TAG, "Finished query, got "+solutions.size()+" solutions");
    for(Solution i : solutions) {
      try {
        final String uri = (String)i.getBinding("uri").get(1);
        final String label = (String)i.getBinding("label").get(1);
        items.add(new LabeledUri(label, uri));
        Log.d(LOG_TAG, label + ": " + uri);
      } catch(final Exception e) {
        Log.w(LOG_TAG, "Unexpected exception processing SPARQL results.", e);
        form.runOnUiThread(new Runnable() {
          public void run() {
            UnableToRetrieveContent(e.getLocalizedMessage());
          }
        });
        initialized = false;
        return;
      }
    }
    initialized = true;
    Log.d(LOG_TAG, "Finished processing results, calling AfterQuery");
    form.runOnUiThread(new Runnable() {
      public void run() {
        AfterQuery();
      }
    });
  }

  /**
   * This event is raised before the query is executed on the remote endpoint.
   */
  @SimpleEvent
  public void BeforeQuery() {
    EventDispatcher.dispatchEvent(this, "BeforeQuery");
  }

  /**
   * This event is raised after the query is executed on the remote endpoint.
   */
  @SimpleEvent
  public void AfterQuery() {
    EventDispatcher.dispatchEvent(this, "AfterQuery");
  }
  
  /**
   * Raised in the event a query fails.
   * @param error Error message sent by server.
   */
  @SimpleEvent
  public void UnableToRetrieveContent(String error) {
    EventDispatcher.dispatchEvent(this, "UnableToRetrieveContent", error);
  }

  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
      description = "<p>If the list picker is placed within a Linked Data Form, "
          + "the Property URI specifies the relationship between the object "
          + "being built by the form and the item selected in the list picker."
          + "</p><p>For example, an application for disaster reporting may "
          + "query a hierarchy of disaster types and present those types using "
          + "the Linked Data List Picker.</p>")
  @Override
  public String PropertyURI() {
    return propertyUri;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_PROPERTY_URI,
      defaultValue = "")
  @SimpleProperty
  @Override
  public void PropertyURI(String uri) {
    propertyUri = uri;
  }

  @Override
  public Object Value() {
    return selectionUri;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty
  public void SubjectIdentifier(boolean isSubject) {
    this.isSubject = isSubject;
  }

  @Override
  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
      description = "<p>If the list picker is placed within a Linked Data Form, "
          + "the Subject Identifier value indicates whether the selected "
          + "item in the list picker should be used as an input when the "
          + "Linked Data component generates a new subject identifier for the "
          + "thing described by the form.</p>")
  public boolean SubjectIdentifier() {
    return isSubject;
  }

  /**
   * Returns the label of the user's selection.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "")
  public String SelectionLabel() {
    return selectionLabel;
  }

  /**
   * Returns the URI of the user's selection.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "")
  public String SelectionURI() {
    return selectionUri;
  }

  /**
   * Returns the index of the user's selection.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "")
  public int SelectionIndex() {
    return selectionIndex;
  }

}
