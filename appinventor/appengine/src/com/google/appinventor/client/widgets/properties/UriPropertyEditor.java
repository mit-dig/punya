package com.google.appinventor.client.widgets.properties;

/**
 * Basic editor for providing URI fields for properties.
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
public class UriPropertyEditor extends TextPropertyEditor {

  @Override
  protected void validate(String text) throws InvalidTextException {
    super.validate(text);
    // TODO(ewpatton): URI validation
  }

}
