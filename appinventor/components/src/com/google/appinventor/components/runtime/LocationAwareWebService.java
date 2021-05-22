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
    @SimpleProperty(description = "The default interval (in seconds) between actions, where an action includes a location probe plus service call")
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
    @SimpleProperty(description = "The default duration (in seconds) of each location probe scan")
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
            "If the location accuracy lies below this threshold, then the online service will not be called.")
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
    @SimpleProperty(description = "Whether the location probe will use GPS or not")
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
    @SimpleProperty(description = "whether the location probe will use the network or not")
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
            "This avoids calling the online service for location-specific data when the user's location has not really changed much.")
    public int MinimumLocationChange() { return minimumLocationChange; }

    /**
     * Specifies the minimal difference in location (in meters) compared to the prior location, before the service is called.
     *
     * @param minimumLocationChange
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    public void MinimumLocationChange(int minimumLocationChange) { this.minimumLocationChange = minimumLocationChange; }

    @Override
    public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
        Log.i(TAG, "receive data");

        Location location = new Location(data.get(ProbeKeys.LocationKeys.LATITUDE).getAsDouble(),
                data.get(ProbeKeys.LocationKeys.LONGITUDE).getAsDouble(),
                data.get(ProbeKeys.LocationKeys.ACCURACY).getAsFloat(),
                data.get("mProvider").getAsString(),
                data.get(ProbeKeys.LocationKeys.TIMESTAMP).getAsLong());

        Log.i(TAG, "location:" + location);

        locationDataReceived(location);
    }

    @Override
    public void onDataCompleted(IJsonObject iJsonObject, JsonElement jsonElement) {
    }

    /**
     * This method is called when receiving new location data, as per the configuration of the location probe sensor.
     * A subclass will do something with this location (e.g., call a service for nearby places) and likely raise an event.
     *
     * @param locationData
     */
    protected abstract void locationDataReceived(Location locationData);

    protected class Location {

        double lat;
        double lon;
        float accuracy;
        String provider;
        long timestamp;

        public Location(double lat, double lon, float accuracy, String provider, long timestamp) {
            this.lat = lat;
            this.lon = lon;
            this.accuracy = accuracy;
            this.provider = provider;
            this.timestamp = timestamp;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public float getAccuracy() {
            return accuracy;
        }

        public String getProvider() {
            return provider;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "LocationData{" +
                    "lat=" + lat +
                    ", lon=" + lon +
                    ", accuracy=" + accuracy +
                    ", provider='" + provider + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}