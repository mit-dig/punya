// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.explorer.commands;

import static com.google.appinventor.client.Ode.MESSAGES;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.appinventor.client.DesignToolbar;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.widgets.LabeledTextBox;
import com.google.appinventor.client.youngandroid.TextValidators;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidFormNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidPackageNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidSourceNode;
import com.google.appinventor.shared.rpc.semweb.SemWebServiceAsync;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.xedge.jquery.ui.client.model.LabelValuePair;

/**
 * A command that auto generate a LD form.
 *
 * @author weihuali0509@gmail.com (Weihua Li)
 */
public final class GenerateLDFormCommand extends ChainableCommand {

  private static final int MAX_FORM_COUNT = 1;
  private int OkButtonSwitcher = 0;
  private NewLDFormDialog LDFormDialog;
  private VerticalPanel cBoxPanel = new VerticalPanel();
  private List<String> checkBoxCollection = new ArrayList<String>();

  /**
   * Creates a new command for auto generating a LD form
   */
  public GenerateLDFormCommand() {
  }

  @Override
  public boolean willCallExecuteNextCommand() {
    return true;
  }

  @Override
  public void execute(ProjectNode node) {
    if (node instanceof YoungAndroidProjectNode) {
      LDFormDialog = new NewLDFormDialog((YoungAndroidProjectNode) node);
      LDFormDialog.center();
    } else {
      executionFailedOrCanceled();
      throw new IllegalArgumentException("node must be a YoungAndroidProjectNode");
    }
  }

  /**
   * Dialog for getting the name for the new form.
   */
  private class NewLDFormDialog extends DialogBox {
    // UI elements
    private final TextBox ontologyTextBox;
    private final Set<String> otherFormNames;
    private Button okButtonForUri;

    NewLDFormDialog(final YoungAndroidProjectNode projectRootNode) {
      super(false, true);

      setStylePrimaryName("ode-DialogBox");
      setText(MESSAGES.newLDFormTitle());
      VerticalPanel contentPanel = new VerticalPanel();

      final String prefix = "Screen";
      final int prefixLength = prefix.length();
      int highIndex = 0;
      // Collect the existing form names so we can prevent duplicate form names.
      otherFormNames = new HashSet<String>();

      for (ProjectNode source : projectRootNode.getAllSourceNodes()) {
        if (source instanceof YoungAndroidFormNode) {
          String formName = ((YoungAndroidFormNode) source).getFormName();
          otherFormNames.add(formName);

          if (formName.startsWith(prefix)) {
            try {
              highIndex = Math.max(highIndex, Integer.parseInt(formName.substring(prefixLength)));
            } catch (NumberFormatException e) {
              continue;
            }
          }
        }
      }

      String defaultFormName = prefix + (highIndex + 1);
      Ode ode = Ode.getInstance();
      YoungAndroidSourceNode sourceNode = ode.getCurrentYoungAndroidSourceNode();

      ontologyTextBox = new TextBox();
      ontologyTextBox.setText("");
      ontologyTextBox.setVisibleLength(150);
      ontologyTextBox.addKeyUpHandler(new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
          int keyCode = event.getNativeKeyCode();
          if (keyCode == KeyCodes.KEY_ENTER) {
            handleOkClick(projectRootNode);
          } else if (keyCode == KeyCodes.KEY_ESCAPE) {
            hide();
            executionFailedOrCanceled();
          }
        }
      });
      
      Label uriLabel = new Label(MESSAGES.LDOntologyLabel());
      contentPanel.add(uriLabel);
      contentPanel.add(ontologyTextBox);
      String cancelText = MESSAGES.cancelButton();
      String okText = MESSAGES.okButton();

