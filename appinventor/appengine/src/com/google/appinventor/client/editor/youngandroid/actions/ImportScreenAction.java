// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2013 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.actions;

import com.google.appinventor.client.wizards.ScreenUploadWizard;
import com.google.gwt.user.client.Command;

public class ImportScreenAction implements Command {
  @Override
  public void execute() {
    new ScreenUploadWizard().center();
  }
}
