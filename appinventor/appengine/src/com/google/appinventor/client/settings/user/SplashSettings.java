// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.settings.user;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.settings.Settings;
import com.google.appinventor.client.widgets.properties.EditableProperty;
import com.google.appinventor.shared.rpc.user.UserInfoProvider;
import com.google.appinventor.shared.settings.SettingsConstants;

/**
 * General settings.
 *
 */
public final class SplashSettings extends Settings {
  /**
   * Creates a new instance of user-specific general settings.
   *
   * @param user  user associated with settings
   */
  public SplashSettings(UserInfoProvider user) {
    super(SettingsConstants.SPLASH_SETTINGS);

    addProperty(new EditableProperty(this, SettingsConstants.SPLASH_SETTINGS_SHOWSURVEY,
        "0", null, EditableProperty.TYPE_INVISIBLE));
    addProperty(new EditableProperty(this, SettingsConstants.SPLASH_SETTINGS_DECLINED,
        "", null, EditableProperty.TYPE_INVISIBLE));
    addProperty(new EditableProperty(this, SettingsConstants.SPLASH_SETTINGS_VERSION,
        "0", null, EditableProperty.TYPE_INVISIBLE));
  }

  @Override
  protected void updateAfterDecoding() {
    Ode.getInstance().showSplashScreens();
  }
}
