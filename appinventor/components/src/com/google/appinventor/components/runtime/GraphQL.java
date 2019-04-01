// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.errors.PermissionException;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.FileUtil;
import com.google.appinventor.components.runtime.util.GingerbreadUtil;
import com.google.appinventor.components.runtime.util.JsonUtil;
import gnu.lists.FString;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The {@link GraphQL} component communicates with a GraphQL endpoint to execute queries and mutations. It represents
 * returned data as a dictionary. All queries have an associated operation name and are executed asynchronously.
 * Completed queries trigger different events depending on whether there the queries had any errors.
 *
 * @author lujingcen@gmail.com (Lujing Cen)
 */
@DesignerComponent(version = YaVersion.GRAPHQL_COMPONENT_VERSION,
    description = "Non-visible component that interacts with a GraphQL endpoint.",
    designerHelpDescription = "Non-visible component that interacts with a GraphQL endpoint.",
    category = ComponentCategory.EXPERIMENTAL,
    nonVisible = true,
    iconName = "images/graphQL.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "json.jar")
public class GraphQL extends AndroidNonvisibleComponent implements Component {
  private static final String LOG_TAG = "GraphQL";

  private final Handler androidUIHandler;
  private final Activity activity;
  private final CookieHandler cookieHandler;

  private Map<String, List<String>> cookiesMap;
  private Map<String, List<String>> headersMap;

  private String endpointURL;
  private String httpHeaders;

  /**
   * Creates a new GraphQL component.
   *
   * @param container the form that this component is contained in.
   */
  public GraphQL(ComponentContainer container) {
    super(container.$form());

    this.androidUIHandler = new Handler();
    this.activity = container.$context();
    this.cookieHandler = GingerbreadUtil.newCookieManager();

    // Create empty maps.
    this.cookiesMap = new HashMap<>();
    this.headersMap = new HashMap<>();

    // Log creation of component.
    Log.d(LOG_TAG, "Created GraphQL component.");
  }

  /**
   * Getter for the GraphQL endpoint URL.
   *
   * @return the URL for this GraphQL instance.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Gets the URL for this GraphQL component.",
      userVisible = false)
  public String GqlEndpointUrl() {
    return endpointURL;
  }

  /**
   * Getter for the GraphQL HTTP headers.
   *
   * @return the HTTP headers for this GraphQL instance.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Gets the HTTP headers for this GraphQL component.",
      userVisible = false)
  public String GqlHttpHeaders() {
    return httpHeaders;
  }

  /**
   * Specifies the URL for this GraphQL component.
   *
   * @param gqlUrl the URL for this GraphQL component.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Sets the URL for this GraphQL component.")
  public void GqlEndpointUrl(final String gqlUrl) {
    // Set the new URL.
    endpointURL = gqlUrl;

    // Log URL change.
    Log.d(LOG_TAG, "Endpoint URL changed to " + gqlUrl + ".");
  }

  /**
   * Specifies the HTTP headers for this GraphQL component as a JSON dictionary.
   *
   * @param gqlHttpHeaders the HTTP headers this GraphQL component.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Sets the HTTP headers for this GraphQL component.")
  public void GqlHttpHeaders(final String gqlHttpHeaders) {
    // Set the new HTTP headers string.
    httpHeaders = gqlHttpHeaders;

    // Log header change.
    Log.d(LOG_TAG, "HTTP headers changed to " + gqlHttpHeaders + ".");

    // Clear old headers.
    headersMap.clear();

    // Empty headers should be ignored.
    if (httpHeaders == null || httpHeaders.isEmpty()) {
      return;
    }

    // Parse the header to a JSON object.
    final JSONObject jsonObject;
    try {
      jsonObject = (JSONObject) (new JSONTokener(httpHeaders)).nextValue();
    } catch (final JSONException | ClassCastException e) {
      form.dispatchErrorOccurredEvent(this, "GqlHttpHeaders",
          ErrorMessages.ERROR_GQL_INVALID_HTTP_HEADERS);
      return;
    }

    // Populate the map.
    final Iterator keyIterator = jsonObject.keys();
    while (keyIterator.hasNext()) {
      // The key should be a string.
      final String name = (String) keyIterator.next();

      // Get the value.
      final String value;
      try {
        value = jsonObject.getString(name);
      } catch (final JSONException e) {
        form.dispatchErrorOccurredEvent(this, "GqlHttpHeaders",
            ErrorMessages.ERROR_GQL_INVALID_HTTP_HEADERS);
        return;
      }

      // Split by commas.
      final String[] headerValues = value.split(",");

      // Trim all values.
      for (int i = 0; i < headerValues.length; i++) {
        headerValues[i] = headerValues[i].trim();
      }

      // Add entry to map.
      headersMap.put(name, Arrays.asList(headerValues));
    }
  }

  /**
   * Triggers an event indicating that the given operation has successfully executed and returned data. This method
   * should be executed in the application's main thread.
   *
   * @param gqlQueryName the query name associated with this event.
   * @param gqlResponse  a non-empty response map containing data from executing the associated query.
   */
  @SimpleEvent(description = "Event triggered by the \"Query\" method.")
  public void GqlGotResponse(final String gqlQueryName, final Object gqlResponse) {
    EventDispatcher.dispatchEvent(this, "GqlGotResponse", gqlQueryName, gqlResponse);

    // Log event dispatch.
    Log.d(LOG_TAG, "Dispatched response event for " + gqlQueryName + ".");
  }

  /**
   * Triggers an event indicating that there were one or more errors when executing the query. This method should be
   * executed in the application's main thread.
   *
   * @param gqlQueryName the query name associated with this event.
   * @param gqlError     a list of error messages, which must be non-empty.
   */
  @SimpleEvent(description = "Indicates that the GraphQL endpoint responded with an error.")
  public void GqlGotError(final String gqlQueryName, final List<String> gqlError) {
    EventDispatcher.dispatchEvent(this, "GqlGotError", gqlQueryName, gqlError);

    // Log event dispatch.
    Log.d(LOG_TAG, "Dispatched error event for " + gqlQueryName + ".");
  }

  /**
   * Executes an arbitrary query against the GraphQL endpoint.
   *
   * @param gqlQueryName the name for this query.
   * @param gqlQuery     the query string to execute.
   */
  @SimpleFunction(description = "Execute a GraphQL query against the endpoint.")
  public void GqlQuery(final String gqlQueryName, final String gqlQuery) {
    // Method name for error handling.
    final String METHOD = "GqlQuery";

    // Build the post data.
    final byte[] postData = buildPost(gqlQuery, null, null);

    // Asynchronously complete request.
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          performRequest(gqlQueryName, postData);
        } catch (final PermissionException e) {
          form.dispatchPermissionDeniedEvent(GraphQL.this, METHOD, e);
        } catch (final FileUtil.FileException e) {
          form.dispatchErrorOccurredEvent(GraphQL.this, METHOD, e.getErrorMessageNumber());
        } catch (final Exception e) {
          form.dispatchErrorOccurredEvent(GraphQL.this, METHOD,
              ErrorMessages.ERROR_GQL_UNABLE_TO_POST, e.toString());
        }
      }
    });

    // Log query request.
    Log.d(LOG_TAG, "Query for " + gqlQueryName + " has been enqueued.");
  }

  /**
   * Gets the bytes for the body of a GraphQL query POST request.
   *
   * @param query         the input query string.
   * @param operationName the operation name of the query, which can be null.
   * @param variables     the variables associated with this query, which can be null.
   * @return a byte array representing the POST request body.
   */
  private static byte[] buildPost(final String query, final String operationName, final Map<String, Object> variables) {
    try {
      // Construct the GraphQL query in standard JSON format.
      final JSONObject queryBody = new JSONObject();
      queryBody.put("query", query);
      queryBody.put("operationName", (operationName == null) ? JSONObject.NULL : operationName);
      queryBody.put("variables", (variables == null) ? JSONObject.NULL : new JSONObject(variables));

      // Log query.
      Log.d(LOG_TAG, "Building query " + queryBody + ".");

      // Get the byte encoding.
      return queryBody.toString().getBytes(StandardCharsets.UTF_8);
    } catch (final JSONException e) {
      // We do not expect to get here.
      Log.e(LOG_TAG, "Error building post body.", e);
      throw new RuntimeException(e);
    }
  }

  private void performRequest(final String queryName, final byte[] postData) throws IOException {
    // Open the connection.
    final HttpURLConnection connection = openConnection();

    // Always close the connection.
    try {
      // Set the length of data to be posted.
      connection.setFixedLengthStreamingMode(postData.length);

      // Write the data.
      try (BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
        out.write(postData, 0, postData.length);
        out.flush();
      }

      // Process the cookies.
      processResponseCookies(connection);

      // Handle the response.
      handleResponse(queryName, connection);
    } finally {
      // Destroy the connection.
      connection.disconnect();
    }
  }

  /**
   * Open a connection with the appropriate headers and cookies.
   */
  private HttpURLConnection openConnection() throws IOException {
    // Open a connection to the endpoint.
    final URL url = new URL(endpointURL);
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    // We are sending a POST request.
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);

    // Add custom request headers.
    for (final Map.Entry<String, List<String>> header : headersMap.entrySet()) {
      final String name = header.getKey();
      for (final String value : header.getValue()) {
        connection.addRequestProperty(name, value);
      }
    }

    // Set the content type.
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

    // Get the cookies.
    try {
      cookiesMap.clear();
      cookiesMap = cookieHandler.get(url.toURI(), headersMap);
    } catch (final IOException | URISyntaxException e) {
      // Sorry, no cookies for you.
    }

    // Add cookies.
    for (final Map.Entry<String, List<String>> cookie : cookiesMap.entrySet()) {
      final String name = cookie.getKey();
      for (String value : cookie.getValue()) {
        connection.addRequestProperty(name, value);
      }
    }

    return connection;
  }

  /**
   * Processes cookies from an HTTP connection object.
   *
   * @param connection the HTTP connection object.
   */
  private void processResponseCookies(final HttpURLConnection connection) {
    try {
      final Map<String, List<String>> headerFields = connection.getHeaderFields();
      cookieHandler.put(connection.getURL().toURI(), headerFields);
    } catch (final IOException | URISyntaxException e) {
      // Sorry, no cookies for you.
    }
  }

  /**
   * Gets the response string for an HTTP connection object.
   *
   * @param connection the HTTP connection object.
   * @return the response content as a string.
   */
  private static String getResponseContent(final HttpURLConnection connection) throws IOException {
    // Use the content encoding to convert bytes to characters.
    String encoding = connection.getContentEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }

    // Determine the input stream.
    InputStream inputStream;
    try {
      inputStream = connection.getInputStream();
    } catch (final IOException e) {
      inputStream = connection.getErrorStream();
    }

    if (inputStream == null) {
      return null;
    }

    // Read in chunks.
    try (final InputStreamReader reader = new InputStreamReader(inputStream, encoding)) {
      final int contentLength = connection.getContentLength();
      final StringBuilder sb = (contentLength != -1)
          ? new StringBuilder(contentLength)
          : new StringBuilder();
      final char[] buf = new char[1024];

      int read;
      while ((read = reader.read(buf)) != -1) {
        sb.append(buf, 0, read);
      }

      return sb.toString();
    }
  }

  private void dispatchError(final String queryName, final String error) {
    dispatchError(queryName, Collections.singletonList(error));
  }

  private void dispatchError(final String queryName, final List<String> errors) {
    // Dispatch the event.
    androidUIHandler.post(new Runnable() {
      @Override
      public void run() {
        GqlGotError(queryName, errors);
      }
    });
  }

  private void handleResponse(final String queryName, final HttpURLConnection connection) throws IOException {
    // Get the response string.
    final String responseString = getResponseContent(connection);

    // If there is no response, indicate the response code.
    if (responseString == null) {
      dispatchError(queryName, "Got unexpected response code " + connection.getResponseCode() + ".");
    }

    // Any JSON errors should indicate that the response is malformed.
    try {
      // Parse the response JSON into a known format for further processing.
      final JSONObject responseMap = (JSONObject) (new JSONTokener(responseString)).nextValue();

      // If there were errors, trigger the appropriate event.
      if (responseMap.has("errors")) {
        // Get the error JSON array.
        final JSONArray jsonArray = responseMap.getJSONArray("errors");

        // Construct a list of error messages.
        final List<String> errorMessages = new ArrayList<String>();

        // Populate error messages.
        for (int i = 0; i < jsonArray.length(); i++) {
          final JSONObject errorObject = jsonArray.getJSONObject(i);
          errorMessages.add(errorObject.getString("message"));
        }

        // Dispatch errors.
        dispatchError(queryName, errorMessages);
      }

      // If there were data entries, trigger the appropriate event.
      if (responseMap.has("data")) {
        // Extract data from response and convert to a list of list representation.
        final JSONObject jsonObject = responseMap.getJSONObject("data");
        final Object listOfListData = JsonUtil.convertJsonItem(jsonObject);

        // Post data on the application's main UI thread.
        androidUIHandler.post(new Runnable() {
          @Override
          public void run() {
            GqlGotResponse(queryName, listOfListData);
          }
        });
      }
    } catch (final JSONException e) {
      dispatchError(queryName, "Response JSON is malformed.");
    }
  }

  // TODO(bobbyluig): Write this into the scheme runtime.
  public static FString quote(final Object value) {
    if (value instanceof GqlLiteral) {
      return new FString(value.toString());
    } else {
      return new FString(JSONObject.quote(value.toString()));
    }
  }

  public static class GqlLiteral implements CharSequence {
    private final String value;

    public GqlLiteral(final String value) {
      this.value = value;
    }

    @Override
    public int length() {
      return value.length();
    }

    @Override
    public char charAt(int index) {
      return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return value.subSequence(start, end);
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
