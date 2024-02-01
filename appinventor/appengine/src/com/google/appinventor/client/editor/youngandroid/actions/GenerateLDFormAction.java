// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2023 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.actions;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.explorer.commands.ChainableCommand;
import com.google.appinventor.client.explorer.commands.GenerateLDFormCommand;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.gwt.user.client.Command;

public class GenerateLDFormAction implements Command {
  @Override
  public void execute() {
    Ode ode = Ode.getInstance();
    if (ode.screensLocked()) {
      return;                 // Don't permit this if we are locked out (saving files)
    }
    ProjectRootNode projectRootNode = ode.getCurrentYoungAndroidProjectRootNode();
    if (projectRootNode != null) {
      ChainableCommand cmd = new GenerateLDFormCommand();
      cmd.startExecuteChain(Tracking.PROJECT_ACTION_GENERATELDFORM_YA, projectRootNode);
    }
  }
}
