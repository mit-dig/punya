package com.google.appinventor.components.runtime.util;

import android.util.Base64;
import android.util.Log;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.LDComponent;
import com.google.appinventor.components.runtime.LinkedDataForm;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.Prologue;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;

public final class RdfUtil {
  public static final String LOG_TAG = RdfUtil.class.getSimpleName();

  private static final Set<RDFDatatype> INTEGER_TYPES;
  private static final Set<RDFDatatype> DOUBLE_TYPES;

  static {
    INTEGER_TYPES = new HashSet<>();
    INTEGER_TYPES.add(XSDDatatype.XSDbyte);
    INTEGER_TYPES.add(XSDDatatype.XSDunsignedByte);
    INTEGER_TYPES.add(XSDDatatype.XSDshort);
    INTEGER_TYPES.add(XSDDatatype.XSDunsignedShort);
    INTEGER_TYPES.add(XSDDatatype.XSDint);
    INTEGER_TYPES.add(XSDDatatype.XSDinteger);
    INTEGER_TYPES.add(XSDDatatype.XSDunsignedInt);
    INTEGER_TYPES.add(XSDDatatype.XSDnonNegativeInteger);
    INTEGER_TYPES.add(XSDDatatype.XSDnonPositiveInteger);
    INTEGER_TYPES.add(XSDDatatype.XSDpositiveInteger);
    INTEGER_TYPES.add(XSDDatatype.XSDnegativeInteger);
    DOUBLE_TYPES = new HashSet<>();
    DOUBLE_TYPES.add(XSDDatatype.XSDfloat);
    DOUBLE_TYPES.add(XSDDatatype.XSDdouble);
    DOUBLE_TYPES.add(XSDDatatype.XSDdecimal);
  }

  public static final class VariableBinding extends ArrayList<Object> {
    /**
     * 
     */
    private static final long serialVersionUID = -5764559802159393326L;

    protected VariableBinding() {
      super(2);
    }

    protected VariableBinding(String var, Resource value) {
      super(2);
      add(var);
      if(value.isURIResource()) {
        add(value.getURI());
      } else {
        add(value.getId().toString());
      }
    }

    protected VariableBinding(String var, Literal value) {
      super(2);
      add(var);
      if(value.getDatatype() == null) {
        add(value.getLexicalForm());
      } else if(value.getDatatype().equals(XSDDatatype.XSDinteger)) {
        add(Long.parseLong(value.getLexicalForm()));
      } else if(value.getDatatype().equals(XSDDatatype.XSDdouble)) {
        add(value.getDouble());
      } else if(value.getDatatype().equals(XSDDatatype.XSDdecimal)) {
        add(Double.parseDouble(value.getLexicalForm()));
      } else {
        add(value.getLexicalForm());
      }
    }
  }

  public static final class Solution implements Set<VariableBinding>, Serializable, Cloneable {
    private final Map<String, VariableBinding> backingMap;
    /**
     * 
     */
    private static final long serialVersionUID = -6904508566180785801L;

    protected Solution() {
      backingMap = new HashMap<String, VariableBinding>();
    }

    protected Solution(Solution old) {
      backingMap = new HashMap<String, VariableBinding>(old.backingMap);
    }

    protected Solution(QuerySolution solution) {
      backingMap = new HashMap<String, VariableBinding>();
      Iterator<String> i = solution.varNames();
      while(i.hasNext()) {
        String var = i.next();
        RDFNode node = solution.get(var);
        if(node.isLiteral()) {
          backingMap.put(var, new VariableBinding(var, node.asLiteral()));
        } else {
          backingMap.put(var, new VariableBinding(var, node.asResource()));
        }
      }
    }

    @Override
    public Object clone() {
      return new Solution(this);
    }

    @Override
    public int size() {
      return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
      return backingMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      if(o == null) {
        return false;
      }
      if(o instanceof VariableBinding) {
        return backingMap.containsKey(((VariableBinding)o).get(0));
      } else {
        return false;
      }
    }

    @Override
    public Iterator<VariableBinding> iterator() {
      return Collections.unmodifiableCollection(backingMap.values()).iterator();
    }

    @Override
    public Object[] toArray() {
      return backingMap.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return backingMap.values().toArray(a);
    }

    @Override
    public boolean add(VariableBinding e) {
      if(e == null) {
        return false;
      }
      if(!backingMap.containsKey(e.get(0))) {
        return false;
      }
      backingMap.put((String)e.get(0), e);
      return true;
    }

