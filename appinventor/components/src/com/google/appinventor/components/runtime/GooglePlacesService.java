package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

import java.time.LocalTime;
import java.util.Arrays;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;

@DesignerComponent(version = YaVersion.GPLACES_COMPONENT_VERSION,
        description = "<p>A component that accesses the Google Places API given the user's current location, " +
                "and returns a set of nearby places (see also OverpassPlacesService). " +
                "It relies on the LocationProbeSensor and thus offers similar options, " +
                "i.e., periodically get the user's location at a configurable time-interval, scanning period, " +
                "and good-enough-accuracy. Subsequently, the component will get places nearby the user's location. " +
                "(After the scanning period, the component will return the found location with the highest accuracy; " +
                "ending early when finding a location with good-enough-accuracy.)</p>" +
                "Additionally, one can specify a minimum-location-change property that will call the Places API" +
                " only in case the user's location has changed significantly.</p>" +
                "<p>One can specify the radius (in meters) around the user for which to return places.</p>",
        category = ComponentCategory.CONNECTIVITY, nonVisible = true, iconName = "images/locationProbe.png")
@UsesLibraries(libraries = "google-maps-services-0.18.1.jar")
public class GooglePlacesService extends PlacesWebService {

    /**
     * Creates a new GooglePlacesService component.
     *
     * @param container the container that this component will be placed in
     */
    protected GooglePlacesService(ComponentContainer container) {
        super(container);

        apiKeyRequired = true;
    }

    /**
     * This method is called when receiving new location data, as per the configuration of the location probe sensor.
     * A subclass will do something with this location (e.g., call a service for nearby places) and likely raise an event.
     *
     * @param locationData
     */
    @Override
    protected void locationDataReceived(Location locationData) {
        GeoApiContext context = new GeoApiContext.Builder().apiKey(apiKey).build();
        LatLng myLocation = new LatLng(locationData.lat, locationData.lon);

        try {
            PlacesSearchResponse results = PlacesApi.nearbySearchQuery(context, myLocation).radius(nearbyRadius).await();
            for (PlacesSearchResult result : results.results) {
                
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
