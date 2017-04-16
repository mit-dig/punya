package com.google.appinventor.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexWriterFactory;
import org.apache.jena.larq.LARQ;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.store.RAMDirectory;

import com.google.appinventor.shared.rpc.semweb.SemWebConstants;
import com.google.appinventor.shared.rpc.semweb.SemWebService;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Implementation of the semantic web service that provides features
 * for accessing ontological descriptions of linked data to enable
 * developers to build applications that contribute to the web of data.
 * 
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class SemWebServiceImpl extends OdeRemoteServiceServlet implements
    SemWebService {

  /**
   * 
   */
  private static final long serialVersionUID = 8321583419959798841L;

  /**
   * Stores all ontology information.
   */
  private static final transient Model ontologyModel;

  /**
   * Sets up the ontology model and performs lucene indexing
   */
  static {
    long start = System.currentTimeMillis();
    // configure log4j
    // TODO: put together a log4j.properties
    ConsoleAppender x = new ConsoleAppender();
    x.setWriter(new PrintWriter(System.err));
    x.setLayout(new PatternLayout(PatternLayout.DEFAULT_CONVERSION_PATTERN));
    Logger log = Logger.getRootLogger();
    log.addAppender(x);
    log.setLevel(Level.DEBUG);

    // read properties file
    log.info("Initializing Lucene index for OWL ontologies...");
    Properties props = new Properties();
    try {
      props.load(SemWebConstants.class.getResourceAsStream("SemWebConstants.properties"));
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    String ontologiesList = props.getProperty("ontologies", "");
    String ontologies[] = ontologiesList.split(",");

    // configure the index writer to use serial merge as
    // AppEngine does not allow spawning threads
    /*
    IndexWriter writer = null;
    try {
      writer = IndexWriterFactory.create(new RAMDirectory());
      writer.setMergeScheduler(new SerialMergeScheduler());
    } catch (CorruptIndexException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    IndexBuilderString larqBuilder;
    if(writer == null) {
      larqBuilder = new IndexBuilderString();
    } else {
      larqBuilder = new IndexBuilderString(writer);
    }
    */

    // create the ontology model and load ontologies
    ontologyModel = ModelFactory.createDefaultModel();
    //ontologyModel.register(larqBuilder);
    for(int i=0;i<ontologies.length;i++) {
      log.debug("Reading <"+ontologies[i]+"> ...");
      try {
        // we use this instead of Model.read(String) because Jena would
        // use Apache HTTP commons, which attempts to read the jssecacerts file.
        // AppEngine throws an AccessControlException that is never caught
        // and ultimately will cause this to fail. By using our own connection
        // we bypass this issue.
        HttpURLConnection conn = (HttpURLConnection)new URL(ontologies[i]).openConnection();
        conn.addRequestProperty("Accept", "application/rdf+xml,text/turtle,text/n3");
        conn.setDoInput(true);
        conn.connect();
        String contentType = conn.getContentType();
        if(contentType.equals("application/rdf+xml")) {
          ontologyModel.read(conn.getInputStream(), ontologies[i]);
        } else if(contentType.equals("text/turtle")) {
          ontologyModel.read(conn.getInputStream(), ontologies[i], "TTL");
        } else if(contentType.equals("text/n3")) {
          ontologyModel.read(conn.getInputStream(), ontologies[i], "N3");
        } else if(contentType.equals("text/plain")) {
          // for non-compliant servers that return turtle as plain text
          try {
            ontologyModel.read(conn.getInputStream(), ontologies[i], "TTL");
          } catch(Exception e) {
            log.warn("Unexpected content type 'text/plain' returned by server.");
          }
        } else {
          log.warn("Unexpected content type '"+contentType+"' returned by server.");
        }
      } catch(Exception e) {
        Logger.getRootLogger().warn("Unable to read ontology "+ontologies[i], e);
      }
    }
    //larqBuilder.closeWriter();
    //ontologyModel.unregister(larqBuilder);
    //LARQ.setDefaultIndex(larqBuilder.getIndex());
    log.info("Lucene initialization completed in " + (System.currentTimeMillis()-start) + " ms.");
    ontologyModel.removeNsPrefix("");
  }

  public void initialize() {
    // do nothing
  }

  /**
   * Creates a simple dictionary containing label and value keys.
   * @param label Label to be displayed on client
   * @param uri URI (value) to be used as the actual identifier
   * @return
   */
  private Map<String, String> createEntry(String label, String uri, String prefix) {
    Map<String, String> x = new HashMap<String, String>();
    if(prefix != null) {
      label = label + " ("+prefix+")";
    }
    x.put("label", label);
    x.put("value", uri);
    return x;
  }

  /**
   * Processes the results of a query (assumes label, uri variables) and
   * generates objects to be sent back to the client.
   * @param queryText SPARQL query to be executed over the ontologies
   * @return
   */
  private List<Map<String, String>> processQuery(String queryText) {
    final Logger log = Logger.getRootLogger();
    final List<Map<String, String>> pairs = new ArrayList<Map<String, String>>();
    Query query = QueryFactory.create(queryText);
    QueryExecution qe = QueryExecutionFactory.create(query, ontologyModel);
    if(query.isSelectType()) {
      // handle SELECT queries
      ResultSetRewindable rs = ResultSetFactory.makeRewindable(qe.execSelect());
      while(rs.hasNext()) {
        // convert the query solution into a more compact representation
        QuerySolution qs = rs.nextSolution();
        String label = null;
        if ( qs.getLiteral("label") != null ) {
          label = qs.getLiteral("label").getString();
        } else if( qs.get( "label" ) != null ) {
          label = qs.get( "label" ).toString();
        } else if( qs.getResource( "uri" ) != null ) {
          label = qs.getResource( "uri" ).getURI();
          int idx = Math.max(label.lastIndexOf("/"), label.lastIndexOf("#"));
          label = label.substring(idx+1);
        } else {
          // uri was null so this entry is pretty useless.
          continue;
        }
        final String value = qs.getResource("uri").getURI();
        final String prefix = ontologyModel.qnameFor(value);
        log.info(label+","+value+","+prefix);
        pairs.add(createEntry(label, value, prefix));
      }
      if ( pairs.size() == 0 ) {
        Map<String, String> nullPair = new HashMap<String, String>();
        nullPair.put("label", "No results found");
        nullPair.put("value", "");
        pairs.add(nullPair);
      }
      log.info("Finished query");
    } else {
      log.warn("Unexpected query type");
    }
    return pairs;
  }

  @Override
  public List<Map<String, String>> searchClasses(String text) {
    // TODO: Escape the incoming text
    String queryText = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> "+
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
        "SELECT DISTINCT ?uri (SAMPLE(?lbl) AS ?label) WHERE { "+
        "{ ?uri a owl:Class } UNION { ?uri a rdfs:Class }" +
        "{ ?uri rdfs:label ?lbl } UNION { ?uri skos:prefLabel ?lbl } " +
        "FILTER(lang(?lbl) = \"\" || langMatches(lang(?lbl), \"EN\")) . " +
        "FILTER(regex(?lbl, \""+text+"\", \"i\")) "+
        "} GROUP BY ?uri ORDER BY ?label";
    return processQuery(queryText);
  }

  @Override
  public List<Map<String, String>> searchProperties(String text) {
    // TODO: Escape the incoming text
    String queryText = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> "+
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
        "SELECT DISTINCT ?uri (SAMPLE(?lbl) AS ?label) WHERE { "+
        "{ ?uri a owl:ObjectProperty } UNION { ?uri a owl:DatatypeProperty } " +
        "UNION { ?uri a rdf:Property } " +
        "{ ?uri rdfs:label ?lbl } UNION { ?uri skos:prefLabel ?lbl } " +
        "FILTER(lang(?lbl) = \"\" || langMatches(lang(?lbl), \"EN\")) . " +
        "FILTER(regex(?lbl, \""+text+"\", \"i\")) "+
        "} GROUP BY ?uri ORDER BY ?label";
    return processQuery(queryText);
  }

  @Override
  public List<String> getProperties(String concept) {
  	final Logger log = Logger.getRootLogger();
  	List<String> propertyList = new ArrayList<String>();
  	try {
	    HttpURLConnection conn = (HttpURLConnection)new URL(concept).openConnection();
	    conn.addRequestProperty("Accept", "application/rdf+xml,text/turtle,text/n3");
	    conn.setDoInput(true);
	    conn.connect();
      OntModel model = (OntModel) ModelFactory.createOntologyModel();
      model.read(conn.getInputStream(), concept, "RDF/XML");
	  	OntClass ontClass = model.getOntClass(concept);
	  	ExtendedIterator<OntProperty> propIt = ontClass.listDeclaredProperties();
	  	log.info("The list of properties is: ");
	  	while (propIt.hasNext()){
	  		if (propIt.next() != null) {
		  		propertyList.add(propIt.next().getURI());
		  		log.info(propIt.next().getURI());
	  		}
	  	}
  	} catch(Exception e) {
      Logger.getRootLogger().warn("Unable to read ontology " + concept, e);
    }
  	return propertyList;
  }

}
