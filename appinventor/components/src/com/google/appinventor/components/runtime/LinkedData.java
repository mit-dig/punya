// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2013-2020 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.util.Log;
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
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.IOUtils;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.RdfUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.XSD;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@DesignerComponent(version = YaVersion.LINKED_DATA_COMPONENT_VERSION,
    description = "Non-visible component that communicates with a SPARQL-powered triple store",
    category = ComponentCategory.LINKEDDATA,
    nonVisible = true,
    iconName = "images/semanticWeb.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "xercesImpl.jar," +
    "slf4j-android.jar," + "jena-iri.jar," + "jena-core.jar," +
    "jena-arq.jar," + "xml-apis.jar")
public class LinkedData extends LinkedDataBase<Model> implements
		Component {

  /* constants for convenience */
  private static final String LOG_TAG = "LinkedData";
  private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
  private static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
  private static final String SIOC_NS = "http://rdfs.org/sioc/ns#";
  private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
  private static final String SKOS_NS = "http://www.w3.org/2004/02/skos/core#";

  /** endpointURL stores the URI of a SPARQL endpoint **/
  private String endpointURL;

  public LinkedData(ComponentContainer<?> container) {
	  super(container, ModelFactory.createDefaultModel());
	  endpointURL = "http://dbpedia.org/sparql";
    model.setNsPrefix("rdf", RDF_NS);
    model.setNsPrefix("rdfs", RDFS_NS);
    model.setNsPrefix("owl", OWL_NS);
    model.setNsPrefix("sioc", SIOC_NS);
    model.setNsPrefix("geo", GEO_NS);
    model.setNsPrefix("skos", SKOS_NS);
    model.setNsPrefix("xsd", XSD.getURI());
  }

  public void Initialize() {
    model.setNsPrefixes(RdfUtil.PREFIXES);
  }

  /**
   * Returns the URL of the SPARQL endpoint.
   *
   * @return the SPARQL endpoint used for reading/writing remote RDF
   */
  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
      description = "<p>Use the Endpoint URL field to specify a Uniform "
          + "Resource Locator (URL) of a SPARQL endpoint to read and write "
          + "data on the web. For example, if you want to query structured "
          + "content from DBpedia, use <code>http://dbpedia.org/sparql</code>."
          + "</p><p>There are a number of public endpoints listed at "
          + "<a href=\"http://datahub.io/dataset?res_format=api%2Fsparql\" "
          + "target='_new'>DataHub.io</a></p>")
  public String EndpointURL() {
    return endpointURL;
  }

  /**
   * Specifies the URL of a SPARQL endpoint.
   * The default value is the DBpedia endpoint.
   *
   * @param url the SPARQL endpoint to use for reading/writing remote RDF
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "http://dbpedia.org/sparql")
  @SimpleProperty
  public void EndpointURL(String url) {
	  endpointURL = url;
  }

  /**
   * Execute a SPARQL query on the set EndpointURL of this Linked Data component.
   * Currently only supports SELECT queries, and converts all integer types into Long
   * and decimal types into Double.
   *
   * @param query Query text to execute
   */
  @SimpleFunction
  public void ExecuteSPARQLQuery(final String query) {
    final Runnable call = new Runnable() {
      public void run() { executeQuery(query); }
    };
    AsynchUtil.runAsynchronously(call);
  }

  private void executeQuery(String queryText) {
    try {
      ResultSet results = RdfUtil.executeSELECT( endpointURL, queryText );
      if ( results == null ) {
        form.runOnUiThread(new Runnable() {
          public void run() {
            UnsupportedQueryType();
          }
        });
        return;
      }
      results = ResultSetFactory.copyResults( results );
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ResultSetFormatter.outputAsJSON( baos, results );
      final String jsonResults = baos.toString();
      form.runOnUiThread(new Runnable() {
        public void run() {
          RetrievedRawResults("SELECT", jsonResults);
        }
      });
      ((ResultSetRewindable)results).reset();
      final YailList solutions = RdfUtil.resultSetUsingYailDictionary( results );
      form.runOnUiThread(new Runnable() {
        public void run() {
          RetrievedResults("SELECT", solutions);
        }
      });
    } catch (final Exception e ) {
      Log.w(LOG_TAG, e);
      Log.w(LOG_TAG, queryText);
      form.runOnUiThread(new Runnable() {
        public void run() {
          FailedToExecuteQuery(e.getMessage());
        }
      });
    }
  }

  /**
   * This event is raised after a SPARQL engine finishes processing a query
   * and the client has received the results, but before those results have
   * been processed into objects so that they may be used in conjunction with
   * other linked-data-enabled components.
   *
   * @param type the query type
   * @param contents the contents of the query results as a string
   */
  @SimpleEvent
  public void RetrievedRawResults(String type, String contents) {
    EventDispatcher.dispatchEvent(this, "RetrievedRawResults", type, contents);
  }

  /**
   * This event is raised after a SPARQL engine finishes processing
   * a query and the client has received the results.
   *
   * @param type Type of query executed, e.g. SELECT
   * @param bindings A list of bindings satisfying the SPARQL query
   */
  @SimpleEvent
  public void RetrievedResults(String type, YailList bindings) {
    EventDispatcher.dispatchEvent(this, "RetrievedResults", type, bindings);
  }

  /**
   * Event raised when a SPARQL query to be executed is not supported
   * by the Linked Data component.
   */
  @SimpleEvent
  public void UnsupportedQueryType() {
    EventDispatcher.dispatchEvent(this, "UnsupportedQueryType");
  }

  /**
   * Event raised when a SPARQL query raises an exception.
   */
  @SimpleEvent
  public void FailedToExecuteQuery(String error) {
    EventDispatcher.dispatchEvent(this, "FailedToExecuteQuery", error);
  }

  /**
   * Read contents of the specified path (local or remote) into the referent model.
   *
   * @param path Path to a file containing linked data
   * @return true if the graph was read successfully, otherwise false
   */
  @SimpleFunction
  public boolean ReadDataFromWeb(String path) {
    return loadRemoteResource(path);
  }

  /**
   * Read contents of the specified path (local or remote) into the referent model.
   *
   * @param path Path to a file containing linked data
   * @return true if the graph was read successfully, otherwise false
   */
  @SimpleFunction
  public boolean ReadDataFromLocal(String path) {
    InputStream input = null;
    try {
      String type = "RDF/XML";
      if(path.endsWith(".n3")) {
        type = "N3";
      } else if(path.endsWith(".ttl")) {
        type = "TURTLE";
      }
      input = MediaUtil.openMedia(form, path);
      model.read(input, path, type);
    } catch(Exception e) {
      Log.w(LOG_TAG, "Unable to read model.", e);
      return false;
    } finally {
      IOUtils.closeQuietly(LOG_TAG, input);
    }
    return true;
  }

  /**
   * Saves the model to the given path on the file system.
   *
   * @param path Path to a local file where the model will be written
   * @return true if the data were written successfully, otherwise false
   */
  @SimpleFunction
  public boolean WriteDataToLocal(String path) {
    try {
      String type = "RDF/XML";
      if(path.endsWith(".n3")) {
        type = "N3";
      } else if(path.endsWith(".ttl")) {
        type = "TURTLE";
      } else if(path.endsWith(".txt")) {
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
   * Takes a component implementing the LDComponent interface and uses the properties defined
   * there to insert a triple into the model using the given subject.
   *
   * @param component An AndroidViewComponent with a PropertyURI defined
   * @param subject URI or CURIE representing the subject the property,value pair will be added to
   * @return true if the component was successfully converted into a triple, otherwise false
   */
  @SimpleFunction
  public boolean AddDataFromComponent(Component component, String subject) {
    // check that we can proceed
    Class<?> clazz = component.getClass();
    if ( !LDComponent.class.isAssignableFrom(clazz) ) {
      Log.w(LOG_TAG, "Component does not implement LDComponent");
      return false;
    }
    return RdfUtil.triplifyComponent((LDComponent)component, subject, model);
  }

  /**
   * Takes a LinkedDataForm component and converts it and any nested elements into
   * triples within the model encapsulated by this LinkedData component.
   *
   * @param form the form to serialize as RDF
   * @return true if the form converted into an RDF graph successfully, otherwise false
   */
  @SimpleFunction
  public boolean AddDataFromLinkedDataForm(LinkedDataForm form) {
    try {
      return RdfUtil.triplifyForm(form, RdfUtil.generateSubjectForForm(form), model);
    } catch(Exception e) {
      Log.w(LOG_TAG, "Unable to triplify form due to exception.", e);
      return false;
    }
  }

  private void doPublishModel(final URI uri, final String graph) {
    try {
      if(RdfUtil.publishGraph(uri, model)) {
        form.runOnUiThread(new Runnable() {
          public void run() {
              FinishedWritingDataToWeb(graph);
          }
        });
      } else {
        form.runOnUiThread(new Runnable() {
          public void run() {
              FailedToWriteDataToWeb(graph, "See log for details.");
          }
        });
      }
    } catch(final Exception e) {
      form.runOnUiThread(new Runnable() {
        public void run() {
          Log.w(LOG_TAG, "Unable to publish graph.", e);
          FailedToWriteDataToWeb(graph, e.getLocalizedMessage());
        }
      });
    }
  }

  /**
   * Write the model represented by the LinkedData component to the
   * RDF graph store represented by EndpointURL using the given graph URI.
   *
   * @param graph the target graph to receive the contents of the model
   */
  @SimpleFunction
  public void WriteDataToWeb(final String graph) {
    try {
      URI part = new URI(null, null, "rdf-graph-store", "graph="+graph, null);
      final URI uri = URI.create(EndpointURL()).resolve(part);
      Runnable call = new Runnable() {
        public void run() {
          doPublishModel(uri, graph);
        }
      };
      AsynchUtil.runAsynchronously(call);
    } catch (URISyntaxException e) {
      Log.w(LOG_TAG, "Unable to generate RDF Graph Store URL.", e);
      FailedToWriteDataToWeb(graph, "Invalid endpoint URI. See log for details.");
    }
  }

  /**
   * This event is raised when the LinkedData component fails to publish a
   * graph to a remote SPARQL endpoint.
   *
   * @param graph the target graph that was written
   * @param error an error message reported by the server
   */
  @SimpleEvent
  public void FailedToWriteDataToWeb(String graph, String error) {
    EventDispatcher.dispatchEvent(this, "FailedToWriteDataToWeb", graph, error);
  }

  /**
   * This event is raised when a graph is successfully published on a remote
   * endpoint.
   *
   * @param graph the target graph that was written
   */
  @SimpleEvent
  public void FinishedWritingDataToWeb(String graph) {
    EventDispatcher.dispatchEvent(this, "FinishedWritingDataToWeb", graph);
  }

  private static final Set<Class<?>> WRAPPER_TYPES =
      new HashSet<Class<?>>(Arrays.asList(Boolean.class, Byte.class,
          Short.class, Integer.class, Long.class, Float.class, Double.class));
  private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
    return clazz.isPrimitive() || WRAPPER_TYPES.contains(clazz);
  }

  @SimpleFunction
  public void HttpsPostFileToWeb(final String Url, final String certificateName, final String securityToken, final String filepath) {
    try {
      Runnable call = new Runnable() {
        public void run() {
        	doInsertData(Url, certificateName, securityToken, filepath);
        }
      };
      AsynchUtil.runAsynchronously(call);
    } catch (Exception e) {
    	Log.e(LOG_TAG, "Unable to https post file to web." + e.getLocalizedMessage());
      form.runOnUiThread(new Runnable() {
        public void run() {
        	FailedHttsPostingFileToWeb("Please see system log (logcat) for more info.");
        }
      });
    }
  }
  
  private void doInsertData(final String Url, final String certificateName, final String securityToken, final String filePath) {
		try {
    	final InputStream inputStream = form.getAssets().open(certificateName);
			boolean success = RdfUtil.performHttpsRequest(Url, inputStream, securityToken, filePath);
      if(success) {
        form.runOnUiThread(new Runnable() {
          public void run() {
		        FinishedHttsPostingFileToWeb("done");
          }
        });
      } else {
        form.runOnUiThread(new Runnable() {
          public void run() {
		        FailedHttsPostingFileToWeb("Please see system log (logcat) for more info.");
          }
        });
      }
		} catch (IOException e) {
			Log.e(LOG_TAG, "Unable to https post file to web." + e.getLocalizedMessage());
    	final String erroMessage = e.getLocalizedMessage();
      form.runOnUiThread(new Runnable() {
        public void run() {
	        FailedHttsPostingFileToWeb(erroMessage);
        }
      });
		}
  }
  
  /**
   * Attempts to insert the statements contained within this Linked Data
   * component into the endpoint with an optional graph.
   * @param graph Empty string for the default graph, otherwise a valid URI
   * @param noResolveUpdate true if the component should attempt to resolve the
   * update URL relative to {@link #EndpointURL()}, false will send the query
   * directly to {@link #EndpointURL()}.
   */
  @SimpleFunction
  public void AddDataToWeb(final String graph, boolean noResolveUpdate) {
    try {
      URI part = new URI(null, null, "update", null, null);
      URI base = URI.create(EndpointURL());
      final URI uri = noResolveUpdate ? base : base.resolve(part);
      Runnable call = new Runnable() {
        public void run() {
        	if (endpointURL.contains("dydra.com")) {
            doInsertModel(0, uri, graph);
        	} else {
            doInsertModel(1, uri, graph);       		
        	}
        }
      };
      AsynchUtil.runAsynchronously(call);
    } catch (URISyntaxException e) {
      Log.w(LOG_TAG, "Unable to generate SPARQL Update URL.", e);
      FailedToAddDataToWeb(graph, "Invalid endpoint URI. See log for details.");
    }
  }

  private void doInsertModel(int option, final URI uri, final String graph) {
    try {
    	
    	boolean selection = false;
    	if (option == 0) {
    		selection = RdfUtil.insertDataToDydra(uri, model, graph.length() == 0 ? null : graph);
    	} else if (option == 1) {
    		selection = RdfUtil.insertDataToVirtuoso(uri, model, graph.length() == 0 ? null : graph);
    	} 
    	
      if(selection) {
        form.runOnUiThread(new Runnable() {
          public void run() {
            FinishedAddingDataToWeb(graph);
          }
        });
      } else {
        form.runOnUiThread(new Runnable() {
          public void run() {
            FailedToAddDataToWeb(graph, "See log for details.");
          }
        });
      }
    } catch(final Exception e) {
      form.runOnUiThread(new Runnable() {
        public void run() {
          Log.w(LOG_TAG, "Unable to insert data to graph.", e);
          FailedToAddDataToWeb(graph, e.getLocalizedMessage());
        }
      });
    }
  }

  @SimpleEvent
  public void FailedToAddDataToWeb(String graph, String error) {
    EventDispatcher.dispatchEvent(this, "FailedToAddDataToWeb", graph, error);
  }

  @SimpleEvent
  public void FinishedAddingDataToWeb(String graph) {
    EventDispatcher.dispatchEvent(this, "FinishedAddingDataToWeb", graph);
  }

  /**
   * Attempts to feed the statements contained within this Linked Data
   * component into the endpoint (most likely CSPARQL).
   */
  @SimpleFunction
  public void FeedDataToWeb() {
    final URI uri = URI.create(EndpointURL());
    Runnable call = new Runnable() {
      public void run() {
        doFeedModel(uri);
      }
    };
    AsynchUtil.runAsynchronously(call);
  }

  private void doFeedModel(final URI uri) {
    try {
      if(RdfUtil.feedData(uri, model)) {
        form.runOnUiThread(new Runnable() {
          public void run() {
            FinishedFeedingDataToWeb();
          }
        });
      } else {
        form.runOnUiThread(new Runnable() {
          public void run() {
            FailedToFeedDataToWeb("See log for details.");
          }
        });
      }
    } catch(final Exception e) {
      form.runOnUiThread(new Runnable() {
        public void run() {
          Log.w(LOG_TAG, "Unable to feed data to web.", e);
          FailedToFeedDataToWeb(e.getLocalizedMessage());
        }
      });
    }
  }

	@SimpleFunction
	public String ResultsToSimpleJSON(final YailList results) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < results.size(); i++) {
			YailList solution = (YailList) results.getObject(i);
			if (i > 0) {
				sb.append(",");
			}
			sb.append("{");
			for (int j = 0; j < solution.size(); j++) {
				YailList binding = (YailList) solution.getObject(j);
				String varName = binding.getString(0);
				Object varValue = binding.getObject(1);
				if (j != 0) {
					sb.append(",");
				}
				sb.append("\"");
				sb.append(varName);
				sb.append("\":");
				if (isPrimitiveOrWrapper(varValue.getClass())) {
					sb.append(varValue);
				} else {
					sb.append("\"");
					sb.append(varValue);
					sb.append("\"");
				}
				sb.append("");
			}
			sb.append("}");
		}
		sb.append("]");
		String result = sb.toString();
		Log.d(LOG_TAG, "JSON = " + result);
		return result;
	}

  @SimpleEvent
  public void FailedToFeedDataToWeb(String error) {
    EventDispatcher.dispatchEvent(this, "FailedToFeedDataToWeb", error);
  }

  @SimpleEvent
  public void FinishedFeedingDataToWeb() {
    EventDispatcher.dispatchEvent(this, "FinishedFeedingDataToWeb");
  }

  /**
   * Attempts to delete the statements contained within this Linked Data
   * component from the endpoint with an optional graph.
   * @param graph Empty string for the default graph, otherwise a valid URI
   * @param noResolveUpdate true if the component should attempt to resolve the
   * update URL relative to {@link #EndpointURL()}, false will send the query
   * directly to {@link #EndpointURL()}.
   */
  @SimpleFunction
  public void DeleteDataFromWeb(final String graph, boolean noResolveUpdate) {
    try {
      URI part = new URI(null, null, "update", null, null);
      URI base = URI.create(EndpointURL());
      final URI uri = noResolveUpdate ? base : base.resolve(part);
      Runnable call = new Runnable() {
        public void run() {
          doDeleteModel(uri, graph);
        }
      };
      AsynchUtil.runAsynchronously(call);
    } catch (URISyntaxException e) {
      Log.w(LOG_TAG, "Unable to generate SPARQL Update URL.", e);
      FailedToDeleteDataFromWeb(graph, "Invalid endpoint URI. See log for details.");
    }
  }

  private void doDeleteModel(final URI uri, final String graph) {
    try {
      if(RdfUtil.deleteData(uri, model, graph.length() == 0 ? null : graph)) {
        form.runOnUiThread(new Runnable() {
          public void run() {
              FinishedDeletingDataFromWeb(graph);
          }
        });
      } else {
        form.runOnUiThread(new Runnable() {
          public void run() {
              FailedToDeleteDataFromWeb(graph, "See log for details.");
          }
        });
      }
    } catch(final Exception e) {
      form.runOnUiThread(new Runnable() {
        public void run() {
          Log.w(LOG_TAG, "Unable to publish graph.", e);
          FailedToDeleteDataFromWeb(graph, e.getLocalizedMessage());
        }
      });
    }
  }

  @SimpleEvent
  public void FailedToDeleteDataFromWeb(String graph, String error) {
    EventDispatcher.dispatchEvent(this, "FailedToDeleteDataFromWeb", graph, error);
  }

  @SimpleEvent
  public void FinishedDeletingDataFromWeb(String graph) {
    EventDispatcher.dispatchEvent(this, "FinishedDeletingDataFromWeb", graph);
  }

  /**
   * Deletes all data from the referent model
   */
  @SimpleFunction
  public void DeleteDataFromLocal() {
    try {
      model.removeAll();
      FinishedDeletingDataFromLocal();
    } catch (Exception e) {
      Log.w(LOG_TAG, "Unable to delete data from model", e);
      FailedToDeleteDataFromLocal();
    }
  }

  @SimpleEvent
  public void FailedToDeleteDataFromLocal() {
    EventDispatcher.dispatchEvent(this, "FailedToDeleteDataFromLocal");
  }

  @SimpleEvent
  public void FinishedDeletingDataFromLocal() {
    EventDispatcher.dispatchEvent(this, "FinishedDeletingDataFromLocal");
  }

  /**
   * Returns the contents of this LinkedData component as a string. Useful
   * for debugging purposes.
   *
   * @return the contents of the model in Turtle
   */
  @SimpleFunction
  public String ToString() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    model.write(out, "TTL");
    return out.toString();
  }

  @SimpleEvent
  public void FinishedHttsPostingFileToWeb(String message) {
    EventDispatcher.dispatchEvent(this, "FinishedHttsPostingFileToWeb", message);
  }

  @SimpleEvent
  public void FailedHttsPostingFileToWeb(String errorMessage) {
    EventDispatcher.dispatchEvent(this, "FailedHttsPostingDataToWeb", errorMessage);
  }
}
