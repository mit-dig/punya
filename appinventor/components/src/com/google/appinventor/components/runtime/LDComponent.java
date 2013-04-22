package com.google.appinventor.components.runtime;

/**
 * LDComponent is used to mark {@link AndroidViewComponent}s that provide
 * linked data features to developers. It is used by the {@link SemanticWeb}
 * component to generate linked data from forms.
 * 
 * @author Evan Patton <ewpatton@gmail.com>
 *
 */
public interface LDComponent {
  /**
   * Returns the ConceptURI used to type entries
   * generated from this component.
   * @return
   */
  public String ConceptURI();

  /**
   * Sets the ConceptURI used to type entries
   * generated from this component.
   * @param uri URI or CURIE of a class on the semantic web,
   * e.g. http://xmlns.com/foaf/0.1/Person or foaf:Person
   */
  public void ConceptURI(String uri);

  /**
   * Returns the PropertyURI used to link the subject
   * of a form to the value of this field.
   * @return
   */
  public String PropertyURI();

  /**
   * Sets the PropertyURI used to link the subject of
   * a form to the value of this component.
   * @param uri URI or CURIE of a property on the semantic web,
   * e.g. http://xmlns.com/foaf/0.1/name or foaf:name
   */
  public void PropertyURI(String uri);

  /**
   * Gets a value of the field in an appropriate form. For example,
   * checkboxes would return a java.lang.Boolean whereas textboxes
   * return java.lang.String. The SemanticWeb component will combine
   * type information with the value provided in {@link #ConceptURI()} to
   * generate the appropriate linked data elements. 
   * 
   * @return
   */
  public Object Value();

  /**
   * Returns whether or not the value of this field should be used
   * to identify the subject of a Semantic Form.
   * @see SemanticForm
   * @return
   */
  public boolean SubjectIdentifier();

}
