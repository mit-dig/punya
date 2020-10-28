// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.content.Context;
import android.webkit.JavascriptInterface;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;

import com.google.appinventor.components.runtime.util.EclairUtil;
import com.google.appinventor.components.runtime.util.FroyoUtil;
import com.google.appinventor.components.runtime.util.SdkLevel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;

import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Component for displaying web pages
 * This is a very limited form of browser.  You can view web pages and
 * click on links. It also handles  Javascript. There are lots of things that could be added,
 * but this component is mostly for viewing individual pages.  It's not intended to take
 * the place of the browser.
 *
 * @author halabelson@google.com (Hal Abelson)
 */


@DesignerComponent(version = YaVersion.DATA_VIS_COMPONENT_VERSION,
    description = "Visible component that graphs 3 possible types of inputs:" +
                  " 1) SparQL Queries to linked data endpoints, 2) Google Spreadsheets," +
                  " and 3) JSON data. This is all done using Javascript visualization libraries" +
                  " on the WebViewer component.",
    category = ComponentCategory.INTERNAL,
    iconName = "images/graph.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_COARSE_LOCATION, "
    + "android.permission.ACCESS_FINE_LOCATION, "
    + "android.permission.ACCESS_WIFI_STATE, "
    + "android.permission.ACCESS_NETWORK_STATE, "
    + "android.permission.BLUETOOTH, "
    + "android.permission.INTERNET, "
    + "android.permission.WRITE_EXTERNAL_STORAGE")

public final class Graph extends AndroidViewComponent {

  private final WebView webview;

  // URL for the WebViewer to load initially
  // private String homeUrl;

  // whether or not to follow links when they are tapped
  private boolean followLinks = true;

  // Whether or not to prompt for permission in the WebViewer
  private boolean prompt = true;

  // ignore SSL Errors (mostly certificate errors. When set
  // self signed certificates should work.

  private boolean ignoreSslErrors = false;

  // allows passing strings to javascript
  WebViewInterface wvInterface;

  //path to javascript library uploaded by user
  private String jsLibraryPath = "";

  /**
   * Creates a new WebViewer component.
   *
   * @param container  container the component will be placed in
   */
  public Graph(ComponentContainer container) {
    super(container);

    webview = new WebView(container.$context());
    resetWebViewClient();       // Set up the web view client
    webview.getSettings().setJavaScriptEnabled(true);
    webview.setFocusable(true);
    // adds a way to send strings to the javascript
    wvInterface = new WebViewInterface(webview.getContext());
    webview.addJavascriptInterface(wvInterface, "AppInventor");
    // enable pinch zooming and zoom controls
    webview.getSettings().setBuiltInZoomControls(true);

    container.$add(this);

    webview.loadUrl("http://mit-dig.github.io/punya-webservices/");
    Width(LENGTH_FILL_PARENT);
    Height(LENGTH_FILL_PARENT);
  }

  /**
   * Gets the web view string
   *
   * @return string
   */
  @SimpleProperty(description = "Gets the WebView's String, which is viewable through " +
      "Javascript in the WebView as the window.AppInventor object",
      category = PropertyCategory.BEHAVIOR)
  private String WebViewString() {
    return wvInterface.getWebViewString();
  }

