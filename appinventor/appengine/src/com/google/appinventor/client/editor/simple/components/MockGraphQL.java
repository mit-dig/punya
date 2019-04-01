// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2019-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.gwt.user.client.ui.Image;

public class MockGraphQL extends MockNonVisibleComponent {

  public static final String TYPE = "GraphQL";
  private static final String PROPERTY_GQL_ENDPOINT_URL = "GqlEndpointUrl";
  private static final String PROPERTY_GQL_HTTP_HEADERS = "GqlHttpHeaders";

  /**
   * Creates a new instance of a non-visible component whose icon is loaded dynamically (not part of the icon image
   * bundle).
   */
  public MockGraphQL(SimpleEditor editor, String type, Image iconImage) {
    super(editor, type, iconImage);
  }

  @Override
  public void onComponentCreated() {
    super.onComponentCreated();

    // Update schema.
    onPropertyChange(PROPERTY_GQL_ENDPOINT_URL, getPropertyValue(PROPERTY_GQL_ENDPOINT_URL));
  }

  @Override
  public void onPropertyChange(String propertyName, String newValue) {
    super.onPropertyChange(propertyName, newValue);

    // If the property change was the endpoint, update the schema.
    if (PROPERTY_GQL_ENDPOINT_URL.equals(propertyName) && !newValue.isEmpty()) {
      register(newValue, getPropertyValue(PROPERTY_GQL_HTTP_HEADERS));
    }

    // If the property change was the headers, update the schema.
    if (PROPERTY_GQL_HTTP_HEADERS.equals(propertyName) && !getPropertyValue(PROPERTY_GQL_ENDPOINT_URL).isEmpty()) {
      register(getPropertyValue(PROPERTY_GQL_ENDPOINT_URL), newValue);
    }
  }

  @Override
  public void onRemoved() {
    unregister();
  }

  /**
   * Registers this instance with a given endpoint.
   *
   * @param url the endpoint to use.
   * @param headers the headers to use.
   */
  private native void register(String url, String headers) /*-{
    var uid = this.@com.google.appinventor.client.editor.simple.components.MockGraphQL::getUuid()();
    Blockly.GraphQLBlock.registerInstance(uid, url, headers);
  }-*/;

  /**
   * Unregisters this instance.
   */
  private native void unregister() /*-{
    var uid = this.@com.google.appinventor.client.editor.simple.components.MockGraphQL::getUuid()();
    Blockly.GraphQLBlock.unregisterInstance(uid);
  }-*/;
}
