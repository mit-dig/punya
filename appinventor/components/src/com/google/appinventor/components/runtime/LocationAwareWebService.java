package com.google.appinventor.components.runtime;

import android.util.Log;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.gson.JsonElement;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

/**
 * This class is meant to act as a superclass to concrete location-aware, web service components.
 *
 * @author william.van.woensel@gmail.com
 */
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_FINE_LOCATION")
public abstract class LocationAwareWebService extends WebService implements Probe.DataListener {

    private final String TAG = "PlacesWebService";

    protected LocationProbeSensor locationProbeSensor;

    protected int minimumLocationChange = 0; // 0 meters

    /**
     * Creates a new LocationAwareWebService component.
     *
     * @param container the container that this component will be placed in
     */
    protected LocationAwareWebService(ComponentContainer container) {
        super(container);

        this.locationProbeSensor = new LocationProbeSensor(container);
        // override the data listener with this component
        // when run-once or scheduled location fixes are received,
        // the methods of this listener will be called
        locationProbeSensor.overrideListener(this);
    }

    /**
     * Returns the default interval (in seconds) between actions, where an action includes a location probe plus service call.
     *
     * @return defaultInterval
     */
    @SimpleProperty(description = "The default interval (in seconds) between actions, where an action includes a location probe plus service call",
            category = PropertyCategory.BEHAVIOR)
    public int DefaultInterval(){return locationProbeSensor.DefaultInterval();}

    /**
     * Specifies the default interval (in seconds) between actions, where an action includes a location probe plus service call.
     *
     * @param defaultInterval
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "180")
    @SimpleProperty
    public void DefaultInterval(int defaultInterval) { locationProbeSensor.DefaultInterval(defaultInterval); }

    /**
     * Returns the default duration (in seconds) of each location probe scan
     *
     * @return defaultDuration
     */
    @SimpleProperty(description = "The default duration (in seconds) of each location probe scan",
            category = PropertyCategory.BEHAVIOR)
    public int DefaultDuration(){ return locationProbeSensor.DefaultDuration();}

    /**
     * Specifies the default duration (in seconds) of each location probe scan.
     *
     * @param defaultDuration
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "10")
    @SimpleProperty
    public void DefaultDuration(int defaultDuration) { locationProbeSensor.DefaultDuration(defaultDuration); }

    /**
     *  Returns the good-enough-accuracy of the location data (0-100).
     *  If the location accuracy lies below this threshold, then the online service will not be called.
     *
     * @return goodEnoughAccuracy
     */
    @SimpleProperty(description = "The good-enough-accuracy of the location data (0-100). " +
            "If the location accuracy lies below this threshold, then the online service will not be called.",
            category = PropertyCategory.BEHAVIOR)
    public int GoodEnoughAccuracy() {
        return locationProbeSensor.GoodEnoughAccuracy();
    }

    /**
     * Sets the good-enough-accuracy of the location data (0-100).
     *
     * @param goodEnoughAccuracy
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "80")
    @SimpleProperty
    public void GoodEnoughAccuracy(int goodEnoughAccuracy) { locationProbeSensor.GoodEnoughAccuracy(goodEnoughAccuracy); }

    /**
     *  Returns whether the location probe will use GPS or not.
     */
    @SimpleProperty(description = "Whether the location probe will use GPS or not", category = PropertyCategory.BEHAVIOR)
    public boolean UseGPS() {
        return locationProbeSensor.UseGPS();
    }

    /**
     * Specifies whether the location probe will use GPS or not.
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void UseGPS(boolean useGPS) { locationProbeSensor.UseGPS(useGPS); }

    /**
     *  Returns whether the location probe will use the network or not.
     */
    @SimpleProperty(description = "whether the location probe will use the network or not",
            category = PropertyCategory.BEHAVIOR)
    public boolean UseNetwork() { return locationProbeSensor.UseNetwork(); }

    /**
     * Specifies whether the location probe will use the network or not.
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    public void UseNetwork(boolean useNetwork) { locationProbeSensor.UseNetwork(useNetwork); }

    /**
     * Returns the minimal difference in location (in meters) compared to the prior location, before the service is called.
     * This avoids calling the online service for location-specific data when the user's location has not really changed much.
     *
     * @return minimumLocationChange
     */
    @SimpleProperty(description = "The minimal difference in location (in meters) compared to the prior location, before the service is called. " +
            "This avoids calling the online service for location-specific data when the user's location has not really changed much.",
            category = PropertyCategory.BEHAVIOR)
    public int MinimumLocationChange() { return minimumLocationChange; }

    /**
     * Specifies the minimal difference in location (in meters) compared to the prior location, before the service is called.
     *
     * @param minimumLocationChange
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    public void MinimumLocationChange(int minimumLocationChange) { this.minimumLocationChange = minimumLocationChange; }
}