  /**
   * Sets the web view string
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  private void WebViewString(String newString) {
    wvInterface.setWebViewString(newString);
  }

  @Override
  public View getView() {
    return webview;
  }

  // Create a class so we can override the default link following behavior.
  // The handler doesn't do anything on its own.  But returning true means that
  // this do nothing will override the default WebVew behavior.  Returning
  // false means to let the WebView handle the Url.  In other words, returning
  // true will not follow the link, and returning false will follow the link.
  private class WebViewerClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      return !followLinks;
    }
  }

  // Components don't normally override Width and Height, but we do it here so that
  // the automatic width and height will be fill parent.
  @Override
  @SimpleProperty()
  public void Width(int width) {
    if (width == LENGTH_PREFERRED) {
      width = LENGTH_FILL_PARENT;
    }
    super.Width(width);
  }

  @Override
  @SimpleProperty()
  public void Height(int height) {
    if (height == LENGTH_PREFERRED) {
      height = LENGTH_FILL_PARENT;
    }
    super.Height(height);
  }


  /**
   * Returns the URL currently being viewed
   *
   * @return URL of the page being viewed
   */
  @SimpleProperty(
      description = "URL of the page currently viewed.   This could be different from the " +
          "Home URL if new pages were visited by following links.",
      category = PropertyCategory.BEHAVIOR)
  private String CurrentUrl() {
    return (webview.getUrl() == null) ? "" : webview.getUrl();
  }

  /**
   * Returns the title of the page currently being viewed
   *
   * @return title of the page being viewed
   */
  @SimpleProperty(
      description = "Title of the page currently viewed",
      category = PropertyCategory.BEHAVIOR)
  private String CurrentPageTitle() {
    return (webview.getTitle() == null) ? "" : webview.getTitle();
  }


  /**
   *  Load the given URL
   */
  @SimpleFunction(
      description = "Load the page at the given URL.")
  private void GoToUrl(String url) {
    webview.loadUrl(url);
  }


  private void resetWebViewClient() {
    if (SdkLevel.getLevel() >= SdkLevel.LEVEL_FROYO) {
      webview.setWebViewClient(FroyoUtil.getWebViewClient(ignoreSslErrors, followLinks, container.$form(), this));
    } else {
      webview.setWebViewClient(new WebViewerClient()); //setupWebViewClient();
    }
  }

  // private void setupWebViewClient() {

  //   webview.setWebViewClient(new WebViewClient() {
  //       private int running = 0; // Could be public if you want a timer to check.

  //       @Override
  //       public boolean shouldOverrideUrlLoading(WebView webview, String urlNewString) {
  //           running++;
  //           webview.loadUrl(urlNewString);
  //           return true;
  //       }

  //       @Override
  //       public void onPageStarted(WebView webview, String url, Bitmap favicon) {
  //           running = Math.max(running, 1); // First request move it to 1.
  //       }

  //       @Override
  //       public void onPageFinished(WebView webview, String url) {
  //           if(--running == 0) { // just "running--;" if you add a timer.
                
  //               // webview.loadUrl("javascript: document.body.innerHTML += 'We have reached onPageFinished!'; draw();");
  //               // wvInterface.onLoad();
  //               webview.loadUrl(jsScript);
  //               // TODO: finished... if you want to fire a method.
  //           }
  //       }
  //     });
  //   }

  /*
   *  Draw using Google Chart
   */
  @SimpleFunction(description = "Add Google Chart based on Google spreadsheet url. If you don't know what query is, just leave it blank.")
  public void GoogleSpreadsheet(String url, String query, String chartType) {
    wvInterface.setInputTypeString("GOOGLE");
    wvInterface.setArg1String(chartType);
    wvInterface.setArg2String(url);
    wvInterface.setArg3String(query);
    GoToUrl("http://mit-dig.github.io/punya-webservices/");
    // String inputs = "'" + chartType + "','" + url + "','" + query + "'"; //document.body.innerHTML += '" + inputs + "'; 
    // String jsScript = "javascript: document.body.innerHTML += '" + inputs + "'; window.AppInventor.runMethod(GoogleSpreadsheet(" + inputs + ");"; //javascript:window.AppInventor.runMethod(GoogleSpreadsheet('AreaChart','https://docs.google.com/spreadsheets/d/1GkVebTN6fyu5-l2dUmGiZR9-WvXg1RSvgzlxnl73bFI/edit#gid=0', 'SELECT O,C,D,E,G where N =\"VI-2\"','{\"title\":\"Course 6 Enrollment\",\"vAxis\": {\"format\": \"########\", \"title\" : \"# of Students\"}, \"hAxis\": {\"format\": \"\", \"title\": \"Year\"}, \"width\" : 1000, \"height\":500}');
    // wvInterface.setJavascriptString(jsScript);
    // webview.loadUrl(jsScript);
    // setupWebViewClient();
  }

