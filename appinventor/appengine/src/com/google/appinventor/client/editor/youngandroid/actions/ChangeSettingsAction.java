// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2024 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.actions;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.editor.SettingsEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;

import com.google.gwt.user.client.Command;

public class ChangeSettingsAction implements Command {
  @Override
  public void execute() {
    ProjectRootNode projectRootNode = Ode.getInstance().getCurrentYoungAndroidProjectRootNode();
    Project p = Ode.getInstance().getProjectManager().getProject(projectRootNode);
    new SettingsEditor(p).show();
  }
}
