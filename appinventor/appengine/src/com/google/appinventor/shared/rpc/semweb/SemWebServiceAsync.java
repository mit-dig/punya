// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2013-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.shared.rpc.semweb;

import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.List;
import java.util.Map;

/**
 * Asynchronous definition of SemWebService
 * 
 * @see SemWebService
 * @author Evan W. Patton (ewpatton@gmail.com)
 *
 */
public interface SemWebServiceAsync {
  /**
   * Asynchronous search for classes, see {@link SemWebService#searchClasses(String)}.
   * @param text User input search text.
   * @param callback Handler to fire when data is received.
   */
  void searchClasses(String text, AsyncCallback<List<Map<String, String>>> callback);

  /**
   * Asynchronous search for properties, see {@link SemWebService#searchProperties(String)}.
   * @param text User input search text.
   * @param callback Handler to fire when data is received.
   */
  void searchProperties(String text, AsyncCallback<List<Map<String, String>>> callback);
  
  void getProperties(String concept, AsyncCallback<List<String>> callback);
}
