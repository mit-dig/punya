package com.google.appinventor.shared.rpc.semweb;

import java.util.List;
import java.util.Map;

import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Interface defining the API used to expose the ontological information
 * captured by the semantic web features in AppInventor to clients for
 * the purpose of building a better user interface to support linked
 * data generation.
 * 
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
@RemoteServiceRelativePath(ServerLayout.SEMWEB_SERVICE)
public interface SemWebService extends RemoteService {
  /**
   * Was used to initialize, now handled in &lt;clinit&gt;
   * @deprecated
   */
  void initialize();

  /**
   * Searches the <code>rdfs:label</code> of <code>owl:Class</code>es to
   * find labels containing the string text.
   * @param text Substring sent from client used to search the knowledge base
   * for classes with relevant names
   * @return An array of dictionaries containing value and label information
   * of classes matched by the search.
   */
  List<Map<String, String>> searchClasses(String text);

  /**
   * Searches the <code>rdfs:label</code> of <code>owl:ObjectProperty</code>
   * and <code>owl:DatatypeProperty</code> objects to find labels containing 
   * the string <em>text</em>.
   * @param text Substring sent from client used to search the knowledge base
   * for properties with relevant names
   * @return An array of dictionaries containing value and label information
   * of properties matched by the search.
   */
  List<Map<String, String>> searchProperties(String text);
}
