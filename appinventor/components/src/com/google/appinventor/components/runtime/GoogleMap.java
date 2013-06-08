package com.google.appinventor.components.runtime;


import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AlignmentUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.OnInitializeListener;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/** Component for displaying information on Google Map
 * This component makes use of Android MapView (v2) to location specific information.
 * App Inventor user could use this component to do things like those demo apps
 * for Google Mapview in the android sdk
 *
 * @author fuming@mit.mit (Fuming Shih)
 */
@DesignerComponent(version = YaVersion.GOOGLE_MAP_COMPONENT_VERSION,
    description = "Visible component that show information on Google map.",
    category = ComponentCategory.MISC)
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, "
    + "android.permission.ACCESS_NETWORK_STATE, "
    + "android.permission.INTERNET, "
    + "android.permission.ACCESS_COARSE_LOCATION, "
    + "android.permission.ACCESS_FINE_LOCATION, "
    + "com.google.android.providers.gsf.permission.READ_GSERVICES, "
    + "android.permission.WRITE_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "google-play-services.jar")
public class GoogleMap extends AndroidViewComponent implements OnResumeListener, OnInitializeListener {

  private final Activity context;
  private final Form form;
  private static final String TAG = "GoogleMap";

  // Layout
  // We create thie LinerLayout and add our mapFragment in it.
  private final com.google.appinventor.components.runtime.LinearLayout viewLayout;


  // translates App Inventor alignment codes to Android gravity
  private final AlignmentUtil alignmentSetter;

  // the alignment for this component's LinearLayout
  private int verticalAlignment;

  private static final String MAP_FRAGMENT_TAG = "map";
  private com.google.android.gms.maps.GoogleMap mMap;
  private SupportMapFragment mMapFragment;

  private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

  public GoogleMap(ComponentContainer container) throws IOException {
      super(container);
      context = container.$context();
      form = container.$form();
      viewLayout = new com.google.appinventor.components.runtime.LinearLayout(context,
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL);
      alignmentSetter = new AlignmentUtil(viewLayout);

      verticalAlignment = ComponentConstants.VERTICAL_ALIGNMENT_DEFAULT;

      alignmentSetter.setVerticalAlignment(verticalAlignment);
      ViewGroup viewG = viewLayout.getLayoutManager();

      viewG.setId(generateViewId());


      container.$add(this);           // add first (will be WRAP_CONTENT)
      Log.i(TAG, "here before reset width and length");
      container.setChildWidth(this, LENGTH_FILL_PARENT); //change to FILL_PARENT
      container.setChildHeight(this, LENGTH_FILL_PARENT);

    //add check if the phone has installed Google Map and Google Play Service sdk

    checkGooglePlayServiceSDK() ;
    checkGoogleMapInstalled() ;

    mMapFragment = (SupportMapFragment) form.getSupportFragmentManager()
        .findFragmentByTag(MAP_FRAGMENT_TAG);


    // We only create a fragment if it doesn't already exist.
    if (mMapFragment == null) {
//      // To programmatically add the map, we first create a SupportMapFragment.
      mMapFragment = SupportMapFragment.newInstance();

      //mMapFragment = new MySupportMapFragment();
      FragmentTransaction fragmentTransaction =
          form.getSupportFragmentManager().beginTransaction();
      Log.i(TAG, "here before adding fragment");
//      fragmentTransaction.add(viewG.getId(), mMapFragment, MAP_FRAGMENT_TAG);

      fragmentTransaction.add(android.R.id.content, mMapFragment, MAP_FRAGMENT_TAG);
      fragmentTransaction.commit();


    }

    // We can't be guaranteed that the map is available because Google Play services might
    // not be available.
    setUpMapIfNeeded();
    form.registerForOnInitialize(this);
    form.registerForOnResume(this);

  }

//  /*
//   * Currently this is not working, we will come back to this later....
//   */
//
//      public class MySupportMapFragment extends SupportMapFragment {
//        public MySupportMapFragment() {
//          return;
//        }
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//          Log.v(TAG, "In overridden onCreateView.");
//          View v = super.onCreateView(inflater, container, savedInstanceState);
//
//      Log.v(TAG, "Initialising map.");
//
//      initMap();
//
//
//      return v;
//    }
//
//    @Override
//    public void onViewCreated (View view, Bundle savedInstanceState) {
//      super.onViewCreated(view, savedInstanceState);
//      Log.v(TAG, "chage to fill_parent.");
//
//      changeWidthHeight();
//      view.requestLayout();
////      vg.requestLayout();
//      Log.v(TAG, "Moving the MyPositionButton");
////      resetMyPositionButton(view);
//    }
//
//    private void initMap(){
//      UiSettings settings = getMap().getUiSettings();
//      settings.setAllGesturesEnabled(true);
//      settings.setMyLocationButtonEnabled(true);
//      LatLng latLong = new LatLng(22.320542, 114.185715);
//      getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,11));
//
//    }
//
//  }

  /**
   * Generate a value suitable for use in .
   * This value will not collide with ID values generated at build time by aapt for R.id.
   *
   * @return a generated ID value
   */
  private static int generateViewId() {
      for (;;) {
          final int result = sNextGeneratedId.get();
          // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
          int newValue = result + 1;
          if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
          if (sNextGeneratedId.compareAndSet(result, newValue)) {
              return result;
          }
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

  private void setUpMapIfNeeded() {
      // Do a null check to confirm that we have not already instantiated the map.
      if (mMap == null) {
        // Try to obtain the map from the SupportMapFragment.
        mMap = mMapFragment.getMap();
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
          Log.i(TAG, "do we have google map?");
          setUpMap();
        } else {
          // means that Google Service is not available
          form.dispatchErrorOccurredEvent(this, "setUpMapIfNeeded",
              ErrorMessages.ERROR_GOOGLE_PLAY_NOTINSTALLED);
        }

      }
  }
  
  
  private void setUpMap() {
    // could be the boilerplate for initiating everything
    // including all the configurations and markers 
    
    // (testing: add an marker)
    
    
    
    
    
    mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

  }

  private void checkGooglePlayServiceSDK() {
    //To change body of created methods use File | Settings | File Templates.
    final int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
    if(googlePlayServicesAvailable != ConnectionResult.SUCCESS){
      form.dispatchErrorOccurredEvent(this, "checkGooglePlayServiceSDK",
          ErrorMessages.ERROR_GOOGLE_PLAY_NOTINSTALLED);
    }
  }

  private void checkGoogleMapInstalled() {
    try
    {
      ApplicationInfo info = context.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0 );

    }
    catch(PackageManager.NameNotFoundException e)
    {
      form.dispatchErrorOccurredEvent(this, "checkGoogleMapInstalled",
          ErrorMessages.ERROR_GOOGLE_MAP_NOTINSTALLED);
    }
  }

  // AndroidViewComponent implementation

  @Override
  public View getView() {
    return viewLayout.getLayoutManager();
  }

  @Override
  public void onResume() {
    // TODO: http://stackoverflow.com/questions/15001207/android-googlemap-is-null-displays-fine-but-cant-add-markers-polylines
    // only now is it saved to redraw the map...
    Log.i(TAG, "in onResume...Google Map redraw");
    setUpMapIfNeeded();

  }

  @Override
  public void onInitialize() {
    // TODO Auto-generated method stub
    Log.i(TAG, "try to do after the component is initialized...");
    setUpMapIfNeeded();
    // fire an event so that AI user could add markers on initialized 

  }




}
