// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2011-2013 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.actions;

import com.google.appinventor.client.boxes.ProjectListBox;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.utils.Downloader;
import com.google.appinventor.client.Ode;
import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.gwt.user.client.Command;
import java.util.List;

public class ExportProjectScreenAction implements Command {
  @Override
  public void execute() {
    List<Project> selectedProjects =
        ProjectListBox.getProjectListBox().getProjectList().getSelectedProjects();
    if (Ode.getInstance().getCurrentView() != Ode.PROJECTS) {
      //If we are in the designer view.
      Downloader.getInstance().download(ServerLayout.DOWNLOAD_SERVLET_BASE
          + ServerLayout.DOWNLOAD_PROJECT_SOURCE_SCREEN
          + "/" + Ode.getInstance().getCurrentYoungAndroidProjectId()
          + "/" + Ode.getInstance().getCurrentYoungAndroidProjectRootNode().getName()
          + "/" + Ode.getInstance().getCurrentYoungAndroidSourceNode().getFormName());
    }
  }
}
