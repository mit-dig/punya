package com.google.appinventor.components.runtime;

import android.app.Activity;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;

/**
 * This class is meant to act as a superclass to concrete web service components.
 *
 * @author william.van.woensel@gmail.com
 */
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
        // "android.permission.WRITE_EXTERNAL_STORAGE," +
        // "android.permission.READ_EXTERNAL_STORAGE")

public class WebService extends AndroidNonvisibleComponent implements Component {

    protected final Activity activity;
    protected boolean apiKeyRequired = false;
    protected String apiKey;

    /**
     * Creates a new WebService component.
     *
     * @param container the container that this component will be placed in
     */
    protected WebService(ComponentContainer container) {
        super(container.$form());

        this.activity = container.$context();
    }

    protected boolean checkInput() {
        if (apiKeyRequired && apiKey == null) {
            ServiceError("API key required for this service but none given. See ApiKey property.");
            return false;
        } else
            return true;
    }

    /**
     * The API key for the web service, if any.
     *
     * @return the API key
     */
    @SimpleProperty(description = "The API key for this web service, if any.", category = PropertyCategory.BEHAVIOR)
    public String ApiKey() { return apiKey; }

    /**
     * Specifies the API key.
     *
     * @param apiKey
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
            defaultValue = "")
    @SimpleProperty
    public void ApiKey(String apiKey) { this.apiKey = apiKey; }

    /**
     * Event indicating an error during the service call.
     *
     * @param error
     */
    @SimpleEvent(description = "Event indicating an error during the service call")
    public void ServiceError(final String error) {
        final Component component = this;
        // TODO unclear whether this is needed (done in LocationProbeSensor, not Web)
        activity.runOnUiThread(new Runnable() {
            public void run() {
                EventDispatcher.dispatchEvent(component, "ServiceError", error);
            }
        });
    }

    /**
     * Event indicating that the service call has finished.
     *
     * @param data
     */
    @SimpleEvent(description = "Event indicating that the service call has finished")
    public void ServiceDataReceived(final String data) {
        final Component component = this;
        // TODO unclear whether this is needed (done in LocationProbeSensor, not Web)
        activity.runOnUiThread(new Runnable() {
            public void run() {
                EventDispatcher.dispatchEvent(component, "ServiceDataReceived", data);
            }
        });
    }
}
