package com.google.appinventor.components.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.SWListActivity.LabeledUri;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.RdfUtil;
import com.google.appinventor.components.runtime.util.RdfUtil.Solution;
import com.hp.hpl.jena.query.ResultSet;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

/**
 * Provides a list picker backed by the results of a SPARQL query.
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
@DesignerComponent(version = YaVersion.SEMANTIC_WEB_LISTPICKER_COMPONENT_VERSION,
    category = ComponentCategory.SEMANTICWEB,
    description = "")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "xercesImpl.jar," + "slf4j-android.jar," + 
    "jena-iri.jar," + "jena-core.jar," + "jena-arq.jar")
public class SemanticWebListPicker extends Picker implements ActivityResultListener, Deleteable, LDComponent {

  private static final String LOG_TAG = SemanticWebListPicker.class.getSimpleName();
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
  private List<LabeledUri> items;
  private final Form form;
  private volatile boolean initialized = false;

  public SemanticWebListPicker(ComponentContainer container) {
    super(container);
    Log.d(LOG_TAG, "Constructing SemanticWebListPicker");
    selectionUri = "";
    selectionLabel = "";
    selectionIndex = 0;
    isSubject = false;
    conceptUri = "";
    propertyUri = "";
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
    intent.putParcelableArrayListExtra(SWLIST_ACTIVITY_ARG_NAME, (ArrayList<LabeledUri>)items);
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
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Endpoint from which entities will be retrieved.")
  public String EndpointURL() {
    return this.endpointUrl;
  }

  // LDComponent implementation

  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "The picker will be populated with entities of the specified type.")
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

  private void populateItemsList(final String endpoint, final String conceptUri) {
    Log.d(LOG_TAG, "Populating item list for semantic list picker");
    final String query = "PREFIX dc: <http://purl.org/dc/terms/> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
        "SELECT DISTINCT ?uri (SAMPLE(?lbl) AS ?label) WHERE { " +
        "?uri a <" + conceptUri + "> " +
        "{ ?uri rdfs:label ?lbl }" +
        //" UNION { ?uri skos:prefLabel ?lbl } " +
        //"UNION { ?uri foaf:name ?lbl } UNION { ?uri dc:title ?lbl } " +
        "FILTER(lang(?lbl) = \"\" || langMatches(lang(?lbl), \"" +
        Locale.getDefault().getLanguage() + "\"))" +
        "} GROUP BY ?uri ORDER BY ?label";
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

  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
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
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "")
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
