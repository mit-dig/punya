// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.errors.PermissionException;
import com.google.appinventor.components.runtime.util.BulkPermissionRequest;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.FileUtil;
import android.Manifest;
import android.os.Environment;
import android.util.Log;

import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.YailList;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.io.IOException;

import com.google.appinventor.components.annotations.UsesLibraries;


/**
 * ![LdpCoapClient](images/ldpCoap.png)
 *
 * LdpCoapClient to make ldp coap request
 *
 */
@DesignerComponent(version = YaVersion.LDPCOAP_CLIENT_COMPONENT_VERSION,
        description = "<p>LDP-COAP Client</p>",
        category = ComponentCategory.CONNECTIVITY,
        nonVisible = true,
        iconName = "images/ldpcoapclient.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET," +
        "android.permission.WRITE_EXTERNAL_STORAGE," +
        "android.permission.READ_EXTERNAL_STORAGE")
@UsesLibraries(libraries= "californium-core-ldp-1.0.0-SNAPSHOT.jar,"+
"element-connector-1.0.7.jar")

public final class LdpCoapClient extends AndroidNonvisibleComponent
        implements Component {

    private static final String TAG = "LdpCoapClient";

    protected String BASE_URI;
    protected CoapResponse resp = null;
    protected String containerType = "ldp:BasicContainer";

    public LdpCoapClient(final ComponentContainer<? extends Component> container) {
        super(container.$form());
    }
    
    public byte[] parseHexBinary(String s) {
    final int len = s.length();

    // "111" is not a valid hex encoding.
    if( len%2 != 0 )
        throw new IllegalArgumentException("hexBinary needs to be even-length: "+s);

    byte[] out = new byte[len/2];

    for( int i=0; i<len; i+=2 ) {
        int h = hexToBin(s.charAt(i));
        int l = hexToBin(s.charAt(i+1));
        if( h==-1 || l==-1 )
            throw new IllegalArgumentException("contains illegal character for hexBinary: "+s);

        out[i/2] = (byte)(h*16+l);
    }
    return out;
    }
    private static int hexToBin( char ch ) {
    if( '0'<=ch && ch<='9' )    return ch-'0';
    if( 'A'<=ch && ch<='F' )    return ch-'A'+10;
    if( 'a'<=ch && ch<='f' )    return ch-'a'+10;
    return -1;
    }

    @SimpleProperty(description = "Set BASE URI", category = PropertyCategory.BEHAVIOR)
    public void BASE_URI(String URI) {
        BASE_URI = URI;
    }
    @SimpleProperty(description = "Get BASE URI", category = PropertyCategory.BEHAVIOR)
    public String BASE_URI() {
        return BASE_URI;
    }
    @SimpleFunction(description = "Get response code")
    public String getResponseCode() {
    if (resp!= null)
        return resp.getCode().toString();
    return "";
    }
    @SimpleFunction(description = "Get Location Path")
    public String getLocationPath() {
    if (resp!= null)
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            return resp.getOptions().getLocationPathString();
        }
        return "";
    }
    @SimpleFunction(description = "get request Status")
    public String getRequestStatus() {
    if (resp!= null)
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            return "Success";
        }
        else if(CoAP.ResponseCode.isClientError(resp.getCode())) {
            return "Client error";
        }
        else if(CoAP.ResponseCode.isServerError(resp.getCode())) {
            return "Server Error";
        }
        return "Not Executed";
    }

    @SimpleFunction(description = "LDP-CoAP Get method")
    public void Get(String resource, int type) {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/" + resource);
        resp = client.get(type);
    }

    @SimpleFunction(description = "LDP Resource Discovery in text/turtle")
    public void DiscoveryResourcesTextTurtle() {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/.well-known/core");
        resp = client.get(MediaTypeRegistry.TEXT_TURTLE);
    }
    @SimpleFunction(description = "LDP Resource Discovery in text/plain")
    public void DiscoveryResourcesTextPlain() {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/.well-known/core");
        resp = client.get(MediaTypeRegistry.TEXT_PLAIN);
    }
    @SimpleFunction(description = "LDP Resource Discovery in application/rdf-patch")
    public void DiscoveryResourcesRdfPatch() {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/.well-known/core");
        resp = client.get(MediaTypeRegistry.APPLICATION_RDF_PATCH);
    }

    @SimpleFunction(description = "CoAP Delete resource")
    public void Delete(String resource) {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/" + resource);
        resp = client.delete();
    }

    @SimpleFunction(description = "POST Request")
    public void Post(String resource, int type, String data, String title) {
        CoapClient client;
        if(resource.contains("?")){
            client = new CoapClient(BASE_URI + "/" + resource + "&title=" + title);
        }
        else {
            client = new CoapClient(BASE_URI + "/" + resource + "?title=" + title);
        }
        resp = client.post(data, type);
    }

    @SimpleFunction(description = "HEAD Request")
    public void Head(String resource) {
        CoapClient client;
        if(resource.contains("?")){
            client = new CoapClient(BASE_URI + "/" + resource + "&ldp=head");
        }
        else {
            client = new CoapClient(BASE_URI + "/" + resource + "?ldp=head");
        }
        resp = client.get();
    }

    @SimpleFunction(description = "Get ETag")
    public String GetETag() {
    if (resp!= null)
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            return resp.getOptions().toString().substring(10, 26);
        }
        return "";
    }

    @SimpleFunction(description = "Get Content-Format (ct)")
    public String getContentFormat() {
    if (resp!= null)
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            return MediaTypeRegistry.toString(resp.getOptions().getContentFormat());
        }
        return "";
    }

    @SimpleFunction(description = "OPTIONS Request")
    public void Options(String resource) {
        CoapClient client;
        if(resource.contains("?")){
            client = new CoapClient(BASE_URI + "/" + resource + "&ldp=options");
        }
        else {
            client = new CoapClient(BASE_URI + "/" + resource + "?ldp=options");
        }
        resp = client.get();
    }

    @SimpleFunction(description = "Get Response Text")
    public String getResponseText() {
    if (resp!= null)
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            return resp.getResponseText();
        }
        return "";
    }

    @SimpleFunction(description = "PUT Request")
    public void Put(String resource, int type, String data) {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/" + resource);
        resp = client.get();
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            byte[] etag = parseHexBinary(resp.getOptions().toString().substring(10, 26));
            resp = client.putIfMatch(data, type, etag);
        }
    }
    @SimpleFunction(description = "PUT Request")
    public void PutEtagInput(String resource, int type, String data, String etag) {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/" + resource);
        resp = client.putIfMatch(data, type, parseHexBinary(etag));
    }
    @SimpleFunction(description = "PATCH Request")
    public void Patch(String resource, int type, String data) {
        CoapClient client;
        client = new CoapClient(BASE_URI + "/" + resource);
        resp = client.get();
        if(resource.contains("?")){
            client = new CoapClient(BASE_URI + "/" + resource + "$ldp=patch");
        }
        else {
            client = new CoapClient(BASE_URI + "/" + resource + "?ldp=patch");
        }
        if(CoAP.ResponseCode.isSuccess(resp.getCode())) {
            byte[] etag = parseHexBinary(resp.getOptions().toString().substring(10, 26));
            resp = client.putIfMatch(data, type, etag);
        }
    }
    @SimpleFunction(description = "PATCH Request")
    public void PatchEtagInput(String resource, int type, String data, String etag) {
        CoapClient client;
        if(resource.contains("?")){
            client = new CoapClient(BASE_URI + "/" + resource + "$ldp=patch");
        }
        else {
            client = new CoapClient(BASE_URI + "/" + resource + "?ldp=patch");
        }
        resp = client.putIfMatch(data, type, parseHexBinary(etag));
    }
    @SimpleProperty(description = "text/plain code")
    public int TextPlain() {
        return MediaTypeRegistry.TEXT_PLAIN;
    }
    @SimpleProperty(description = "text/csv code")
    public int TextCsv() {
        return MediaTypeRegistry.TEXT_CSV;
    }
    @SimpleProperty(description = "text/html code")
    public int TextHtml() {
        return MediaTypeRegistry.TEXT_HTML;
    }
    @SimpleProperty(description = "image/gif code")
    public int ImageGif() {
        return MediaTypeRegistry.IMAGE_GIF;
    }
    @SimpleProperty(description = "image/jpeg code")
    public int ImageJpeg() {
        return MediaTypeRegistry.IMAGE_JPEG;
    }
    @SimpleProperty(description = "image/png code")
    public int ImagePng() {
        return MediaTypeRegistry.IMAGE_PNG;
    }
    @SimpleProperty(description = "image/tiff code")
    public int ImageTiff() {
        return MediaTypeRegistry.IMAGE_TIFF;
    }
    @SimpleProperty(description = "application/link-format code")
    public int ApplicationLinkFormat() {
        return MediaTypeRegistry.APPLICATION_LINK_FORMAT;
    }
    @SimpleProperty(description = "application/xml code")
    public int ApplicationXml() {
        return MediaTypeRegistry.APPLICATION_XML;
    }
    @SimpleProperty(description = "application/octet-stream code")
    public int ApplicationOctetStream() {
        return MediaTypeRegistry.APPLICATION_OCTET_STREAM;
    }
    @SimpleProperty(description = "application/rdf+xml code")
    public int ApplicationRdfXml() {
        return MediaTypeRegistry.APPLICATION_RDF_XML;
    }
    @SimpleProperty(description = "application/soap+xml code")
    public int ApplicationSoapXml() {
        return MediaTypeRegistry.APPLICATION_SOAP_XML;
    }
    @SimpleProperty(description = "application/atom+xml code")
    public int ApplicationAtomXml() {
        return MediaTypeRegistry.APPLICATION_ATOM_XML;
    }
    @SimpleProperty(description = "application/xmpp+xml code")
    public int ApplicationXmppXml() {
        return MediaTypeRegistry.APPLICATION_XMPP_XML;
    }
    @SimpleProperty(description = "application/exi code")
    public int ApplicationExi() {
        return MediaTypeRegistry.APPLICATION_EXI;
    }
    @SimpleProperty(description = "application/fastinfoset code")
    public int ApplicationFastinfoset() {
        return MediaTypeRegistry.APPLICATION_FASTINFOSET;
    }
    @SimpleProperty(description = "application/soap+fastinfoset code")
    public int ApplicationSoapFastinfoset() {
        return MediaTypeRegistry.APPLICATION_SOAP_FASTINFOSET;
    }
    @SimpleProperty(description = "application/json code")
    public int ApplicationJson() {
        return MediaTypeRegistry.APPLICATION_JSON;
    }
    @SimpleProperty(description = "application/x-obix-binary code")
    public int ApplicationXobixBinary() {
        return MediaTypeRegistry.APPLICATION_X_OBIX_BINARY;
    }
    @SimpleProperty(description = "text/turtle code")
    public int TextTurtle() {
        return MediaTypeRegistry.TEXT_TURTLE;
    }
    @SimpleProperty(description = "application/ld+json code")
    public int ApplicationLdJson() {
        return MediaTypeRegistry.APPLICATION_LD_JSON;
    }
    @SimpleProperty(description = "application/rdf-patch code")
    public int ApplicationRdfPatch() {
        return MediaTypeRegistry.APPLICATION_RDF_PATCH;
    }
    @SimpleProperty(description = "application/gzip code")
    public int ApplicationGzip() {
        return MediaTypeRegistry.APPLICATION_GZIP;
    }
    @SimpleProperty(description = "application/bz2 code")
    public int ApplicationBzip2() {
        return MediaTypeRegistry.APPLICATION_BZIP2;
    }
    @SimpleProperty(description = "application/bson code")
    public int ApplicationBson() {
        return MediaTypeRegistry.APPLICATION_BSON;
    }
    @SimpleProperty(description = "application/ubjson code")
    public int ApplicationUbjson() {
        return MediaTypeRegistry.APPLICATION_UBJSON;
    }
    @SimpleProperty(description = "application/msgpack code")
    public int ApplicationMsgpack() {
        return MediaTypeRegistry.APPLICATION_MSGPACK;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CONCEPT_URI,
            defaultValue = "ldp:BasicContainer")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void ContainerType(String type) {
        this.containerType = type;
    }

    @SimpleProperty
    public String ContainerType() {
        return containerType;
    }

    @Deprecated   // remove when ready for production
    @SimpleFunction
    public void RegisterSensor(ObservableDataSource<?, ?> sensor, String name, YailDictionary fields) {
        Post(
            name,
            MediaTypeRegistry.TEXT_TURTLE,
            "...",  // TODO(Floriano): Can you provide the Turtle template here?
            name + "-" + sensor.getClass().getSimpleName() + "&rt=" + containerType
        );
    }
}
