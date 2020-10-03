package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.SimpleObject;
import com.hp.hpl.jena.rdf.model.Model;

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

  protected Model getModel() {
    return model;
  }
}
