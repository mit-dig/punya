package com.google.appinventor.components.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesTemplates;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.Survey.SaveSurvey;


import com.google.gson.JsonParser;



/**
 * Component for Linked Data Visualization 
 * This component makes use of Android WebView and javascript library to visualize Linked 
 * Data. It wraps around the Sgvizler js library (http://code.google.com/p/sgvizler/)
 * and provides interface to interact with App Inventor components
 *
 * @author fuming@mit.mit (Fuming Shih)
 */

@DesignerComponent(version = YaVersion.LINKED_DATA_COMPONENT_VERSION,
    category = ComponentCategory.MAPVIZ,
    description = "Component for Visualizing Linked Data. This component makes use " +
        "of Android WebView for visualization and allows users to input sparql queries")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, " +
"android.permission.INTERNET")
@UsesTemplates(templateNames = "sgvizler.js, " +
		"sgvizler.chart.css," +
		"jquery.min.js," +
		"dataviz.html")
public final class LinkedDataViz extends AndroidViewComponent{
  
  private final WebView webview;

  
  private static final String TAG = "LinkedDataViz";
  private String htmlContent;
  

  private String endpoint = "";
  private String sparqlQuery = "";
  private String chartStyle = "";
  // private String logLevel;
  private String chartOptions = "";
  private final HashMap<String, String> namespacePrefix;
  private static String VIZ_TEMPLATE_FILE = "dataviz.html";
 
 
  /**
   * Creates a new LinkDataViz component.
   * 
   * @param container
   *            container the component will be placed in
   * @throws IOException 
   */
  public LinkedDataViz(ComponentContainer container) throws IOException {
    super(container);
// Hal's stuff    
//    webview = new WebView(container.$context());
//    webview.setWebViewClient(new WebViewerClient());
//    webview.getSettings().setJavaScriptEnabled(true);
//    webview.setFocusable(true);
//    // enable pinch zooming and zoom controls
//    webview.getSettings().setBuiltInZoomControls(true);
    

    webview = new WebView(container.$context());
    webview.setWebViewClient(new MyWebViewClient());
    webview.getSettings().setJavaScriptEnabled(true);
    webview.setFocusable(true);
    webview.getSettings().setBuiltInZoomControls(true);

    container.$add(this);

    namespacePrefix = new HashMap<String, String>();
    
    webview.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
          case MotionEvent.ACTION_UP:
            if (!v.hasFocus()) {
              v.requestFocus();
            }
            break;
        }
        return false;
      }
    });
    Width(LENGTH_FILL_PARENT);
    Height(LENGTH_FILL_PARENT);

  }
  
  private class MyWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      return false;
    }
  }


  
  @Override
  public View getView() {
    // TODO Auto-generated method stub
    return webview;
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

  
  
  /*
   * TODO: need to test different kinds of styles, and see if they can be shown correctly on android.
   */
  @SimpleFunction(description = "Set attributes for the visualization, including the endpoint," +
  		"type of visualization, and addition chart options. Pleae refers to " +
  		"https://code.google.com/p/sgvizler/wiki/DesigningQueries for all possible chart styles" +
  		"As for chartOptions Each option is given as option=value and each option-value-pair is " +
  		"separated with a vertical bar, e.g. option1=value1|option2=value2. Please consult the " +
  		"documentation on Google Chart page the available options and permissible values of a specific" +
  		"chart type.")
  public void SetAttribute(String endPoint, String style, String chartOptions){
    this.endpoint = endPoint;
    this.chartStyle = style;
    this.chartOptions = chartOptions;
    // we will later set div width and length the window size 100%
    
  }
  
  @SimpleFunction(description = "Set SPQARQL query. Be aware that the order of " +
  		"the variables in the \"Select\" clause in sparql query. The order " +
  		"decides how the information in the diagram is grouped. " +
  		"For more information: https://code.google.com/p/sgvizler/wiki/DesigningQueries ")
  public void SetSparqlQuery(String sparqlQuery) {
    this.sparqlQuery = sparqlQuery;
    
  }
  

  
  @SimpleFunction(description = "Add SPARQL namespace prefix. Add convenient prefixes for your dataset." +
  		" rdf, rdfs, xsd, owlare already set.")
  public void AddNamespacePrefix(String prefix, String namespace){
    
    this.namespacePrefix.put(prefix, namespace);
    
    
  }
  

  /*
   * This will load the visualization template in the WebView. 
   * To show different things on the chart/map, one need to reset the SPARQL query and 
   * call Visualize() again
   * 
   */
  @SimpleFunction(description = "Please to set visualization style, set SPARQL query before" +  
       "calling Vizualize")
  public void Vizualize() throws IOException{
    
    this.htmlContent = prepareHtml();

    //see http://pivotallabs.com/users/tyler/blog/articles/1853-android-webview-loaddata-vs-loaddatawithbaseurl
    //http://myexperiencewithandroid.blogspot.com/2011/09/android-loaddatawithbaseurl.html
    this.webview.loadDataWithBaseURL("file:///android_asset/", this.htmlContent, "text/html", "UTF-8", null);
 
    Log.i(TAG,  "After visualize data" );
 
  }
  
  @SimpleFunction(description = "Enable user to zoom in or zoom out the who visualization")
  public void ZoomEnable(boolean enabled) {
    this.webview.getSettings().setBuiltInZoomControls(enabled);
    
  }
  

  private String prepareHtml() throws IOException {
    //should not have IOException here, because we make sure we have included the templates
    Log.i(TAG, "start reading template file");
    BufferedInputStream in = new BufferedInputStream(container.$context().getAssets().open(VIZ_TEMPLATE_FILE));
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    
    StringBuilder sb = new StringBuilder();
    String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
    }
    Log.i(TAG, "after reading template file to String builder");
    
    //1. Replace the following text in the template with namespace uri  
    //__namespace__
    // sgvizler.option.namespace.npd = 'http://sws.ifi.uio.no/npd/';
    // sgvizler.option.namespace.npdv = 'http://sws.ifi.uio.no/vocab/npd#';
    Log.i(TAG, "begin replacing name space");
    String namespaceCue = "__namespace__";
    int insertPos = sb.indexOf(namespaceCue) + namespaceCue.length();
    String namespaceTemplate = "sgvizler.option.namespace";
    for (String prefix : namespacePrefix.keySet()){
      
      String namespaceUri = namespacePrefix.get(prefix);
      String add = "\n" + namespaceTemplate + "." + prefix + "='" + namespaceUri + "';"; 
      
      sb.insert(insertPos, add);
      insertPos += add.length(); // go to the next line
    } 
    
    Log.i(TAG, "html: " + sb.toString());
    
    //2. Replace the endpoint, query, chart_style
    Log.i(TAG, "start inserting endpoint");
    String endpointCue = "__endpoint__"; 
    int startPos = sb.indexOf(endpointCue);
    int endPos = sb.indexOf(endpointCue) + endpointCue.length();
    sb.replace(startPos, endPos, this.endpoint);
    Log.i(TAG, "end inserting endpoint");
    
    //replace SPARQl query
    String queryCue = "__query__";
    startPos = sb.indexOf(queryCue);
    endPos = sb.indexOf(queryCue) + queryCue.length();
    sb.replace(startPos, endPos, this.sparqlQuery);
    Log.i(TAG, "end inserting query");
    
    //replace chart style
    String chartStyleCue = "__chart__";
    startPos = sb.indexOf(chartStyleCue);
    endPos = sb.indexOf(chartStyleCue) + chartStyleCue.length();
    sb.replace(startPos, endPos, this.chartStyle);
    Log.i(TAG, "end chart style query");
    
    //replace chart options
    String chartOptionsCue = "__options__";
    startPos = sb.indexOf(chartOptionsCue);
    endPos = sb.indexOf(chartOptionsCue) + chartOptionsCue.length();
    sb.replace(startPos, endPos, this.chartOptions);
    Log.i(TAG, "end inserting options");
    
  // We will prepare the html template with the input value 
  //for visualization (query, endpoint, style.,etc) 
  //here's an example, 
    
//    <div id="sgvzl_example_query" data-sgvizler-endpoint="http://sws.ifi.uio.no/sparql/npd" 
//        data-sgvizler-query="SELECT * WHERE { [] a npdv:NCSProductionPeriod ; npdv:year ?year ; " +
//        		"npdv:producedNetOilMillSm ?Oil_millSm ; npdv:producedNetGasBillSm ?Gas_billSm ; " +
//        		"npdv:producedNetNGLMillSm3 ?NGL_millSm3 ; npdv:producedNetCondensateMillSm3 " +
//        		"?Condensate_millSm3 ; npdv:producedWaterMillSm3 ?Water_millSm3 ; " +
//        		"FILTER (xsd:int(?year) < 2011) } ORDER BY ?year" 
//       data-sgvizler-chart="gColumnChart" 
//       data-sgvizler-loglevel="2" 
//       style="width:500px; height:300px;"></div>
    Log.i(TAG, "<before return html>\n" + sb.toString());
    
    return sb.toString();
    
  }      
  /*
   * Testing purpose
   */
  @SimpleFunction
  public void GoToUrl(String url){
    webview.loadUrl(url);

  }
  
  
       
       
  
  
}