package com.google.appinventor.shared.rpc.semweb;

import com.google.gwt.i18n.client.Constants;

/**
 * Exposes constants from the SemWebConstants.properties file for
 * use on the AppInventor client.
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public interface SemWebConstants extends Constants {
  /**
   * Default endpoint to use for populating UI elements with semantic content.
   * @return
   */
  String defaultEndpoint();

  /**
   * An array of strings representing dereferencable URIs used for
   * identifying class/property definitions.
   * @return
   */
  String[] ontologies();
}
