package com.google.appinventor.components.runtime;

import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsyncCallbackPair;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.RdfUtil;
import com.google.appinventor.components.runtime.util.RdfUtil.Solution;
import com.google.appinventor.components.runtime.util.WebServiceUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import android.util.Log;

@DesignerComponent(version = YaVersion.SEMANTIC_WEB_COMPONENT_VERSION,
    description = "Non-visible component that communicates with a SPARQL-powered triple store",
    category = ComponentCategory.SEMANTICWEB,
    nonVisible = true,
    iconName = "images/semanticWeb.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "xercesImpl.jar," + 
    "slf4j-android.jar," + "jena-iri.jar," + "jena-core.jar," +
    "jena-arq.jar")
public class SemanticWeb extends AndroidNonvisibleComponent implements
		Component {

  /* constants for convenience */
  private static final String LOG_TAG = "SemanticWeb";
  private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
  private static final String OWL_NS = "http://www.w3.org/2002/07/owl#";

  private final Model model;

  /** endpointURL stores the URI of a SPARQL endpoint **/
  private String endpointURL;

  /** baseURL is used for constructing URIs of new objects **/
  private String baseURL;

  public SemanticWeb(ComponentContainer container) {
	  super(container.$form());
	  endpointURL = "http://dbpedia.org/sparql";
	  baseURL = "http://example.com/";
    model = ModelFactory.createDefaultModel();
    model.setNsPrefix("rdf", RDF_NS);
    model.setNsPrefix("rdfs", RDFS_NS);
    model.setNsPrefix("owl", OWL_NS);
  }

  /**
   * Returns the URL of the SPARQL endpoint.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String EndpointURL() {
    return endpointURL;
  }

  /**
   * Specifies the URL of a SPARQL endpoint.
   * The default value is the DBpedia endpoint.
   * @param url
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "http://dbpedia.org/sparql")
  @SimpleProperty
  public void EndpointURL(String url) {
	  endpointURL = url;
  }

  /**
   * Returns the base URL of the component
   * @see #BaseURL(String)
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String BaseURL() {
    return baseURL;
  }

  /**
   * Specifies the base URL used for constructing new identifiers
   * when using linked data components. The URL should end with either
   * a / or a # per linked data design principles.
   * @return
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "http://example.com/")
  @SimpleProperty
  public void BaseURL(String url) {
    baseURL = url;
  }

  /**
   * Requests a file containing a SPARQL query from a remote server.
   * 
   * @param uri URI identifying a file containing a SPARQL query.
   */
  @SimpleFunction
  public void RetrieveQueryFromURI(final String uri) {
    final Runnable call = new Runnable() {
      public void run() { retrieveQueryFromURI(uri); }
    };
    AsynchUtil.runAsynchronously(call);
  }

  // We retrieve the query text from the specified URI here.
  private void retrieveQueryFromURI(String uri) {
    AsyncCallbackPair<String> myCallback = new AsyncCallbackPair<String>() {
      public void onSuccess(final String result) {
        if(result == null) {
          form.runOnUiThread(new Runnable() {
            public void run() {
              WebServiceError("The Web server did not send back a valid response.");
            }
          });
        } else {
          form.runOnUiThread(new Runnable() {
            public void run() {
              RetrievedQuery(result);
            }
          });
        }
      }
      public void onFailure(final String message) {
        form.runOnUiThread(new Runnable() {
          public void run() {
            WebServiceError(message);
          }
        });
        return;
      }
    };
    WebServiceUtil.getInstance().getCommand(uri, null, null, myCallback);
    return;
  }

  /**
   * Indicates that a RetrieveQuery request has succeeded.
   * 
   * @param queryText The query text at the original URI.
   */
  @SimpleEvent
  public void RetrievedQuery(String queryText) {
    EventDispatcher.dispatchEvent(this, "RetrievedQuery", queryText);
  }

  /**
   * Indicates that the communication with the Web service signaled an error
   * 
   * @param message the error message
   */
  @SimpleEvent
  public void WebServiceError(String message) {
    EventDispatcher.dispatchEvent(this, "WebServiceError", message);
  }

  /**
   * Execute a SPARQL query on the set EndpointURL of this semantic web component.
   * Currently only supports SELECT queries, and converts all integer types into Long
   * and decimal types into Double.
   * @param query Query text to execute
   */
  @SimpleFunction
  public void ExecuteQuery(final String query) {
    final Runnable call = new Runnable() {
      public void run() { executeQuery(query); }
    };
    AsynchUtil.runAsynchronously(call);
  }

  private void executeQuery(String queryText) {
    try {
      final ResultSet results = RdfUtil.executeSELECT( endpointURL, queryText );
      if ( results == null ) {
        form.runOnUiThread(new Runnable() {
          public void run() {
            UnsupportedQueryType();
          }
        });
        return;
      }
      final Collection<Solution> solutions = RdfUtil.resultSetAsCollection( results );
      form.runOnUiThread(new Runnable() {
        public void run() {
          RetrievedResults("SELECT", solutions);
        }
      });
    } catch ( Exception e ) {
      Log.w(LOG_TAG, e);
    }
  }

  /**
   * This event is raised after a SPARQL engine finishes processing
   * a query and the client has received the results.
   * @param type Type of query executed, e.g. SELECT
   * @param bindings A list of bindings satisfying the SPARQL query
   */
  @SimpleEvent
  public void RetrievedResults(String type, Collection<?> bindings) {
    EventDispatcher.dispatchEvent(this, "RetrievedResults", type, bindings);
  }

  /**
   * Event raised when a SPARQL query to be executed is not supported
   * by the Semantic Web component.
   */
  @SimpleEvent
  public void UnsupportedQueryType() {
    EventDispatcher.dispatchEvent(this, "UnsupportedQueryType");
  }

  /**
   * Loads the contents of the specified path into
   * the referent model.
   * @param model Model created using {@link #OpenModel()}
   * @param path Path to a file containing semantic web data (can be local or HTTP)
   * @return
   */
  @SimpleFunction
  public boolean LoadModel(String path) {
    try {
      String type = "RDF/XML";
      if(path.endsWith(".n3")) {
        type = "N3";
      } else if(path.endsWith(".ttl")) {
        type = "TURTLE";
      }
      model.read(path, type);
    } catch(Exception e) {
      Log.w(LOG_TAG, "Unable to read model.", e);
      return false;
    }
    return true;
  }

  /**
   * Adds a triple to the given model.
   * @param model Model reference created using {@link #OpenModel()}
   * @param subject URI or CURIE representing the subject of the triple
   * @param predicate URI or CURIE representing the predicate of the triple
   * @param object URI or CURIE representing the object of the triple
   */
  @SimpleFunction
  public void AddObjectTriple(String subject, String predicate, String object) {
    if(predicate.equals("a")) {
      predicate = RDF.type.getURI();
    }
    Resource s,o;
    Property p;
    s = model.getResource(model.expandPrefix(subject));
    p = model.getProperty(model.expandPrefix(predicate));
    o = model.getResource(model.expandPrefix(object));
    model.add(s, p, o);
  }

  /**
   * Saves the model to the given path on the file system.
   * @param model Model reference created using {@link #OpenModel()}
   * @param path Path to a local file where the model will be written
   * @return
   */
  @SimpleFunction
  public boolean SaveModel(String path) {
    try {
      String type = "RDF/XML";
      if(path.endsWith(".n3")) {
        type = "N3";
      } else if(path.endsWith(".ttl")) {
        type = "TURTLE";
      }
      FileOutputStream fos = new FileOutputStream(path);
      model.write(fos, type);
    } catch(Exception e) {
      Log.w(LOG_TAG, "Unable to write model.", e);
      return false;
    }
    return true;
  }

  /**
   * Sets an arbitrary number of (prefix,uri) pairs in the
   * model that will be used for expanding CURIEs used when
   * adding data to the model.
   * @param namespaces A list of pairs mapping prefixes to URIs
   * @see #SetNamespace(String, String)
   */
  @SimpleFunction
  public void SetNamespaces(YailList namespaces) {
    @SuppressWarnings("rawtypes")
    Iterator i = namespaces.iterator();
    while( i.hasNext() ) {
      try {
        YailList pair = (YailList) i.next();
        if( pair.length() != 2 ) {
          continue;
        }
        final String ns = (String) pair.get(0);
        final String uri = (String) pair.get(1);
        SetNamespace( ns, uri );
      } catch( ClassCastException e ) {
        // not a nested list...
      }
    }
  }

  /**
   * Sets a prefix to a specific URI
   * @param prefix Valid prefix in XML or Turtle
   * @param uri URI to substitute for the given prefix
   */
  @SimpleFunction
  public void SetNamespace(String prefix, String uri) {
    if( prefix == null || uri == null ) {
      return;
    }
    model.setNsPrefix(prefix, uri);
  }

  /**
   * Lists all of the instances in this model (i.e. all URIs minus Classes and Properties)
   * @param model Model reference obtained from {@link #OpenModel()}
   * @return A list of instance URIs in the model
   */
  @SimpleFunction
  public YailList ListInstances(String model) {
    YailList items = new YailList();
    /*
    Model m = models.get(model);
    if( m != null ) {
      Iterator<Individual> i = m.listIndividuals();
      while( i.hasNext() ) {
        items.add( i.next().getURI() );
      }
    }
    */
    return items;
  }

  /**
   * Retrieves the values for a specific subject,predicate pair in the model.
   * @param model Model reference obtained from {@link #OpenModel()}
   * @param subject URI or CURIE representing the subject
   * @param property URI or CURIE representing the predicate
   * @return A list of (type, value) pairs where type is either "uri" or "literal" to
   * indicate whether the value should be interpreted as another node in the graph
   * or as a fixed literal.
   */
  @SimpleFunction
  public YailList ValuesForProperty(String subject, String property) {
    YailList values = new YailList();
    Resource s = model.getResource( model.expandPrefix( subject ) );
    Property p = model.getProperty( model.expandPrefix( property ) );
    StmtIterator i = model.listStatements( s, p, (RDFNode) null );
    while( i.hasNext() ) {
      Statement stmt = i.next();
      YailList response = new YailList();
      try {
        Resource r = stmt.getResource();
        response.add( "uri" );
        response.add( r.getURI() );
      } catch(Exception e) {
        Literal l = stmt.getLiteral();
        response.add( "literal" );
        response.add( l.getValue() );
      }
      if( response.length() == 2 ) {
        values.add( response );
      }
    }
    return values;
  }

  /**
   * Takes a component implementing the LDComponent interface and uses the properties defined
   * there to insert a triple into the model using the given subject.
   * @param component An AndroidViewComponent with a PropertyURI defined
   * @param model Model reference obtained from {@link #OpenModel()}
   * @param subject URI or CURIE representing the subject the property,value pair will be added to
   * @return true if the component was successfully converted into a triple, otherwise false
   */
  @SimpleFunction
  public boolean TriplifyComponentInModel(Component component, String subject) {
    // check that we can proceed
    Class<?> clazz = component.getClass();
    if ( !LDComponent.class.isAssignableFrom(clazz) ) {
      Log.w(LOG_TAG, "Component does not implement LDComponent");
      return false;
    }
    return RdfUtil.triplifyComponent((LDComponent)component, subject, model);
  }

  /**
   * Takes a SemanticForm component and converts it and any nested elements into
   * triples within the model encapsulated by this SemanticWeb component.
   * @param form
   * @return
   */
  @SimpleFunction
  public boolean TriplifyFormInModel(SemanticForm form) {
    try {
      return RdfUtil.triplifyForm(form, RdfUtil.generateSubjectForForm(form), model);
    } catch(Exception e) {
      Log.w(LOG_TAG, "Unable to triplify form due to exception.", e);
      return false;
    }
  }
}