  /*
   *  Draw SparQL graph, using Sgvizler.
   */
  @SimpleFunction(description = "Add SparQL graph.")
  public void SPARQLquery(String endpoint, String query, String chartType) {
    wvInterface.setInputTypeString("SPARQL");
    wvInterface.setArg1String(endpoint);
    wvInterface.setArg2String(query);
    wvInterface.setArg3String(chartType);
    GoToUrl("http://mit-dig.github.io/punya-webservices/");
    // String inputs = "'" + endpoint + "','" + query + "','" + chartType + "'";
    // String jsScript = "javascript: window.AppInventor.runMethod(SPARQLquery(" + inputs + ");";
    // wvInterface.setJavascriptString(jsScript);
    // webview.loadUrl(jsScript); //window.AppInventor.runMethod(SPARQLquery(" + inputs + "));
    // setupWebViewClient();
  }

  /*
   *  Draw CSVfile graph, using Google Charts.
   */
  @SimpleFunction(description = "Add CSV graph.")
  public void CSVstring(String csvString, String chartType) {
    wvInterface.setInputTypeString("CSV");
    wvInterface.setArg1String(csvString);
    wvInterface.setArg2String(chartType);
    GoToUrl("http://mit-dig.github.io/punya-webservices/");
    // String inputs = "'" + csvString + "','" + chartType + "'";
    // String jsScript = "javascript: document.body.innerHTML += '" + inputs + "'; window.AppInventor.runMethod(CSVstring(" + inputs + "));";
    // wvInterface.setJavascriptString(jsScript);
    // webview.loadUrl(jsScript);
    // setupWebViewClient();
  }

  /**
   * Allows the setting of properties to be monitored from the javascript
   * in the WebView
   */
  private class WebViewInterface {
    Context mContext;
    String returnString;
    String webViewString;
    String inputType;
    String arg1;
    String arg2;
    String arg3;

    /** Instantiate the interface and set the context */
    WebViewInterface(Context c) {
      mContext = c;
      returnString = " ";
      webViewString = " ";
      inputType = " ";
      arg1 = " ";
      arg2 = " ";
      arg3  = " ";
    }

    /**
     * Set returnString to value returned by JavaScript method. (LEGACY CODE FROM WEBVIEWER)
     */
    @JavascriptInterface
    public void runMethod(String value) {
      returnString = value;
    }

    /**
     * Set the WebViewString.
     * @return string
     */
    @JavascriptInterface
    public void setWebViewString(String str) {
      webViewString = str;
    }

    /**
     * Get the WebViewString.
     * @return string
     */
    @JavascriptInterface
    public String getWebViewString() {
      return webViewString;
    }

    /**
     * Set the input type.
     * @return string
     */
    @JavascriptInterface
    public void setInputTypeString(String type) {
      inputType = type;
    }

    /**
     * Get the input type.
     * @return string
     */
    @JavascriptInterface
    public String getInputTypeString() {
      return inputType;
    }

    /**
     * Sets the first argument's string
     */
    public void setArg1String(String arg) {
      arg1 = arg;
    }

    /**
     * Gets the first argument's string
     */
    public String getArg1String() {
      return arg1;
    }

    /**
     * Sets the second argument's string
     */
    public void setArg2String(String arg) {
      arg2 = arg;
    }

    /**
     * Gets the second argument's string
     */
    public String getArg2String() {
      return arg2;
    }

    /**
     * Sets the third argument's string
     */
    public void setArg3String(String arg) {
      arg3 = arg;
    }

    /**
     * Gets the third argument's string
     */
    public String getArg3String() {
      return arg3;
    }

  }
}