      Button cancelButtonForUri = new Button(cancelText);
      cancelButtonForUri.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          hide();
          executionFailedOrCanceled();
        }
      });

      okButtonForUri = new Button(okText);
      okButtonForUri.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          handleOkClick(projectRootNode);
        }
      });
      
      HorizontalPanel buttonPanel = new HorizontalPanel();
      buttonPanel.add(cancelButtonForUri);
      buttonPanel.add(okButtonForUri);
      buttonPanel.setSize("100%", "24px");
      
      cBoxPanel.setVisible(false);
      contentPanel.add(cBoxPanel);
      contentPanel.add(buttonPanel);
      contentPanel.setSize("320px", "100%");

      add(contentPanel);
    }

    private void handleOkClick(YoungAndroidProjectNode projectRootNode) {
      Ode ode = Ode.getInstance();
      YoungAndroidSourceNode sourceNode = ode.getCurrentYoungAndroidSourceNode();
      if (OkButtonSwitcher==0) {
        AsyncCallback<List<String>> continuation = new AsyncCallback<List<String>>() {

					@Override
					public void onFailure(Throwable arg0) {
						addCheckBoxesToPanel(new ArrayList<String>());
					}

					@Override
					public void onSuccess(List<String> arg0) {
		        addCheckBoxesToPanel(arg0);
					}
        };
        SemWebServiceAsync service = Ode.getInstance().getSemanticWebService();
        service.getProperties(ontologyTextBox.getText(), continuation);
        center(); 
        OkButtonSwitcher++;
      } else {
        addLDFormAction(projectRootNode, sourceNode.getFormName());
        hide();
      }
    }
    
    private void addCheckBoxesToPanel(List<String> properties) {
      for(int i = 0; i<properties.size(); i++){
        final CheckBox cBox = new CheckBox(properties.get(i)+"");
        cBox.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            addCheckBoxToCollection(((CheckBox) event.getSource()).getText());
          }
        });
        cBoxPanel.add(cBox);
      }
      if (properties.size() == 0) {
        hide();
        executionFailedOrCanceled();
        Window.confirm(MESSAGES.confirmCheckInputValue());
        return;
      }
      cBoxPanel.setVisible(true);
      ontologyTextBox.setFocus(true);
    }
    
    private void addCheckBoxToCollection(String cBox) {
    	if (checkBoxCollection.contains(cBox)) {
    		checkBoxCollection.remove(cBox);
    	} else {
        checkBoxCollection.add(cBox);
    	}
    }

    /**
     * Adds a new form to the project.
     *
     * @param newFormName the new form name
     */
    protected void addLDFormAction(final YoungAndroidProjectNode projectRootNode, 
        final String targetFormName) {
      final Ode ode = Ode.getInstance();
      final YoungAndroidPackageNode packageNode = projectRootNode.getPackageNode();
      
      String targetQualifiedFormName = packageNode.getPackageName() + '.' + targetFormName;
      final String targetFormFileId = YoungAndroidFormNode.getFormFileId(targetQualifiedFormName);

      OdeAsyncCallback<Long> callback = new OdeAsyncCallback<Long>(
          // failure message
          MESSAGES.copyFormError()) {
        @Override
        public void onSuccess(Long modDate) {
          final Ode ode = Ode.getInstance();
          ode.updateModificationDate(projectRootNode.getProjectId(), modDate);

          // Add the new form and blocks nodes to the project
          final Project project = ode.getProjectManager().getProject(projectRootNode);
          
          // Add the screen to the DesignToolbar and select the new form editor. 
          // We need to do this once the form editor and blocks editor have been
          // added to the project editor (after the files are completely loaded).
          //
          // TODO(sharon): if we create YaProjectEditor.addScreen() and merge
          // that with the current work done in YaProjectEditor.addFormEditor,
          // consider moving this deferred work to the explicit command for
          // after the form file is loaded.
          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
              ProjectEditor projectEditor = 
                  ode.getEditorManager().getOpenProjectEditor(project.getProjectId());
              DesignToolbar designToolbar = Ode.getInstance().getDesignToolbar();
              FileEditor formEditor = projectEditor.getFileEditor(targetFormFileId);
              long projectId = formEditor.getProjectId();
              executeNextCommand(projectRootNode);
              Window.Location.reload();
            }
          });
        }

        @Override
        public void onFailure(Throwable caught) {
          super.onFailure(caught);
          executionFailedOrCanceled();
        }
      };

      // Create the new form on the backend. The backend will create the form (.scm) and blocks
      // (.blk) files.
      if (checkBoxCollection.size() > 0) {
        ode.getProjectService().addLDForm(projectRootNode.getProjectId(), targetFormFileId, checkBoxCollection, callback);
      }
    }

    @Override
    public void show() {
      super.show();
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          ontologyTextBox.setFocus(true);
        }
      });
    }
  }
}
