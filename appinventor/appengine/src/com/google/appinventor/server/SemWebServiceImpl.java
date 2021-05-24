// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2013-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;

/**
 * Implementation of the semantic web service that provides features
 * for accessing ontological descriptions of linked data to enable
 * developers to build applications that contribute to the web of data.
 *
 * @author Evan W. Patton (ewpatton@gmail.com)
 *
 */
public class SemWebServiceImpl extends OdeRemoteServiceServlet implements
    SemWebService {

  private static final long serialVersionUID = 8321583419959798841L;

  private static final Logger LOG = Logger.getLogger(SemWebServiceImpl.class);

  private static final String INDEX_TIME = "Lucene initialization completed in %d ms.";

  /**
   * Stores all ontology information.
   */
  private static final transient Model ontologyModel = ModelFactory.createDefaultModel();

  /**
   * Sets up the ontology model and performs lucene indexing.
   */
  private static class OntologyLoader implements Runnable {
    @Override
    public void run() {
      final long start = System.currentTimeMillis();
      LOG.info("Initializing Lucene index for OWL ontologies...");
      Properties props = new Properties();
      try {
        props.load(SemWebConstants.class.getResourceAsStream("SemWebConstants.properties"));
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      String ontologiesList = props.getProperty("ontologies", "");
      String[] ontologies = ontologiesList.split(",");
      for (String ontology : ontologies) {
        LOG.debug("Reading <" + ontology + "> ...");
        try {
          // we use this instead of Model.read(String) because Jena would
          // use Apache HTTP commons, which attempts to read the jssecacerts file.
          // AppEngine throws an AccessControlException that is never caught
          // and ultimately will cause this to fail. By using our own connection
          // we bypass this issue.
          URL url = new URL(ontology);
          int attempts = 5;
          while (attempts-- > 0) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.addRequestProperty("Accept", "application/rdf+xml,text/turtle,text/n3");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            LOG.debug("Status code = " + response);
            if (response >= 300 && response <= 399) {
              url = new URL(conn.getHeaderField("Location"));
              conn.disconnect();
              continue;
            }
            String contentType = conn.getContentType();
            if (contentType != null && contentType.contains(";")) {
              contentType = contentType.split(";")[0];
            }
            if (contentType == null) {
              if (ontology.endsWith(".owl")) {
                ontology = ontology.replace(".owl", ".ttl");
              }
              RDFDataMgr.read(ontologyModel, conn.getInputStream(), ontology, Lang.TURTLE);
            } else if (contentType.equals("application/rdf+xml")
                || contentType.equals("application/xml")) {
              ontologyModel.read(conn.getInputStream(), ontology);
            } else if (contentType.equals("text/turtle")) {
              ontologyModel.read(conn.getInputStream(), ontology, "TTL");
            } else if (contentType.equals("text/n3")) {
              ontologyModel.read(conn.getInputStream(), ontology, "N3");
            } else if (contentType.equals("text/plain")
                || contentType.equals("application/octet-stream")) {
              // for non-compliant servers that return turtle as plain text
              try {
                ontologyModel.read(conn.getInputStream(), ontology, "TTL");
              } catch (Exception e) {
                LOG.warn("Unexpected content type 'text/plain' returned by server.");
              }
            } else {
              LOG.warn("Unexpected content type '" + contentType + "' returned by server.");
            }
            break;
          }
        } catch (Exception e) {
          Logger.getRootLogger().warn("Unable to read ontology " + ontology, e);
        }
      }
      LOG.info(String.format(INDEX_TIME, System.currentTimeMillis() - start));
      ontologyModel.removeNsPrefix("");
    }
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    new Thread(new OntologyLoader()).start();
  }

  /**
   * Creates a simple dictionary containing label and value keys.
   *
   * @param label Label to be displayed on client
   * @param uri URI (value) to be used as the actual identifier
   * @return create an entry describing a rdf:Resource containing a label and uri
   */
  private Map<String, String> createEntry(String label, String uri, String prefix) {
    Map<String, String> x = new HashMap<>();
    if (prefix != null) {
      label = label + " (" + prefix + ")";
    }
    x.put("label", label);
    x.put("value", uri);
    return x;
  }

  /**
   * Processes the results of a query (assumes label, uri variables) and
   * generates objects to be sent back to the client.
   *
   * @param queryText SPARQL query to be executed over the ontologies
   * @return list of mappings modeling the label and uri for matching rdf:Resources
   */
  private List<Map<String, String>> processQuery(String queryText) {
    final Logger log = Logger.getRootLogger();
    final List<Map<String, String>> pairs = new ArrayList<>();
    Query query = QueryFactory.create(queryText);
    QueryExecution qe = QueryExecutionFactory.create(query, ontologyModel);
    if (query.isSelectType()) {
      // handle SELECT queries
      ResultSetRewindable rs = ResultSetFactory.makeRewindable(qe.execSelect());
      while (rs.hasNext()) {
        // convert the query solution into a more compact representation
        QuerySolution qs = rs.nextSolution();
        String label;
        if (qs.getLiteral("label") != null) {
          label = qs.getLiteral("label").getString();
        } else if (qs.get("label") != null) {
          label = qs.get("label").toString();
        } else if (qs.getResource("uri") != null) {
          label = qs.getResource("uri").getURI();
          int idx = Math.max(label.lastIndexOf("/"), label.lastIndexOf("#"));
          label = label.substring(idx + 1);
        } else {
          // uri was null so this entry is pretty useless.
          continue;
        }
        final String value = qs.getResource("uri").getURI();
        final String prefix = ontologyModel.qnameFor(value);
        log.info(label + "," + value + "," + prefix);
        pairs.add(createEntry(label, value, prefix));
      }
      if (pairs.size() == 0) {
        Map<String, String> nullPair = new HashMap<>();
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
    String queryText = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> "
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
        + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
        + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
        + "SELECT DISTINCT ?uri (SAMPLE(?lbl) AS ?label) WHERE { "
        + "{ ?uri a owl:Class } UNION { ?uri a rdfs:Class }"
        + "{ ?uri rdfs:label ?lbl } UNION { ?uri skos:prefLabel ?lbl } "
        + "FILTER(lang(?lbl) = \"\" || langMatches(lang(?lbl), \"EN\")) . "
        + "FILTER(regex(?lbl, \"" + text + "\", \"i\")) "
        + "} GROUP BY ?uri ORDER BY ?label";
    return processQuery(queryText);
  }

  @Override
  public List<Map<String, String>> searchProperties(String text) {
    // TODO: Escape the incoming text
    String queryText = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> "
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
        + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
        + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
        + "SELECT DISTINCT ?uri (SAMPLE(?lbl) AS ?label) WHERE { "
        + "{ ?uri a owl:ObjectProperty } UNION { ?uri a owl:DatatypeProperty } "
        + "UNION { ?uri a rdf:Property } "
        + "{ ?uri rdfs:label ?lbl } UNION { ?uri skos:prefLabel ?lbl } "
        + "FILTER(lang(?lbl) = \"\" || langMatches(lang(?lbl), \"EN\")) . "
        + "FILTER(regex(?lbl, \"" + text + "\", \"i\")) "
        + "} GROUP BY ?uri ORDER BY ?label";
    return processQuery(queryText);
  }

  @Override
  public List<String> getProperties(String concept) {
    final Logger log = Logger.getRootLogger();
    List<String> propertyList = new ArrayList<>();
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(concept).openConnection();
      conn.addRequestProperty("Accept", "application/rdf+xml,text/turtle,text/n3");
      conn.setDoInput(true);
      conn.connect();
      OntModel model = (OntModel) ModelFactory.createOntologyModel();
      model.read(conn.getInputStream(), concept, "RDF/XML");
      OntClass ontClass = model.getOntClass(concept);
      ExtendedIterator<OntProperty> propIt = ontClass.listDeclaredProperties();
      log.info("The list of properties is: ");
      while (propIt.hasNext()) {
        if (propIt.next() != null) {
          propertyList.add(propIt.next().getURI());
          log.info(propIt.next().getURI());
        }
      }
    } catch (Exception e) {
      Logger.getRootLogger().warn("Unable to read ontology " + concept, e);
    }
    return propertyList;
  }

}