    @Override
    public boolean remove(Object o) {
      if(o == null) {
        return false;
      }
      if(o instanceof VariableBinding) {
        VariableBinding e = (VariableBinding)o;
        if(backingMap.containsKey(e.get(0))) {
          backingMap.remove(e.get(0));
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      for(Object o : c) {
        if(!contains(o)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends VariableBinding> c) {
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return false;
    }

    @Override
    public void clear() {
      backingMap.clear();
    }

    /**
     * Gets a binding in the solution for the given variable name.
     * @param var
     * @return
     */
    public VariableBinding getBinding(String var) {
      return backingMap.get(var);
    }
  }

  private static ResultSet executeSELECTQuery(String endpoint, Query query) {
    QueryEngineHTTP qe = QueryExecutionFactory.createServiceRequest(endpoint, query);
    qe.setSelectContentType("application/sparql-results+json");
    if(!query.isSelectType()) {
      Log.d(LOG_TAG, "Cannot execute query that is not SELECT");
      return null;
    }
    Log.d(LOG_TAG, "Executing SPARQL select query");
    return qe.execSelect();
  }

  /**
   * Executes a SPARQL SELECT query on the specified endpoint.
   * @param endpoint
   * @param queryText
   * @return
   */
  public static ResultSet executeSELECT(String endpoint, String queryText) {
    Query query = QueryFactory.create(queryText);
    return executeSELECTQuery(endpoint, query);
  }

  public static ResultSet executeSELECT(String endpoint, String queryText,
      PrefixMapping prefixes) {
    Query query = QueryFactory.parse(new Query(new Prologue(prefixes)),
        queryText, "", Syntax.syntaxSPARQL_11);
    return executeSELECTQuery(endpoint, query);
  }

  /**
   * Converts a ResultSet from a SELECT query into a collection that can
   * be passed to other App Inventor components.
   * @param results
   * @return
   */
  public static Collection<Solution> resultSetAsCollection(ResultSet results) {
    List<Solution> list = new LinkedList<Solution>();
    while(results.hasNext()) {
      list.add(new Solution(results.next()));
    }
    return list;
  }

  /**
   * Converts a component into a triple using the subject in the given model.
   * @param component
   * @param subject
   * @param model
   * @return
   */
  public static boolean triplifyComponent(LDComponent component, String subject,
      Model model) {
    Log.d(LOG_TAG, "Triplifying component "+component+" with subject <"+subject+">");
    final String conceptUri = component.ObjectType();
    final String propertyUri = component.PropertyURI();
    // verify that propertyUri is set
    if(propertyUri == null || propertyUri.length() == 0) {
      Log.w(LOG_TAG, "Property URI is empty");
      return false;
    }
    final Object value = component.Value();
    // don't store null values
    if(value == null) {
      Log.w(LOG_TAG, "Component.value() is null");
      return false;
    }
    Resource s = model.getResource( model.expandPrefix( subject ) );
    Property p = model.getProperty( model.expandPrefix( propertyUri ) );
    if ( conceptUri == null || conceptUri.length() == 0 ) {
      // if no concept, try to infer xsd datatype
      if (value.getClass() == Boolean.class) {
        model.add(s, p, value.toString(), XSDDatatype.XSDboolean);
      } else if (value.getClass() == Calendar.class) {
        Date d = ((Calendar) value).getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("Y-m-d");
        model.add(s, p, formatter.format(d), XSDDatatype.XSDdate);
      } else if (value.getClass() == String.class) {
        model.add(s, p, (String) value);
      } else {
        Log.w(LOG_TAG, "Concept URI not supplied and unable to determine appropriate XSD type from Value.getClass");
        return false;
      }
    } else if ( conceptUri.startsWith( "xsd:" ) ) {
      Literal l = model.createTypedLiteral(value.toString(),
          conceptUri.replace("xsd:", XSD.getURI()));
      model.add(s, p, l);
    } else if( conceptUri.startsWith( XSD.getURI() ) ) {
      // have a concept and it's in the xsd namespace
      Literal l = model.createTypedLiteral( value.toString(), conceptUri );
      model.add(s, p, l);
    } else {
      // since we are interpreting as a uri, we must have a string
      if( value.getClass() != String.class ) {
        Log.w(LOG_TAG, "Have ConceptURI but Value() is not of type java.lang.String");
        return false;
      }
      Resource o = model.getResource( (String) value );
      // apply the ConceptURI as the type of the value
      if ( !model.contains( o, RDF.type ) ) {
        model.add( o, RDF.type, model.getResource( conceptUri ) );
      }
      model.add( s, p, o );
    }
    return true;
  }

  private static boolean triplifyContainerOrForm(ComponentContainer<AndroidViewComponent> container, String subject,
      Model model) {
    for(AndroidViewComponent i : container) {
      if(i instanceof LinkedDataForm) {
        LinkedDataForm nestedForm = (LinkedDataForm)i;
        if(nestedForm.PropertyURI() == null || nestedForm.PropertyURI().length() == 0) {
          Log.w(LOG_TAG, "Found nested semantic form without a PropertyURI set");
          continue;
        }
        String nestedSubject = generateSubjectForForm(nestedForm);
        if(nestedSubject == null) {
          Log.w(LOG_TAG, "Unable to generate a uri for nested semantic form.");
          continue;
        }
        if(!triplifyForm(nestedForm, nestedSubject, model)) {
          return false;
        }
        Resource subj = model.getResource(subject);
        Property pred = model.getProperty(nestedForm.PropertyURI());
        Resource obj = model.getResource(nestedSubject);
        model.add(subj, pred, obj);
      } else if(i instanceof ComponentContainer) {
        if(!triplifyContainer((ComponentContainer)i, subject, model)) {
          return false;
        }
      } else if(i instanceof LDComponent) {
        if(!triplifyComponent((LDComponent)i, subject, model)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Turns a container into a collection of triples using the specified subject in the given model.
   * @param container
   * @param subject
   * @param model
   * @return
   */
  public static boolean triplifyContainer(ComponentContainer<AndroidViewComponent> container, String subject,
      Model model) {
    Log.d(LOG_TAG, "Triplifying container "+container+" with subject <"+subject+">");
    return triplifyContainerOrForm(container, subject, model);
  }

  /**
   * Given a semantic form, turn any nested components (including other {@link LinkedDataForm}s) into
   * an RDF graph contained within the given model.
   * @param form Semantic Form used to generate an RDF graph
   * @param subject URI used to represent the subject of the graph
   * @param model Model to contain the resulting RDF graph
   */
  public static boolean triplifyForm(LinkedDataForm form, String subject,
      Model model) {
    Log.i(LOG_TAG, "Triplifying form for subject <"+subject+">");
    String conceptUri = form.ObjectType();
    if(conceptUri != null && conceptUri.length() != 0) {
      Resource subj = model.getResource(subject);
      Resource obj = model.getResource(conceptUri);
      model.add(subj, RDF.type, obj);
    }
    return triplifyContainerOrForm(form, subject, model);
  }

  private static String processSubfieldForSubject(LDComponent component,
      final StringBuilder builder) {
    Log.v(LOG_TAG, "  Processing LDComponent "+component);
    Log.v(LOG_TAG, "  subjectIdentifier ? "+component.SubjectIdentifier());
    if(!component.SubjectIdentifier()) {
      return null;
    }
    Log.v(LOG_TAG, "  value ? "+component.Value());
    if(component.Value() == null) {
      return null;
    }
    Log.v(LOG_TAG, "  concept ? "+component.ObjectType());
    if(component.ObjectType().length() == 0 || component.ObjectType().startsWith(XSD.getURI())) {
      builder.append(component.Value());
    } else if(component.Value() instanceof String) {
      return (String)component.Value();
    }
    return null;
  }

  /**
   * If the subcomponent contains a URI field (i.e. a TextBox that has a
   * ConceptURI that isn't in the XSD namespace and whose value is not empty)
   * then this method will immediately return the contents of the field,
   * otherwise it will return null and the builder will be appended with the
   * value of non-URI fields.
   * @param container A component container to evaluate for subjects
   * @param builder Builder used to generate a URI for the calling form
   * @return
   */
  private static String processSubfieldForSubject(ComponentContainer<AndroidViewComponent> container,
      final StringBuilder builder) {
    Log.v(LOG_TAG, ">>> Processing container "+container);
    Log.v(LOG_TAG, "builder = "+builder.toString());
    String fullUri = null;
    for(AndroidViewComponent i : container) {
      Log.v(LOG_TAG, "  i = " + i);
      // semantic forms mark the start of a new instance, skip them.
      if(i instanceof LinkedDataForm) {
        continue;
      }
      // handle nested elements (e.g. developer puts a LDComponent inside an
      // HVArrangement)
      if(i instanceof ComponentContainer) {
        fullUri = processSubfieldForSubject((ComponentContainer)i, builder);
      } else if(i instanceof LDComponent) {
        fullUri = processSubfieldForSubject((LDComponent)i, builder);
      } else {
        continue;
      }
      if(fullUri != null) {
        return fullUri;
      }
    }
    Log.v(LOG_TAG, "builder = "+builder.toString());
    Log.v(LOG_TAG, "<<< Finished container "+container);
    return null;
  }

  /**
   * Given a linked data form, try to generate a subject URI from nested elements.
   * @param form Linked Data form to generate a URI for
   * @return A URI if the operation was completed successfully or null if no
   * valid elements were found with SubjectIdentifier set to true
   */
  public static String generateSubjectForForm(final LinkedDataForm form) {
    StringBuilder subject = new StringBuilder();
    if(form.Subject() != null && form.Subject().length() != 0) {
      return form.Subject();
    }
    subject.append(form.FormID());
    String fullUri = processSubfieldForSubject(form, subject);
    if(fullUri != null) {
      return fullUri;
    }
    if(subject.toString().equals(form.FormID())) {
      Log.d(LOG_TAG, "Form did not have URI fields; generating timestamp + uuid URI");
      String uuid = UUID.randomUUID().toString();
      subject.append(System.currentTimeMillis() + "-" + uuid);
    }
    return subject.toString();
  }

  /**
   * Publishes the a semantic web model to the given URI using the SPARQL
   * Graph Protocol. Data will be transmitted using the text/turtle encoding
   * with the UTF-8 charset. The method will return true if the server returns
   * a 2xx response code, otherwise it will return false and log the reason
   * for the failure to logcat.
   * @see http://www.w3.org/TR/2013/REC-sparql11-http-rdf-update-20130321/#http-put
   * @param uri An indirect graph URI (with a graph query parameter)
   * @param model A Jena model containing the RDF content to HTTP PUT
   * @return
   */
  public static boolean publishGraph(URI uri, Model model) {
    boolean success = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    model.write(baos, "TTL");
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestMethod("PUT");
      conn.setRequestProperty("Content-Length", Integer.toString(baos.size()));
      conn.setRequestProperty("Content-Type", "text/turtle;charset=utf-8");
      conn.connect();
      OutputStream os = conn.getOutputStream();
      baos.writeTo(os);
      int status = conn.getResponseCode();
      if(status == 200 || status == 201 || status == 204) {
        success = true;
      } else {
        Log.w(LOG_TAG, "Unable to put graph due to HTTP code "+status+" "+conn.getResponseMessage());
      }
      conn.disconnect();
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, "Unable to publish graph due to malformed URL", e);
    } catch (ProtocolException e) {
      Log.w(LOG_TAG, "Unable to perform HTTP PUT for given URI", e);
    } catch (IOException e) {
      Log.w(LOG_TAG, "Unable to publish graph due to IO failure", e);
    }
    return success;
  }

  /**
   * Performs a SPARQL 1.1 Update INSERT DATA operation on a remote triple
   * store by inserting the triples in <i>model</i> into the optionally named
   * graph <i>graph</i>
   * @param uri URI for the endpoint
   * @param model RDF model to send to the endpoint
   * @param graph Optional graph URI to insert data into. Pass null to insert
   * into the default graph.
   * @return true on success, false otherwise.
   */
  public static boolean insertDataToDydra(URI uri, Model model, String graph) {
    boolean success = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    model.write(baos, "TTL");
    Log.d(LOG_TAG, "Byte array size: "+baos.size());
    // get model contents in turtle
    String contents = null;
    try {
      contents = baos.toString("UTF-8");
    } catch(UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Unable to encode query.", e);
      return false;
    }
    baos = null;
    Log.d(LOG_TAG, "Model: "+contents);
    // remove PREFIX statements and move them before an insert data block
    Pattern prefixPattern = Pattern.compile("@prefix[ \t]+([^:]*:)[ \t]+<([^>]+)>[ \t]+.[ \t\r\n]+", Pattern.CASE_INSENSITIVE);
    Matcher matcher = prefixPattern.matcher(contents);
    StringBuffer prefixes = new StringBuffer();
    StringBuffer sb = new StringBuffer("INSERT DATA { ");
    if(graph != null && graph.length() != 0) {
      sb.append("GRAPH <"+graph+"> { ");
    }
    sb.append("\r\n");
    while(matcher.find()) {
      prefixes.append("PREFIX ");
      prefixes.append(matcher.group(1));
      prefixes.append(" <");
      prefixes.append(matcher.group(2));
      prefixes.append(">\r\n");
      matcher.appendReplacement(sb, "");
    }
    matcher.appendTail(sb);
    prefixes.append(sb);
    if(graph != null && graph.length() != 0) {
      prefixes.append("}\r\n");
    }
    prefixes.append("}\r\n");
    sb = null;
    HttpURLConnection conn = null;
    Log.i(LOG_TAG, "Sending update to server:");
    Log.d(LOG_TAG, prefixes.toString());
    try {
      conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      //conn.setRequestProperty("Content-Length", Integer.toString(prefixes.length()));
      conn.setRequestProperty("Content-Type", "application/sparql-query");
      conn.setRequestProperty("Accept", "*/*");
      String userInfo = uri.getUserInfo();
      if(userInfo != null && userInfo.length() != 0) {
        if(!userInfo.contains(":")) {
          userInfo = userInfo + ":";
        }
        String encodedInfo = Base64.encodeToString(userInfo.getBytes("UTF-8"), Base64.NO_WRAP).trim();
        Log.d(LOG_TAG, "Authorization = "+encodedInfo);
        conn.setRequestProperty("Authorization", "Basic "+encodedInfo);
      }
      conn.connect();
      OutputStream os = conn.getOutputStream();
      PrintStream ps = new PrintStream(os);
      ps.print(prefixes);
      ps.close();
      int status = conn.getResponseCode();
      Log.d(LOG_TAG, "HTTP Status = " + status);
      if(status == 200) {
        success = true;
      } else if(status >= 400) {
        success = false;
        Log.w(LOG_TAG, "HTTP status for update was "+status);
        Log.w(LOG_TAG, "HTTP response msg was "+conn.getResponseMessage());
      }
      conn.disconnect();
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to malformed URL.");
    } catch (ProtocolException e) {
      Log.w(LOG_TAG, "Unable to perform HTTP POST to given URI.", e);
    } catch (IOException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to communication issue.", e);
    }
    return success;
  }
  
  /**
   * Performs a SPARQL 1.1 Update INSERT DATA operation on a remote triple
   * store by inserting the triples in <i>model</i> into the optionally named
   * graph <i>graph</i>
   * @param uri URI for the endpoint
   * @param model RDF model to send to the endpoint
   * @param graph Optional graph URI to insert data into. Pass null to insert
   * into the default graph.
   * @return true on success, false otherwise.
   */
  public static boolean insertDataToVirtuoso(URI uri, Model model, String graph) {
    boolean success = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    model.write(baos, "TTL");
    Log.d(LOG_TAG, "Byte array size: "+baos.size());
    // get model contents in turtle
    String contents = null;
    try {
      contents = baos.toString("UTF-8");
    } catch(UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Unable to encode query.", e);
      return false;
    }
    baos = null;
    Log.d(LOG_TAG, "Model: "+contents);
    // remove PREFIX statements and move them before an insert data block
    Pattern prefixPattern = Pattern.compile("@prefix[ \t]+([^:]*:)[ \t]+<([^>]+)>[ \t]+.[ \t\r\n]+", Pattern.CASE_INSENSITIVE);
    Matcher matcher = prefixPattern.matcher(contents);
    StringBuffer prefixes = new StringBuffer();
    StringBuffer sb = new StringBuffer("INSERT INTO GRAPH <");
    if(graph != null && graph.length() != 0) {
      sb.append(graph);
    }
    else {
    	sb.append("#");
    }
    sb.append("> {\r\n");
    while(matcher.find()) {
      prefixes.append("PREFIX ");
      prefixes.append(matcher.group(1));
      prefixes.append(" <");
      prefixes.append(matcher.group(2));
      prefixes.append(">\r\n");
      matcher.appendReplacement(sb, "");
    }
    matcher.appendTail(sb);
    prefixes.append(sb);
    prefixes.append("}\r\n");
    sb = null;
    HttpURLConnection conn = null;
    Log.i(LOG_TAG, "Sending update to server:");
    Log.d(LOG_TAG, prefixes.toString());
    try {
      conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      //conn.setRequestProperty("Content-Length", Integer.toString(prefixes.length()));
      //conn.setRequestProperty("Content-Type", "application/sparql-query");
      conn.setRequestProperty("Accept", "*/*");
      String userInfo = uri.getUserInfo();
      if(userInfo != null && userInfo.length() != 0) {
        if(!userInfo.contains(":")) {
          userInfo = userInfo + ":";
        }
        String encodedInfo = Base64.encodeToString(userInfo.getBytes("UTF-8"), Base64.NO_WRAP).trim();
        Log.d(LOG_TAG, "Authorization = "+encodedInfo);
        conn.setRequestProperty("Authorization", "Basic "+encodedInfo);
      }
      conn.connect();
      OutputStream os = conn.getOutputStream();
      PrintStream ps = new PrintStream(os);
      ps.print("query=" + URLEncoder.encode(prefixes.toString()));
      ps.close();
      int status = conn.getResponseCode();
      Log.d(LOG_TAG, "HTTP Status = " + status);
      if(status == 200) {
        success = true;
      } else if(status >= 400) {
        success = false;
        Log.w(LOG_TAG, "HTTP status for update was "+status);
        Log.w(LOG_TAG, "HTTP response msg was "+conn.getResponseMessage());
        Log.w(LOG_TAG, "HTTP response msg was "+conn.getContent().toString());
      }
      conn.disconnect();
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to malformed URL.");
    } catch (ProtocolException e) {
      Log.w(LOG_TAG, "Unable to perform HTTP POST to given URI.", e);
    } catch (IOException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to communication issue.", e);
    }
    return success;
  }

  /**
   * Performs a SPARQL 1.1 Update INSERT DATA operation on a remote triple
   * store by inserting the triples in <i>model</i> into the optionally named
   * graph <i>graph</i>
   * @param uri URI for the endpoint
   * @param model RDF model to send to the endpoint
   * @param graph Optional graph URI to insert data into. Pass null to insert
   * into the default graph.
   * @return true on success, false otherwise.
   */
  public static boolean insertDataToVirtuosoHttps(InputStream inputStream, URI uri, Model model, String graph) {  
      boolean success = false;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      model.write(baos, "TTL");
      Log.d(LOG_TAG, "Byte array size: "+baos.size());
      // get model contents in turtle
      String contents = null;
      try {
        contents = baos.toString("UTF-8");
      } catch(UnsupportedEncodingException e) {
        Log.e(LOG_TAG, "Unable to encode query.", e);
        return false;
      }
      baos = null;
      Log.d(LOG_TAG, "Model: "+contents);
      // remove PREFIX statements and move them before an insert data block
      Pattern prefixPattern = Pattern.compile("@prefix[ \t]+([^:]*:)[ \t]+<([^>]+)>[ \t]+.[ \t\r\n]+", Pattern.CASE_INSENSITIVE);
      Matcher matcher = prefixPattern.matcher(contents);
      StringBuffer prefixes = new StringBuffer();
      StringBuffer sb = new StringBuffer("INSERT INTO GRAPH <");
      if(graph != null && graph.length() != 0) {
        sb.append(graph);
      }
      else {
      	sb.append("#");
      }
      sb.append("> {\r\n");
      while(matcher.find()) {
        prefixes.append("PREFIX ");
        prefixes.append(matcher.group(1));
        prefixes.append(" <");
        prefixes.append(matcher.group(2));
        prefixes.append(">\r\n");
        matcher.appendReplacement(sb, "");
      }
      matcher.appendTail(sb);
      prefixes.append(sb);
      prefixes.append("}\r\n");
      sb = null;
      HttpsURLConnection conn = null;
      Log.i(LOG_TAG, "Sending update to server:");   
      Log.d(LOG_TAG, prefixes.toString());
      Log.d(LOG_TAG, "uri: " + uri);
      try {
        conn = (HttpsURLConnection) uri.toURL().openConnection();
        SSLContext context = generateSSLContext(inputStream);;
        conn.setSSLSocketFactory(context.getSocketFactory());
        conn.setHostnameVerifier(new BrowserCompatHostnameVerifier());
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        //conn.setRequestProperty("Content-Length", Integer.toString(prefixes.length()));
        //conn.setRequestProperty("Content-Type", "application/sparql-query");
        conn.setRequestProperty("Accept", "*/*");
        String userInfo = uri.getUserInfo();
        if(userInfo != null && userInfo.length() != 0) {
          if(!userInfo.contains(":")) {
            userInfo = userInfo + ":";
          }
          String encodedInfo = Base64.encodeToString(userInfo.getBytes("UTF-8"), Base64.NO_WRAP).trim();
          Log.d(LOG_TAG, "Authorization = "+encodedInfo);
          conn.setRequestProperty("Authorization", "Basic "+encodedInfo);
        }
        Log.d(LOG_TAG, "The connection URL is "+conn.getURL());
        conn.connect();
        OutputStream os = conn.getOutputStream();
        PrintStream ps = new PrintStream(os);
        ps.print("query=" + URLEncoder.encode(prefixes.toString()));
        ps.close();
        int status = conn.getResponseCode();
        Log.d(LOG_TAG, "HTTP Status = " + status);
        if(status == 200) {
          success = true;
        } else if(status >= 400) {
          success = false;
          Log.w(LOG_TAG, "HTTP status for update was "+status);
          Log.w(LOG_TAG, "HTTP response msg was "+conn.getResponseMessage());
          Log.w(LOG_TAG, "HTTP response msg was "+conn.getContent().toString());
        }
        conn.disconnect();
      } catch (MalformedURLException e) {
        Log.w(LOG_TAG, "Unable to insert triples due to malformed URL.");
      } catch (ProtocolException e) {
        Log.w(LOG_TAG, "Unable to perform HTTP POST to given URI.", e);
      } catch (IOException e) {
        Log.w(LOG_TAG, "Unable to insert triples due to communication issue.", e);
      }
      return success;
    }
  
  public static SSLContext generateSSLContext(InputStream inputStream) {
  	// Create an SSLContext that uses our TrustManager
  	Log.d(LOG_TAG, "This is within the generateSSLContext method.");
  	SSLContext context = null;
  	Log.d(LOG_TAG, "After the SSLContext initialization.");
  	try {
	  	// Load CAs from an InputStream
	  	// (could be from a resource or ByteArrayInputStream or ...)
	  	CertificateFactory cf = CertificateFactory.getInstance("X.509");
	  	// From https://www.washington.edu/itconnect/security/ca/load-der.crt
	  	Log.d(LOG_TAG, "Before loading the pem file");
	  	InputStream caInput = new BufferedInputStream(inputStream);
	  	Log.d(LOG_TAG, "The pem file is " + caInput.available());
	  	Certificate ca;
	  	try {
	  		ca = cf.generateCertificate(caInput);	
	  	} finally {
	  		caInput.close();
	  	}
	  	
	  	// Create a KeyStore containing our trusted CAs
	  	String keyStoreType = KeyStore.getDefaultType();
	  	KeyStore keyStore = KeyStore.getInstance(keyStoreType);
	  	keyStore.load(null, null);
	  	keyStore.setCertificateEntry("ca", ca);
	
	  	// Create a TrustManager that trusts the CAs in our KeyStore
	  	String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
	  	TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
	  	tmf.init(keyStore);
	
	  	context = SSLContext.getInstance("TLS");
	  	context.init(null, tmf.getTrustManagers(), null);
  	} catch (CertificateException e) {
      Log.e(LOG_TAG, "The CertificateException message is "+e.getLocalizedMessage());
  	} catch (IOException e) {
      Log.e(LOG_TAG, "The IOException message is "+e.getLocalizedMessage());  		
  	} catch (KeyStoreException e) {
      Log.e(LOG_TAG, "The KeyStoreException message is "+e.getLocalizedMessage());  		
  	} catch (NoSuchAlgorithmException e) {
      Log.e(LOG_TAG, "The NoSuchAlgorithmException message is "+e.getLocalizedMessage()); 		
  	} catch (KeyManagementException e) {
      Log.e(LOG_TAG, "The KeyManagementException message is "+e.getLocalizedMessage()); 		
  	} 
  	return context;
 }
  
  /**
   * Performs a SPARQL 1.1 Update DELETE DATA operation on a remote triple
   * store by deleting the triples in <i>model</i> into the optionally named
   * graph <i>graph</i>
   * @param uri URI for the endpoint
   * @param model RDF model containing the data to be deleted from the endpoint
   * @param graph Optional graph URI to delete data from. Pass null to delete
   * from the default graph.
   * @return true on success, false otherwise.
   */
  public static boolean deleteData(URI uri, Model model, String graph) {
    boolean success = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    model.write(baos, "TTL");
    Log.d(LOG_TAG, "Byte array size: "+baos.size());
    // get model contents in turtle
    String contents = null;
    try {
      contents = baos.toString("UTF-8");
    } catch(UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Unable to encode query.", e);
      return false;
    }
    baos = null;
    Log.d(LOG_TAG, "Model: "+contents);
    // remove PREFIX statements and move them before a delete data block
    Pattern prefixPattern = Pattern.compile("@prefix[ \t]+([^:]*:)[ \t]+<([^>]+)>[ \t]+.[ \t\r\n]+", Pattern.CASE_INSENSITIVE);
    Matcher matcher = prefixPattern.matcher(contents);
    StringBuffer prefixes = new StringBuffer();
    StringBuffer sb = new StringBuffer("DELETE DATA { ");
    if(graph != null && graph.length() != 0) {
      sb.append("GRAPH <"+graph+"> { ");
    }
    sb.append("\r\n");
    while(matcher.find()) {
      prefixes.append("PREFIX ");
      prefixes.append(matcher.group(1));
      prefixes.append(" <");
      prefixes.append(matcher.group(2));
      prefixes.append(">\r\n");
      matcher.appendReplacement(sb, "");
    }
    matcher.appendTail(sb);
    prefixes.append(sb);
    if(graph != null && graph.length() != 0) {
      prefixes.append("}\r\n");
    }
    prefixes.append("}\r\n");
    sb = null;
    HttpURLConnection conn = null;
    Log.i(LOG_TAG, "Sending update to server:");
    Log.d(LOG_TAG, prefixes.toString());
    try {
      conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Length", Integer.toString(prefixes.length()));
      conn.setRequestProperty("Content-Type", "application/sparql-update");
      conn.setRequestProperty("Accept", "*/*");
      String userInfo = uri.getUserInfo();
      if(userInfo != null && userInfo.length() != 0) {
        if(!userInfo.contains(":")) {
          userInfo = userInfo + ":";
        }
        String encodedInfo = Base64.encodeToString(userInfo.getBytes("UTF-8"), Base64.NO_WRAP).trim();
        Log.d(LOG_TAG, "Authorization = "+encodedInfo);
        conn.setRequestProperty("Authorization", "Basic "+encodedInfo);
      }
      conn.connect();
      OutputStream os = conn.getOutputStream();
      PrintStream ps = new PrintStream(os);
      ps.print(prefixes);
      ps.close();
      int status = conn.getResponseCode();
      Log.d(LOG_TAG, "HTTP Status = " + status);
      if(status == 200) {
        success = true;
      } else if(status >= 400) {
        success = false;
        Log.w(LOG_TAG, "HTTP status for update was "+status);
      }
      conn.disconnect();
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to malformed URL.");
    } catch (ProtocolException e) {
      Log.w(LOG_TAG, "Unable to perform HTTP POST to given URI.", e);
    } catch (IOException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to communication issue.", e);
    }
    return success;
  }

  public static YailList resultSetUsingYailDictionary(ResultSet results) {
    List<YailDictionary> bindings = new ArrayList<>();
    while (results.hasNext()) {
      QuerySolution s = results.next();
      Iterator<String> varNames = s.varNames();
      YailDictionary binding = new YailDictionary();
      while (varNames.hasNext()) {
        String var = varNames.next();
        RDFNode node = s.get(var);
        if (node.isResource()) {
          binding.put(var, node.toString());
        } else if (node.isLiteral()) {
          Literal l = node.asLiteral();
          if (l.getDatatype() != null) {
            RDFDatatype datatype = l.getDatatype();
            if (XSDDatatype.XSDboolean.equals(datatype)) {
              binding.put(var, l.getBoolean());
            } else if (INTEGER_TYPES.contains(l.getDatatype())) {
              binding.put(var, l.getInt());
            } else if (DOUBLE_TYPES.contains(l.getDatatype())) {
              binding.put(var, l.getDouble());
            } else if (XSDDatatype.XSDdate.equals(datatype)) {
              String[] parts = l.getString().split("-");
              Calendar cal = GregorianCalendar.getInstance();
              cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
              binding.put(var, node.toString());
            } else if (XSDDatatype.XSDdateTime.equals(datatype)) {
              // TODO(ewpatton): Implementation
              String[] parts = l.getString().split("T");
              String[] dateParts = parts[0].split("-");
              String[] timeParts = parts[1].split(":");
              Calendar cal = GregorianCalendar.getInstance();
              cal.set(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]),
                  Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]), Integer.parseInt(timeParts[2]));
            } else {
              binding.put(var, l.getString());
            }
          } else {
            binding.put(var, l.getString());
          }
        } else {
          Log.d(LOG_TAG, "Unexpected type: " + node.getClass());
          binding.put(var, node.toString());
        }
      }
      bindings.add(binding);
    }
    Log.d(LOG_TAG, bindings.toString());
    return YailList.makeList(bindings);
  }
  
  public static YailList resultSetAsYailList(ResultSet results) {
    final Collection<Solution> solutions = RdfUtil.resultSetAsCollection( results );
    final List<YailList> list = new ArrayList<YailList>();
    for ( Solution i : solutions ) {
      List<YailList> solution = new ArrayList<YailList>();
      for ( VariableBinding j : i ) {
        solution.add( YailList.makeList( j ) );
      }
      list.add( YailList.makeList( solution ) );
    }
    return YailList.makeList( list );
  }
  
  /**
   * Performs a POST to a remote CSPARQL Engine feed
   * @param uri URI for the endpoint
   * @param model RDF model to send to the endpoint
   * @return true on success, false otherwise.
   */
  public static boolean feedData(URI uri, Model model) {
    boolean success = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    model.write(baos, "TTL");
    Log.d(LOG_TAG, "Byte array size: "+baos.size());
    // get model contents in turtle
    String contents = null;
    try {
      contents = baos.toString("UTF-8");
    } catch(UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Unable to encode query.", e);
      return false;
    }
    baos = null;
    
    HttpURLConnection conn = null;
    Log.i(LOG_TAG, "Sending update to server:");
    Log.d(LOG_TAG, contents);
    try {
      conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
      String userInfo = uri.getUserInfo();
      if(userInfo != null && userInfo.length() != 0) {
        if(!userInfo.contains(":")) {
          userInfo = userInfo + ":";
        }
        String encodedInfo = Base64.encodeToString(userInfo.getBytes("UTF-8"), Base64.NO_WRAP).trim();
        Log.d(LOG_TAG, "Authorization = "+encodedInfo);
        conn.setRequestProperty("Authorization", "Basic "+encodedInfo);
      }
      conn.connect();
      OutputStream os = conn.getOutputStream();
      PrintStream ps = new PrintStream(os);
      ps.print(contents);
      ps.close();
      int status = conn.getResponseCode();
      Log.d(LOG_TAG, "HTTP Status = " + status);
      if(status == 200) {
        success = true;
      } else if(status >= 400) {
        success = false;
        Log.w(LOG_TAG, "HTTP status for update was "+status);
      }
      conn.disconnect();
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to malformed URL.");
    } catch (ProtocolException e) {
      Log.w(LOG_TAG, "Unable to perform HTTP POST to given URI.", e);
    } catch (IOException e) {
      Log.w(LOG_TAG, "Unable to insert triples due to communication issue.", e);
    }
    return success;
  }

  public static String prepStreamingQuery(String querytext, String regId, String streamName, String window, String step) {
    Log.d(LOG_TAG, "before parsing querytext");
    //Query query = QueryFactory.parse(null, querytext, "", Syntax.syntaxSPARQL_11);
    Query query = QueryFactory.create(querytext);
    
    Log.d(LOG_TAG, "before adding binding variable for UUID");
    query.addResultVar("uuid", NodeValue.makeString(regId));
    
    Log.d(LOG_TAG, "before adding ex.org named graph");
    query.addNamedGraphURI("http://ex.org/gcm");
    
    Log.d(LOG_TAG, "query before replacing FROM clause: " + query.toString());
    String queryStr = query.toString().replace("FROM NAMED <http://ex.org/gcm>", "FROM STREAM <" + streamName + "> [RANGE " + window + " STEP " + step + "]");
    //queryStr = "REGISTER QUERY " + regId + " AS " + queryStr;
    return queryStr;
  }
  
	public static boolean performHttpsRequest(String urlString, InputStream inputStream,
			String securityToken, String filePath) throws IOException {
		boolean success = false;
		HttpsURLConnection conn = null;
		DataOutputStream dos = null;

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
		
		try {
			URL url = new URL(urlString);
			conn = (HttpsURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="
					+ boundary);
			conn.setRequestProperty("securityToken", securityToken);
			
			SSLContext context = generateSSLContext(inputStream);
			conn.setSSLSocketFactory(context.getSocketFactory());
			conn.setHostnameVerifier(new BrowserCompatHostnameVerifier());

			dos = new DataOutputStream(conn.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
					+ filePath + "\"" + lineEnd);
			dos.writeBytes(lineEnd);

			filePath = filePath.replace("file:/", "");
			FileInputStream fileInputStream = new FileInputStream(new File(filePath));
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			while (bytesRead > 0) {
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
			conn.getInputStream();
			fileInputStream.close();
			dos.flush();
			
      int status = conn.getResponseCode();
      Log.d(LOG_TAG, "HTTPS Status = " + status);
      if(status == 200) {
        success = true;
      } else {
        Log.w(LOG_TAG, "HTTPS status for update was "+status);
        Log.w(LOG_TAG, "HTTPS response msg was "+conn.getResponseMessage());
      }
		} finally {
			dos.close();
			conn.disconnect();
		}
		return success;
	}
}
