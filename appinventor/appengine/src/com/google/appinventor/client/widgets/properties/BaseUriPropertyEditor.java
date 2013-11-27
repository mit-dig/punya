package com.google.appinventor.client.widgets.properties;

import static com.google.appinventor.client.Ode.MESSAGES;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.components.common.SemanticWebConstants;

/**
 * A URI editor that requires that the user-provided URI ends with
 * a hash or a slash.
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class BaseUriPropertyEditor extends UriPropertyEditor {

  private boolean autogen = false;

  public BaseUriPropertyEditor(boolean autogen) {
    this.autogen = autogen;
  }

  @Override
  protected void updateValue() {
    if ( property.getValue().equals(SemanticWebConstants.DEFAULT_BASE_URI)
        && autogen ) {
      Ode ode = Ode.getInstance();
      Project project = ode.getProjectManager().getProject(
          ode.getCurrentYoungAndroidProjectId() );
      property.setValue(SemanticWebConstants.DEFAULT_BASE_URI
          + ode.getUser().getUserEmail().replaceFirst("@", "_AT_")
          + ( project != null ? "/" + project.getProjectName() : "" )
          + "/" + System.currentTimeMillis()
          + "/");
    } else {
      super.updateValue();
    }
  }

  @Override
  protected void validate(String text) throws InvalidTextException {
    super.validate(text);
    if (!text.endsWith("#") && !text.endsWith("/")) {
      throw new InvalidTextException(MESSAGES.notABaseUri(text));
    }
  }
}
