package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@SimpleObject
public class LinkedDataBase<T extends Model> extends AndroidNonvisibleComponent {

  protected T model;

  protected LinkedDataBase(ComponentContainer<?> container) {
    this(container, null);
  }

  protected LinkedDataBase(ComponentContainer<?> container, T model) {
    super(container.$form());
    this.model = model;
  }

  /**
   * Get statements as a list of triples from the knowledge base. Each argument can either be false
   * or a string. False is treated as a wildcard. Strings are interpreted as URIs.
   *
   * @param subject the subject to filter by
   * @param predicate the predicate to filter by
   * @param object the object to filter by
   * @return a list of triples matching the (subject, predicate, object) pattern
   */
  @SimpleFunction
  public List<List<String>> GetStatements(Object subject, Object predicate, Object object) {
    List<List<String>> result = new ArrayList<>();
    for (StmtIterator it = statementIterator(subject, predicate, object); it.hasNext(); ) {
      Statement st = it.next();
      result.add(Arrays.asList(
          st.getSubject().toString(),
          st.getPredicate().toString(),
          st.getObject().toString()));
    }
    return result;
  }

  @SimpleFunction
  public List<List<String>> GetLangStatements(Object subject, Object predicate, Object object, Object lang) {
    List<List<String>> result = new ArrayList<>();
    for (StmtIterator it = statementIterator(subject, predicate, object); it.hasNext(); ) {
      Statement st = it.next();
      if (st.getObject().isLiteral() && st.getObject().asLiteral().getLanguage().equals(lang)) {
        result.add(Arrays.asList(
            st.getSubject().toString(),
            st.getPredicate().toString(),
            st.getObject().asLiteral().getString()));
      }
    }
    return result;
  }

  protected Model getModel() {
    return model;
  }

  protected StmtIterator statementIterator(Object subject, Object predicate, Object object) {
    Resource s = null;
    Property p = null;
    RDFNode o = null;
    if (subject != Boolean.FALSE) {
      s = ResourceFactory.createResource(subject.toString());
    }
    if (predicate != Boolean.FALSE) {
      if (predicate.toString().equals("a")) {
        p = RDF.type;
      } else {
        p = ResourceFactory.createProperty(predicate.toString());
      }
    }
    if (object != Boolean.FALSE) {
      String ostr = object.toString();
      if (ostr.startsWith("http:") || ostr.startsWith("https://") || ostr.startsWith("file://")) {
        o = ResourceFactory.createResource(ostr);
      } else {
        o = ResourceFactory.createPlainLiteral(ostr);
      }
    }
    return model.listStatements(s, p, o);
  }

  protected boolean loadRemoteResource(final String url) {
    try {
      return AsynchUtil.runAsynchronously(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          URL parsedUrl = new URL(url);
          HttpURLConnection conn = (HttpURLConnection) parsedUrl.openConnection();
          conn.setRequestProperty("Accept", "text/turtle, text/n-triples, application/rdf+xml");
          conn.setInstanceFollowRedirects(true);
          conn.setDoInput(true);
          conn.connect();
          String contentType = conn.getContentType();
          String lang = "RDF/XML";
          if (contentType.startsWith("text/turtle")) {
            lang = "TURTLE";
          } else if (contentType.startsWith("text/n3")) {
            lang = "N3";
          }
          model.read(conn.getInputStream(), url, lang);
          return true;
        }
      });
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }
}
