package com.google.appinventor.components.runtime.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public final class RdfUtil {
  public static final String LOG_TAG = RdfUtil.class.getSimpleName();

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
      if(value.getDatatype().equals(XSDDatatype.XSDinteger)) {
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
  }

  public static ResultSet executeSELECT(String endpoint, String queryText) {
    Query query = QueryFactory.create(queryText);
    QueryEngineHTTP qe = QueryExecutionFactory.createServiceRequest(endpoint, query);
    qe.setSelectContentType("application/sparql-results+json");
    if(!query.isSelectType()) {
      return null;
    }
    return qe.execSelect();
  }

  public static Collection<Solution> resultSetAsCollection(ResultSet results) {
    List<Solution> list = new LinkedList<Solution>();
    while(results.hasNext()) {
      list.add(new Solution(results.next()));
    }
    return list;
  }

}
