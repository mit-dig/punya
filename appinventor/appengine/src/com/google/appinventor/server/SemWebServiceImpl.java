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
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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

    // create the ontology model and load ontologies
    ontologyModel = ModelFactory.createDefaultModel();
    ontologyModel.register(larqBuilder);
    for(int i=0;i<ontologies.length;i++) {
      log.debug("Reading <"+ontologies[i]+"> ...");
      try {
        // we use this instead of Model.read(String) because Jena would
        // use Apache HTTP commons, which attempts to read the jssecacerts file.
        // AppEngine throws an AccessControlException that is never caught
        // and ultimately will cause this to fail. By using our own connection
        // we bypass this issue.
        HttpURLConnection conn = (HttpURLConnection)new URL(ontologies[i]).openConnection();
        conn.addRequestProperty("accept", "application/rdf+xml,text/turtle,text/n3");
        conn.setDoInput(true);
        conn.connect();
        ontologyModel.read(conn.getInputStream(), ontologies[i]);
      } catch(Exception e) {
        Logger.getRootLogger().warn("Unable to read ontology "+ontologies[i], e);
      }
    }
    larqBuilder.closeWriter();
    ontologyModel.unregister(larqBuilder);
    LARQ.setDefaultIndex(larqBuilder.getIndex());
    log.info("Lucene initialization completed in " + (System.currentTimeMillis()-start) + " ms.");
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
    final List<Map<String, String>> pairs = new ArrayList<Map<String, String>>();
    Query query = QueryFactory.create(queryText);
    QueryExecution qe = QueryExecutionFactory.create(query, ontologyModel);
    if(query.isSelectType()) {
      // handle SELECT queries
      ResultSet rs = qe.execSelect();
      while(rs.hasNext()) {
        // convert the query solution into a more compact representation
        QuerySolution qs = rs.nextSolution();
        final String label = qs.getLiteral("label").getString();
        final String value = qs.getResource("uri").getURI();
        final String prefix = ontologyModel.qnameFor(value);
        Logger.getRootLogger().info(label+","+value+","+prefix);
        pairs.add(createEntry(label, value, prefix));
      }
      Logger.getRootLogger().info("Finished query");
    } else {
      Logger.getRootLogger().warn("Unexpected query type");
    }
    return pairs;
  }

  @Override
  public List<Map<String, String>> searchClasses(String text) {
    // TODO: Escape the incoming text
    String queryText = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> "+
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
        "SELECT DISTINCT ?uri ?label WHERE { "+
        "?uri a owl:Class ; rdfs:label ?label . " +
        "FILTER(lang(?label) = \"\" || langMatches(lang(?label), \"EN\")) . " +
        "FILTER(regex(?label, \""+text+"\", \"i\")) "+
        "} ORDER BY ?label";
    return processQuery(queryText);
  }

  @Override
  public List<Map<String, String>> searchProperties(String text) {
    // TODO: Escape the incoming text
    String queryText = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> "+
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
        "PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
        "SELECT DISTINCT ?uri ?label WHERE { "+
        "{ ?uri a owl:ObjectProperty } UNION { ?uri a owl:DatatypeProperty }  " +
        "?uri rdfs:label ?label . " +
        "FILTER(lang(?label) = \"\" || langMatches(lang(?label), \"EN\")) . " +
        "FILTER(regex(?label, \""+text+"\", \"i\")) . "+
        "} ORDER BY ?label";
    return processQuery(queryText);
  }

}
