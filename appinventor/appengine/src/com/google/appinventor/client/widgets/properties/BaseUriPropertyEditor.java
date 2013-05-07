package com.google.appinventor.client.widgets.properties;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * A URI editor that requires that the user-provided URI ends with
 * a hash or a slash.
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class BaseUriPropertyEditor extends UriPropertyEditor {

  @Override
  protected void validate(String text) throws InvalidTextException {
    super.validate(text);
    if (!text.endsWith("#") && !text.endsWith("/")) {
      throw new InvalidTextException(MESSAGES.notABaseUri(text));
    }
  }
}
