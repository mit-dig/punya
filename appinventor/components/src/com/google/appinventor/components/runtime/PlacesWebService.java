package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.YailList;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@SimpleObject
public abstract class PlacesWebService extends LocationAwareWebService {

    protected boolean enabledNearbyPlaces = false; // run once
    protected boolean enabledScheduleNearbyPlaces = false; // run periodically

    protected int nearbyRadius = 100; // 100 meters
    protected String placeType = null;

    /**
     * Creates a new PlacesWebService component.
     *
     * @param container the container that this component will be placed in
     */
    protected PlacesWebService(ComponentContainer container) {
        super(container);
    }

    @SimpleProperty(description = "Whether the component was run once to get location data and call the web service for nearby places",
            category = PropertyCategory.BEHAVIOR)
    public boolean EnabledNearbyPlaces() { return enabledNearbyPlaces; }

    @SimpleProperty(description = "Whether the component is enabled to periodically listen for location data, " +
            "and call the web service for nearby places",
            category = PropertyCategory.BEHAVIOR)
    public boolean EnabledScheduleNearbyPlaces() { return enabledScheduleNearbyPlaces; }

    @SimpleProperty(description = "The default interval (in seconds) between actions, where an action includes a location probe plus service call",
            category = PropertyCategory.BEHAVIOR)
    public int ScheduleNearbyPlacesInterval() { return locationProbeSensor.DefaultInterval(); }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "180")
    @SimpleProperty
    public void ScheduleNearbyPlacesInterval(int interval) { locationProbeSensor.DefaultInterval(interval); }

    @SimpleFunction(description = "Enable the component to run once to get location data, call the web service for nearby places, " +
            "and raise the corresponding events")
    public void EnableNearbyPlaces(boolean enableNearbyPlaces) {
        if (checkInput()) {
            this.enabledNearbyPlaces = enableNearbyPlaces;
            locationProbeSensor.Enabled(enableNearbyPlaces);
        }
    }

    @SimpleFunction(description = "Enable the component to periodically listen for location data, " +
            "call the web service for nearby places, and raise the corresponding events")
    public void EnableScheduleNearbyPlaces(boolean enableScheduleNearbyPlaces) {
        if (checkInput()) {
            this.enabledScheduleNearbyPlaces = enableScheduleNearbyPlaces;
            locationProbeSensor.EnabledSchedule(enableScheduleNearbyPlaces);
        }
    }

    /**
     * Returns the radius around the user’s current location (meters) for which nearby places should be returned.
     *
     * @return nearbyRadius
     */
    @SimpleProperty(description = "The radius around the user’s current location (meters) for which nearby places should be returned",
            category = PropertyCategory.BEHAVIOR)
    public int NearbyRadius() { return nearbyRadius; }

    /**
     * Specifies the radius around the user’s current location (meters) for which nearby places should be returned.
     *
     * @param nearbyRadius
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "100")
    public void NearbyRadius(int nearbyRadius) { this.nearbyRadius = nearbyRadius; }

    @SimpleEvent(description = "Event indicating that nearby places have been received")
    public void NearbyPlacesReceived(final YailList nearbyPlaces) {
        final Component component = this;
        if (enabledNearbyPlaces || enabledScheduleNearbyPlaces) {
            // TODO unclear whether this is needed (done in LocationProbeSensor, not Web)
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    EventDispatcher.dispatchEvent(component, "NearbyPlacesReceived", nearbyPlaces);
                }
            });
        }
    }

    // We want service-specific documentation so have subclasses implement these
    // methods. Also, subclasses may want to pre-process these types.

    public abstract String PlaceType();

    public abstract void PlaceType(String placeType);
}
