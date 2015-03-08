package com.google.appinventor.client.editor;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.settings.Settings;
import com.google.appinventor.shared.settings.SettingsConstants;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class SettingsEditor extends DialogBox {

  private final Project project;
  private TextBox mapsKeyField;

  public SettingsEditor(Project project) {
    this.project = project;
    this.setStylePrimaryName("ode-DialogBox");
    this.setText("Google Maps Setting");
    mapsKeyField = new TextBox();
    Label label = new Label("Google Maps API Key:");
    VerticalPanel panel2 = new VerticalPanel();
    final Label sha1Label = new Label();
    Ode.getInstance().getUserInfoService().getUserFingerprintSHA1(new AsyncCallback<String>() {
      @Override
      public void onSuccess(String arg0) {
        sha1Label.setText("SHA1: "+arg0);
      }
      @Override
      public void onFailure(Throwable arg0) {
        sha1Label.setText("Could not retrieve SHA1 fingerprint");
      }
    });
    panel2.add(sha1Label);
    HorizontalPanel panel = new HorizontalPanel();
    panel.add(label);
    panel.add(mapsKeyField);
    panel2.add(panel);
    Button saveBtn = new Button("Save");
    saveBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        saveSettings();
      }
    });
    panel = new HorizontalPanel();
    panel.add(saveBtn);
    Button cancelBtn = new Button("Cancel");
    cancelBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        SettingsEditor.this.hide(true);
      }
    });
    panel.add(cancelBtn);
    panel2.add(panel);
    this.add(panel2);
    this.setWidth("100%");
    this.setHeight("70%");this.addStyleName("ode-DialogBox");

    this.center();
    init();
  }

  protected void init() {
    Settings settings = project.getSettings().getSettings(SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS);
    String currentValue = settings.getPropertyValue(SettingsConstants.YOUNG_ANDROID_SETTINGS_MAPS_KEY);
    mapsKeyField.setText(currentValue);
  }

  protected void saveSettings() {
    Settings settings = project.getSettings().getSettings(SettingsConstants.PROJECT_YOUNG_ANDROID_SETTINGS);

    String currentValue = settings.getPropertyValue(SettingsConstants.YOUNG_ANDROID_SETTINGS_MAPS_KEY);
    String newValue = mapsKeyField.getText();
    if (!newValue.equals(currentValue)) {
      settings.changePropertyValue(SettingsConstants.YOUNG_ANDROID_SETTINGS_MAPS_KEY, newValue);
      Ode.getInstance().getEditorManager().scheduleAutoSave(project.getSettings());
    }

    this.hide(true);
  }
}
