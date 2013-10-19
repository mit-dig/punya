package com.google.appinventor.shared.rpc.semweb;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Asynchronous definition of SemWebService
 * 
 * @see SemWebService
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public interface SemWebServiceAsync {
  /**
   * Asynchronous initialization, see {@link SemWebService#initialize()}.
   * @deprecated
   * @param callback
   */
  void initialize(AsyncCallback<java.lang.Void> callback);

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
}
