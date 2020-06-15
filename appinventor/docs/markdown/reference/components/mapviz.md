---
layout: documentation
title: Map & Visualization
---

[&laquo; Back to index](index.html)
# Map & Visualization

Table of Contents:

* [GoogleMap](#GoogleMap)
* [Graph](#Graph)

## GoogleMap  {#GoogleMap}

Component for GoogleMap



### Properties  {#GoogleMap-Properties}

{:.properties}

{:id="GoogleMap.CompassEnabled" .boolean .ro .bo} *CompassEnabled*
: Indicates whether the compass widget is currently enabled in the map ui

{:id="GoogleMap.Height" .number .bo} *Height*
: Property for Height

{:id="GoogleMap.HeightPercent" .number .wo .bo} *HeightPercent*
: Specifies the `GoogleMap`'s vertical height as a percentage
 of the [`Screen`'s `Height`](userinterface.html#Screen.Height).

{:id="GoogleMap.MapCameraChangedListenerEnabled" .boolean .ro .bo} *MapCameraChangedListenerEnabled*
: Indicates if the map camera's position changed listener is currently enabled

{:id="GoogleMap.MapClickListenerEnabled" .boolean .ro .bo} *MapClickListenerEnabled*
: Indicates if the mapClick listener is currently enabled

{:id="GoogleMap.MapLongClickListenerEnabled" .boolean .ro .bo} *MapLongClickListenerEnabled*
: Indicates if the map's longClick event listener is currently enabled

{:id="GoogleMap.MapType" .text .ro .bo} *MapType*
: Indicates the current map type

{:id="GoogleMap.MyLocationEnabled" .boolean .ro .bo} *MyLocationEnabled*
: Indicates whether my locaiton UI control is currently enabled for the Google map.

{:id="GoogleMap.RotateEnabled" .boolean .ro .bo} *RotateEnabled*
: Indicates whether the capability to rotate a map on the ui is currently enabled

{:id="GoogleMap.ScrollEnabled" .boolean .ro .bo} *ScrollEnabled*
: Indicates whether the capability to scroll a map on the ui is currently enabled

{:id="GoogleMap.Visible" .boolean} *Visible*
: Specifies whether the `GoogleMap` should be visible on the screen.  Value is `true`{:.logic.block}
 if the `GoogleMap` is showing and `false`{:.logic.block} if hidden.

{:id="GoogleMap.Width" .number .bo} *Width*
: Property for Width

{:id="GoogleMap.WidthPercent" .number .wo .bo} *WidthPercent*
: Specifies the horizontal width of the `GoogleMap` as a percentage
 of the [`Screen`'s `Width`](userinterface.html#Screen.Width).

{:id="GoogleMap.ZoomControlEnabled" .boolean .ro .bo} *ZoomControlEnabled*
: Indicates whether the zoom widget on the map ui is currently enabled

{:id="GoogleMap.ZoomGestureEnabled" .boolean .ro .bo} *ZoomGestureEnabled*
: Indicates whether the zoom gesture is currently enabled

### Events  {#GoogleMap-Events}

{:.events}

{:id="GoogleMap.CameraPositionChanged"} CameraPositionChanged(*lat*{:.number},*lng*{:.number},*bearing*{:.number},*tilt*{:.number},*zoom*{:.number})
: Called after the camera position has changed, returning all camera position parameters.

{:id="GoogleMap.FinishedDraggingCircle"} FinishedDraggingCircle(*id*{:.number},*centerLat*{:.number},*centerLng*{:.number},*radius*{:.number})
: Event been raised after the action of moving a draggable circle is finished. Possible a user drag the center of the circle or drag the radius marker of the circle

{:id="GoogleMap.InfoWindowClicked"} InfoWindowClicked(*markerId*{:.number})
: When the marker's infowindow is clicked, returning marker's id

{:id="GoogleMap.MapIsReady"} MapIsReady()
: Indicates that the map has been rendered and ready for adding markers or changing other settings. Please add or updating markers within this event

{:id="GoogleMap.OnLocationChanged"} OnLocationChanged(*lat*{:.number},*lng*{:.number})
: Triggers this event when user location has changed. Only works when EnableMylocation is set to true

{:id="GoogleMap.OnMapClick"} OnMapClick(*lat*{:.number},*lng*{:.number})
: Called when the user makes a tap gesture on the map

{:id="GoogleMap.OnMapLongClick"} OnMapLongClick(*lat*{:.number},*lng*{:.number})
: Called when the user makes a long-press gesture on the map

{:id="GoogleMap.OnMarkerClick"} OnMarkerClick(*markerId*{:.number},*latitude*{:.number},*longitude*{:.number})
: When a marker is clicked

{:id="GoogleMap.OnMarkerDrag"} OnMarkerDrag(*markerId*{:.number},*latitude*{:.number},*longitude*{:.number})
: When a marker is been dragged

{:id="GoogleMap.OnMarkerDragEnd"} OnMarkerDragEnd(*markerId*{:.number},*latitude*{:.number},*longitude*{:.number})
: When the user drags a marker and finish the action, returning marker's id and it's latest position

{:id="GoogleMap.OnMarkerDragStart"} OnMarkerDragStart(*markerId*{:.number},*latitude*{:.number},*longitude*{:.number})
: When a marker starts been dragged

### Methods  {#GoogleMap-Methods}

{:.methods}

{:id="GoogleMap.AddCircle" class="method returns number"} <i/> AddCircle(*lat*{:.number},*lng*{:.number},*radius*{:.number},*alpha*{:.number},*hue*{:.number},*strokeWidth*{:.number},*strokeColor*{:.number},*draggable*{:.boolean})
: Create a circle overlay on the map UI with specified latitude and longitude for center. "hue" (min 0, max 360) and "alpha" (min 0, max 255) are used to set color and transparency level of the circle, "strokeWidth" and "strokeColor" are for the perimeter of the circle. Returning a unique id of the circle for future reference to events raised by moving this circle. If the circle isset to be draggable, two default markers will appear on the map: one in the center of the circle, another on the perimeter.

{:id="GoogleMap.AddMarkers" class="method returns list"} <i/> AddMarkers(*markers*{:.list})
: Adding a list of YailLists for markers. The representation of a maker in the inner YailList is composed of: lat(double) [required], long(double) [required], Color, title(String), snippet(String), draggable(boolean). Return a list of unqiue ids for the added  markers. Note that the markers ids are not meant to persist after  the app is closed, but for temporary references to the markers within the program only. Return an empty list if any error happen in the input

{:id="GoogleMap.AddMarkersFromJson" class="method"} <i/> AddMarkersFromJson(*jsonString*{:.text})
: Adding a list of markers that are represented as JsonArray.  The inner JsonObject represents a markerand is composed of name-value pairs. Name fields for a marker are: "lat" (type double) [required], "lng"(type double) [required], "color"(type int)[in hue value ranging from 0~360], "title"(type String), "snippet"(type String), "draggable"(type boolean)

{:id="GoogleMap.AddMarkersHue" class="method returns list"} <i/> AddMarkersHue(*markers*{:.list})
: Add a list of YailList to the map

{:id="GoogleMap.BoundCamera" class="method"} <i/> BoundCamera(*neLat*{:.number},*neLng*{:.number},*swLat*{:.number},*swLng*{:.number})
: Transforms the camera such that the specified latitude/longitude bounds are centered on screen at the greatest possible zoom level. Need to specify both latitudes and longitudes for both northeast location and southwest location of the bounding box

{:id="GoogleMap.EnableCompass" class="method"} <i/> EnableCompass(*enable*{:.boolean})
: Enables/disables the compass widget on the map's ui. Call this only after event "MapIsReady" is received

{:id="GoogleMap.EnableMapCameraPosChangeListener" class="method"} <i/> EnableMapCameraPosChangeListener(*enabled*{:.boolean})
: Enable/Disable map's camera position changed event

{:id="GoogleMap.EnableMapClickListener" class="method"} <i/> EnableMapClickListener(*enabled*{:.boolean})
: Enable map click event listener for this component

{:id="GoogleMap.EnableMapLongClickListener" class="method"} <i/> EnableMapLongClickListener(*enabled*{:.boolean})
: Enable map long click event listener

{:id="GoogleMap.EnableMyLocation" class="method"} <i/> EnableMyLocation(*enabled*{:.boolean})
: Enable or disable my location widget control for Google Map. One can call GetMyLocation() to obtain the current location after enable this."

{:id="GoogleMap.EnableRotate" class="method"} <i/> EnableRotate(*enable*{:.boolean})
: Enables/disables the capability to rotate a map on the ui. Call this only after the event "MapIsReady" is received.

{:id="GoogleMap.EnableScroll" class="method"} <i/> EnableScroll(*enable*{:.boolean})
: Enables/disables the capability to scroll a map on the ui. Call this only after the event "MapIsReady" is received

{:id="GoogleMap.EnableZoomControl" class="method"} <i/> EnableZoomControl(*enable*{:.boolean})
: Enables/disables the zoom widget on the map's ui. Call this only after the event "MapIsReady" is received

{:id="GoogleMap.EnableZoomGesture" class="method"} <i/> EnableZoomGesture(*enable*{:.boolean})
: Enables/disables zoom gesture on the map ui. Call this only after the event  "MapIsReady" is received.

{:id="GoogleMap.GetAllCircleIDs" class="method returns list"} <i/> GetAllCircleIDs()
: Get all circles Ids. A short cut to get all the references for the eixisting circles

{:id="GoogleMap.GetAllMarkerID" class="method returns list"} <i/> GetAllMarkerID()
: Get all the existing markers's Ids

{:id="GoogleMap.GetMarkers" class="method returns list"} <i/> GetMarkers()
: Add a list of markers composed of name-value pairs. Name fields for a marker are: "lat" (type double) [required], "lng"(type double) [required], "color"(type int)[in hue value ranging from 0~360], "title"(type String), "snippet"(type String), "draggable"(type boolean)

{:id="GoogleMap.GetMyLocation" class="method returns list"} <i/> GetMyLocation()
: Get current location using Google Map Service. Return a YailList with first item beingthe latitude, the second item being the longitude, and last time being the accuracy of the reading.

{:id="GoogleMap.MoveCamera" class="method"} <i/> MoveCamera(*lat*{:.number},*lng*{:.number},*zoom*{:.number})
: Move the map's camera to the specified position and zoom level

{:id="GoogleMap.RemoveCircle" class="method returns boolean"} <i/> RemoveCircle(*circleId*{:.number})
: Remove a circle for the map. Returns true if successfully removed, false if the circle does not exist with the specified id

{:id="GoogleMap.RemoveMarker" class="method"} <i/> RemoveMarker(*markerId*{:.number})
: Remove a marker from the map

{:id="GoogleMap.SetMapType" class="method"} <i/> SetMapType(*layerName*{:.text})
: Set the layer of Google map. Default layer is "normal", other choices including "hybrid","satellite", and "terrain"

{:id="GoogleMap.UpdateCircle" class="method"} <i/> UpdateCircle(*circleId*{:.number},*propertyName*{:.text},*value*{:.any})
: Set the property of an existing circle. Properties include: "alpha"(number, value ranging from 0~255), "color" (nimber, hue value ranging 0~360), "radius"(number in meters)

{:id="GoogleMap.UpdateMarker" class="method"} <i/> UpdateMarker(*markerId*{:.number},*propertyName*{:.text},*value*{:.any})
: Set the property of a marker, note that the marker has to be added first or else will throw an exception! Properties include: "color"(hue value ranging from 0~360), "title", "snippet", "draggable"(give either true or false as the value).

{:id="GoogleMap.addOverlay" class="method"} <i/> addOverlay()
: Method for addOverlay

{:id="GoogleMap.addPolygon" class="method"} <i/> addPolygon(*latMin*{:.number},*latMax*{:.number},*lonMin*{:.number},*lonMax*{:.number})
: Method for addPolygon

{:id="GoogleMap.addTileOverlay" class="method"} <i/> addTileOverlay()
: Method for addTileOverlay

{:id="GoogleMap.clearAllPolygons" class="method"} <i/> clearAllPolygons()
: Method for clearAllPolygons

{:id="GoogleMap.drawCentralSquare" class="method"} <i/> drawCentralSquare()
: Method for drawCentralSquare

{:id="GoogleMap.getBoundingBox" class="method returns text"} <i/> getBoundingBox(*latitudeInDegrees*{:.number},*longitudeInDegrees*{:.number},*halfSideInKm*{:.number})
: Method for getBoundingBox

{:id="GoogleMap.getMapCenter" class="method returns text"} <i/> getMapCenter()
: Method for getMapCenter

{:id="GoogleMap.getZoomLevelInfo" class="method returns number"} <i/> getZoomLevelInfo()
: Method for getZoomLevelInfo

## Graph  {#Graph}

Component for displaying web pages
 This is a very limited form of browser.  You can view web pages and
 click on links. It also handles  Javascript. There are lots of things that could be added,
 but this component is mostly for viewing individual pages.  It's not intended to take
 the place of the browser.



### Properties  {#Graph-Properties}

{:.properties}

{:id="Graph.Height" .number .bo} *Height*
: Property for Height

{:id="Graph.HeightPercent" .number .wo .bo} *HeightPercent*
: Specifies the `Graph`'s vertical height as a percentage
 of the [`Screen`'s `Height`](userinterface.html#Screen.Height).

{:id="Graph.Visible" .boolean} *Visible*
: Specifies whether the `Graph` should be visible on the screen.  Value is `true`{:.logic.block}
 if the `Graph` is showing and `false`{:.logic.block} if hidden.

{:id="Graph.Width" .number .bo} *Width*
: Property for Width

{:id="Graph.WidthPercent" .number .wo .bo} *WidthPercent*
: Specifies the horizontal width of the `Graph` as a percentage
 of the [`Screen`'s `Width`](userinterface.html#Screen.Width).

### Events  {#Graph-Events}

{:.events}
None


### Methods  {#Graph-Methods}

{:.methods}

{:id="Graph.CSVstring" class="method"} <i/> CSVstring(*csvString*{:.text},*chartType*{:.text})
: Add CSV graph.

{:id="Graph.GoogleSpreadsheet" class="method"} <i/> GoogleSpreadsheet(*url*{:.text},*query*{:.text},*chartType*{:.text})
: Add Google Chart based on Google spreadsheet url. If you don't know what query is, just leave it blank.

{:id="Graph.SPARQLquery" class="method"} <i/> SPARQLquery(*endpoint*{:.text},*query*{:.text},*chartType*{:.text})
: Add SparQL graph.
