package com.google.appinventor.components.runtime;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

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
import com.google.appinventor.components.runtime.util.WebServiceUtil;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import android.util.Log;

@DesignerComponent(version = YaVersion.SEMANTIC_WEB_COMPONENT_VERSION,
    description = "Non-visible component that communicates with a SPARQL-powered triple store",
    category = ComponentCategory.MISC,
    nonVisible = true,
    iconName = "images/semanticWeb.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "xercesImpl.jar," + 
    "slf4j-android.jar," + "jena-iri.jar," + "jena-core.jar," +
    "jena-arq.jar")
public class SemanticWeb extends AndroidNonvisibleComponent implements
		Component {

  private static final String LOG_TAG = "SemanticWeb";

  private String endpointURL;

  public SemanticWeb(ComponentContainer container) {
	  super(container.$form());
	  endpointURL = "http://dbpedia.org/sparql";
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
    Query query = QueryFactory.create(queryText);
    QueryEngineHTTP qe = QueryExecutionFactory.createServiceRequest(EndpointURL(), query);
    qe.setSelectContentType("application/sparql-results+json");
    if(query.isSelectType()) {
      ResultSet rs = qe.execSelect();
      final LinkedList<Object> bindings = new LinkedList<Object>();
      while(rs.hasNext()) {
        Log.i(LOG_TAG, "Processing binding...");
        LinkedList<Object> binding = new LinkedList<Object>();
        Binding b = rs.nextBinding();
        Iterator<Var> vars = b.vars();
        while(vars.hasNext()) {
          Var var = vars.next();
          Log.i(LOG_TAG, "Processing var..."+var.toString());
          b.get(var);
          LinkedList<Object> pair = new LinkedList<Object>();
          pair.add(var.getName());
          Node node = b.get(var);
          if(node.isLiteral()) {
            if(node.getLiteralDatatype() == XSDDatatype.XSDinteger) {
              pair.add(Long.parseLong(node.getLiteralLexicalForm()));
            } else if(node.getLiteralDatatype() == XSDDatatype.XSDdouble) {
              pair.add(node.getLiteralValue());
            } else if(node.getLiteralDatatype() == XSDDatatype.XSDdecimal) {
              pair.add(Double.parseDouble(node.getLiteralLexicalForm()));
            } else {
              pair.add(node.getLiteralLexicalForm());
            }
          } else if(node.isURI()) {
            pair.add(node.getURI());
          } else {
            Log.w(LOG_TAG, "Variable not URI nor Literal");
          }
          if(pair.size()==2) {
            binding.add(pair);
          } else {
            Log.w(LOG_TAG, "Pair not of size 2");
          }
        }
        bindings.add(binding);
      }
      form.runOnUiThread(new Runnable() {
        public void run() {
          RetrievedResults("SELECT", bindings);
        }
      });
    } else {
      form.runOnUiThread(new Runnable() {
        public void run() {
          UnsupportedQueryType();
        }
      });
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
}
