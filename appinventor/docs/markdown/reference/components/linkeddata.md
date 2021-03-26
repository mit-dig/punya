---
layout: documentation
title: Linked Data
---

[&laquo; Back to index](index.html)
# Linked Data

Table of Contents:

* [LinkedData](#LinkedData)
* [LinkedDataForm](#LinkedDataForm)
* [LinkedDataListPicker](#LinkedDataListPicker)
* [Reasoner](#Reasoner)

## LinkedData  {#LinkedData}

Component for LinkedData



### Properties  {#LinkedData-Properties}

{:.properties}

{:id="LinkedData.EndpointURL" .text} *EndpointURL*
: Specifies the URL of a SPARQL endpoint.
 The default value is the DBpedia endpoint.

### Events  {#LinkedData-Events}

{:.events}

{:id="LinkedData.FailedHttsPostingFileToWeb"} FailedHttsPostingFileToWeb(*errorMessage*{:.text})
: Event for FailedHttsPostingFileToWeb

{:id="LinkedData.FailedToAddDataToWeb"} FailedToAddDataToWeb(*graph*{:.text},*error*{:.text})
: Event for FailedToAddDataToWeb

{:id="LinkedData.FailedToDeleteDataFromLocal"} FailedToDeleteDataFromLocal()
: Event for FailedToDeleteDataFromLocal

{:id="LinkedData.FailedToDeleteDataFromWeb"} FailedToDeleteDataFromWeb(*graph*{:.text},*error*{:.text})
: Event for FailedToDeleteDataFromWeb

{:id="LinkedData.FailedToExecuteQuery"} FailedToExecuteQuery(*error*{:.text})
: Event raised when a SPARQL query raises an exception.

{:id="LinkedData.FailedToFeedDataToWeb"} FailedToFeedDataToWeb(*error*{:.text})
: Event for FailedToFeedDataToWeb

{:id="LinkedData.FailedToWriteDataToWeb"} FailedToWriteDataToWeb(*graph*{:.text},*error*{:.text})
: This event is raised when the LinkedData component fails to publish a
 graph to a remote SPARQL endpoint.

{:id="LinkedData.FinishedAddingDataToWeb"} FinishedAddingDataToWeb(*graph*{:.text})
: Event for FinishedAddingDataToWeb

{:id="LinkedData.FinishedDeletingDataFromLocal"} FinishedDeletingDataFromLocal()
: Event for FinishedDeletingDataFromLocal

{:id="LinkedData.FinishedDeletingDataFromWeb"} FinishedDeletingDataFromWeb(*graph*{:.text})
: Event for FinishedDeletingDataFromWeb

{:id="LinkedData.FinishedFeedingDataToWeb"} FinishedFeedingDataToWeb()
: Event for FinishedFeedingDataToWeb

{:id="LinkedData.FinishedHttsPostingFileToWeb"} FinishedHttsPostingFileToWeb(*message*{:.text})
: Event for FinishedHttsPostingFileToWeb

{:id="LinkedData.FinishedWritingDataToWeb"} FinishedWritingDataToWeb(*graph*{:.text})
: This event is raised when a graph is successfully published on a remote
 endpoint.

{:id="LinkedData.RetrievedRawResults"} RetrievedRawResults(*type*{:.text},*contents*{:.text})
: This event is raised after a SPARQL engine finishes processing a query
 and the client has received the results, but before those results have
 been processed into objects so that they may be used in conjunction with
 other linked-data-enabled components.

{:id="LinkedData.RetrievedResults"} RetrievedResults(*type*{:.text},*bindings*{:.list})
: This event is raised after a SPARQL engine finishes processing
 a query and the client has received the results.

{:id="LinkedData.UnsupportedQueryType"} UnsupportedQueryType()
: Event raised when a SPARQL query to be executed is not supported
 by the Linked Data component.

### Methods  {#LinkedData-Methods}

{:.methods}

{:id="LinkedData.AddDataFromComponent" class="method returns boolean"} <i/> AddDataFromComponent(*component*{:.component},*subject*{:.text})
: Takes a component implementing the LDComponent interface and uses the properties defined
 there to insert a triple into the model using the given subject.

{:id="LinkedData.AddDataFromLinkedDataForm" class="method returns boolean"} <i/> AddDataFromLinkedDataForm(*form*{:.component})
: Takes a LinkedDataForm component and converts it and any nested elements into
 triples within the model encapsulated by this LinkedData component.

{:id="LinkedData.AddDataToWeb" class="method"} <i/> AddDataToWeb(*graph*{:.text},*noResolveUpdate*{:.boolean})
: Attempts to insert the statements contained within this Linked Data
 component into the endpoint with an optional graph.

{:id="LinkedData.DeleteDataFromLocal" class="method"} <i/> DeleteDataFromLocal()
: Deletes all data from the referent model

{:id="LinkedData.DeleteDataFromWeb" class="method"} <i/> DeleteDataFromWeb(*graph*{:.text},*noResolveUpdate*{:.boolean})
: Attempts to delete the statements contained within this Linked Data
 component from the endpoint with an optional graph.

{:id="LinkedData.ExecuteSPARQLQuery" class="method"} <i/> ExecuteSPARQLQuery(*query*{:.text})
: Execute a SPARQL query on the set EndpointURL of this Linked Data component.
 Currently only supports SELECT queries, and converts all integer types into Long
 and decimal types into Double.

{:id="LinkedData.FeedDataToWeb" class="method"} <i/> FeedDataToWeb()
: Attempts to feed the statements contained within this Linked Data
 component into the endpoint (most likely CSPARQL).

{:id="LinkedData.GetLangStatements" class="method returns list"} <i/> GetLangStatements(*subject*{:.any},*predicate*{:.any},*object*{:.any},*lang*{:.any})
: Method for GetLangStatements

{:id="LinkedData.GetStatements" class="method returns list"} <i/> GetStatements(*subject*{:.any},*predicate*{:.any},*object*{:.any})
: Get statements as a list of triples from the knowledge base. Each argument can either be false
 or a string. False is treated as a wildcard. Strings are interpreted as URIs.

{:id="LinkedData.HttpsPostFileToWeb" class="method"} <i/> HttpsPostFileToWeb(*Url*{:.text},*certificateName*{:.text},*securityToken*{:.text},*filepath*{:.text})
: Method for HttpsPostFileToWeb

{:id="LinkedData.ReadDataFromLocal" class="method returns boolean"} <i/> ReadDataFromLocal(*path*{:.text})
: Read contents of the specified path (local or remote) into the referent model.

{:id="LinkedData.ReadDataFromWeb" class="method returns boolean"} <i/> ReadDataFromWeb(*path*{:.text})
: Read contents of the specified path (local or remote) into the referent model.

{:id="LinkedData.ResultsToSimpleJSON" class="method returns text"} <i/> ResultsToSimpleJSON(*results*{:.list})
: Method for ResultsToSimpleJSON

{:id="LinkedData.ToString" class="method returns text"} <i/> ToString()
: Returns the contents of this LinkedData component as a string. Useful
 for debugging purposes.

{:id="LinkedData.WriteDataToLocal" class="method returns boolean"} <i/> WriteDataToLocal(*path*{:.text})
: Saves the model to the given path on the file system.

{:id="LinkedData.WriteDataToWeb" class="method"} <i/> WriteDataToWeb(*graph*{:.text})
: Write the model represented by the LinkedData component to the
 RDF graph store represented by EndpointURL using the given graph URI.

## LinkedDataForm  {#LinkedDataForm}

Linked Data Form provides a layout in which contained form elements will be
 used to generate structured data. This form is used in conjunction with
 the LinkedData component.



### Properties  {#LinkedDataForm-Properties}

{:.properties}

{:id="LinkedDataForm.AlignHorizontal" .number} *AlignHorizontal*
: A number that encodes how contents of the `LinkedDataForm` are aligned horizontally. The choices
 are: `1` = left aligned, `2` = right aligned, `3` = horizontally centered. Alignment has no
 effect if the `LinkedDataForm`'s [`Width`](#LinkedDataForm.Width) is `Automatic`.

{:id="LinkedDataForm.AlignVertical" .number} *AlignVertical*
: A number that encodes how the contents of the `LinkedDataForm` are aligned vertically. The choices
 are: `1` = aligned at the top, `2` = aligned at the bottom, `3` = vertically centered.
 Alignment has no effect if the `LinkedDataForm`'s [`Height`](#LinkedDataForm.Height) is `Automatic`.

{:id="LinkedDataForm.BackgroundColor" .color} *BackgroundColor*
: Specifies the background color of the LinkedDataForm as an alpha-red-green-blue
 integer.  If an Image has been set, the color change will not be visible
 until the Image is removed.

{:id="LinkedDataForm.FormID" .text} *FormID*
: Gets the Base URI of this form.

{:id="LinkedDataForm.GenerateSubjectURI" .text .ro .bo} *GenerateSubjectURI*
: Returns a URI for the form either by examining its Subject property or
 generated from its contents.

{:id="LinkedDataForm.Height" .number .bo} *Height*
: Specifies the `LinkedDataForm`'s vertical height, measured in pixels.

{:id="LinkedDataForm.HeightPercent" .number .wo .bo} *HeightPercent*
: Specifies the `LinkedDataForm`'s vertical height as a percentage
 of the [`Screen`'s `Height`](userinterface.html#Screen.Height).

{:id="LinkedDataForm.Image" .text} *Image*
: Specifies the path of the background image of the `LinkedDataForm`.

{:id="LinkedDataForm.InverseProperty" .boolean} *InverseProperty*
: Gets whether or not this form represents an inverse property.

{:id="LinkedDataForm.ObjectType" .text} *ObjectType*
: Returns the concept URI for this form.

{:id="LinkedDataForm.PropertyURI" .text} *PropertyURI*
: Gets the Property URI for linking a parent form to this form.

{:id="LinkedDataForm.Subject" .text .bo} *Subject*
: Gets the Subject URI for this form.

{:id="LinkedDataForm.Visible" .boolean} *Visible*
: Specifies whether the `LinkedDataForm` should be visible on the screen.  Value is `true`{:.logic.block}
 if the `LinkedDataForm` is showing and `false`{:.logic.block} if hidden.

{:id="LinkedDataForm.Width" .number .bo} *Width*
: Specifies the horizontal width of the `LinkedDataForm`, measured in pixels.

{:id="LinkedDataForm.WidthPercent" .number .wo .bo} *WidthPercent*
: Specifies the horizontal width of the `LinkedDataForm` as a percentage
 of the [`Screen`'s `Width`](userinterface.html#Screen.Width).

{:id="LinkedDataForm.generateRandomUUID" .text .ro .bo} *generateRandomUUID*
: Property for generateRandomUUID

### Events  {#LinkedDataForm-Events}

{:.events}
None


### Methods  {#LinkedDataForm-Methods}

{:.methods}

{:id="LinkedDataForm.FillFromLinkedData" class="method"} <i/> FillFromLinkedData(*linkedData*{:.component},*uri*{:.text})
: Populate the content of the form using information about resource identified by uri from the
 linked data component.

## LinkedDataListPicker  {#LinkedDataListPicker}

Provides a list picker backed by the results of a SPARQL query.



### Properties  {#LinkedDataListPicker-Properties}

{:.properties}

{:id="LinkedDataListPicker.BackgroundColor" .color} *BackgroundColor*
: Specifies the `LinkedDataListPicker`'s background color as an alpha-red-green-blue
 integer.  If an [`Image`](#LinkedDataListPicker.Image) has been set, the color
 change will not be visible until the [`Image`](#LinkedDataListPicker.Image) is removed.

{:id="LinkedDataListPicker.Enabled" .boolean} *Enabled*
: Specifies whether the `LinkedDataListPicker` should be active and clickable.

{:id="LinkedDataListPicker.EndpointURL" .text} *EndpointURL*
: Gets the endpoint URL for the list picker.

{:id="LinkedDataListPicker.FontBold" .boolean} *FontBold*
: Specifies whether the text of the `LinkedDataListPicker` should be bold.
 Some fonts do not support bold.

{:id="LinkedDataListPicker.FontItalic" .boolean} *FontItalic*
: Specifies whether the text of the `LinkedDataListPicker` should be italic.
 Some fonts do not support italic.

{:id="LinkedDataListPicker.FontSize" .number} *FontSize*
: Specifies the text font size of the `LinkedDataListPicker`, measured in sp(scale-independent pixels).

{:id="LinkedDataListPicker.FontTypeface" .number .do} *FontTypeface*
: Specifies the text font face of the `LinkedDataListPicker` as default, serif, sans
 serif, or monospace.

{:id="LinkedDataListPicker.Height" .number .bo} *Height*
: Specifies the `LinkedDataListPicker`'s vertical height, measured in pixels.

{:id="LinkedDataListPicker.HeightPercent" .number .wo .bo} *HeightPercent*
: Specifies the `LinkedDataListPicker`'s vertical height as a percentage
 of the [`Screen`'s `Height`](userinterface.html#Screen.Height).

{:id="LinkedDataListPicker.Image" .text} *Image*
: Specifies the path of the `LinkedDataListPicker`'s image. If there is both an `Image` and a
 [`BackgroundColor`](#LinkedDataListPicker.BackgroundColor) specified, only the `Image` will be visible.

{:id="LinkedDataListPicker.ObjectType" .text} *ObjectType*
: <p>Object Type specifies a Uniform Resource Identifier (URI) for a type used to identify objects that should appear in the list picker.</p><p>For example, <code>http://xmlns.com/foaf/0.1/Person</code> (foaf:Person), is a commonly used class for identifying people. Specifying it here would generate a list of people from the Endpoint URL.</p>

{:id="LinkedDataListPicker.PropertyURI" .text} *PropertyURI*
: <p>If the list picker is placed within a Linked Data Form, the Property URI specifies the relationship between the object being built by the form and the item selected in the list picker.</p><p>For example, an application for disaster reporting may query a hierarchy of disaster types and present those types using the Linked Data List Picker.</p>

{:id="LinkedDataListPicker.RelationToObject" .text} *RelationToObject*
: <p>RelationToObjectType specifies the relationship between objects presented by the list picker and the Object Type. This field defaults to <code>rdf:type</code>, but other useful values may include <code>skos:narrower</code> or <code>schema:additionalType</code>. The correct property to use will be dependent on the data available in Endpoint URL.</p>

{:id="LinkedDataListPicker.SelectionIndex" .number .ro .bo} *SelectionIndex*
: Returns the index of the user's selection.

{:id="LinkedDataListPicker.SelectionLabel" .text .ro .bo} *SelectionLabel*
: Returns the label of the user's selection.

{:id="LinkedDataListPicker.SelectionURI" .text .ro .bo} *SelectionURI*
: Returns the URI of the user's selection.

{:id="LinkedDataListPicker.Shape" .number .do} *Shape*
: Specifies the shape of the `LinkedDataListPicker`. The valid values for this property are `0` (default),
 `1` (rounded), `2` (rectangle), and `3` (oval). The `Shape` will not be visible if an
 [`Image`](#LinkedDataListPicker.Image) is used.

{:id="LinkedDataListPicker.ShowFeedback" .boolean} *ShowFeedback*
: Specifies if a visual feedback should be shown when a `LinkedDataListPicker` with an assigned
 [`Image`](#LinkedDataListPicker.Image) is pressed.

{:id="LinkedDataListPicker.SubjectIdentifier" .boolean} *SubjectIdentifier*
: <p>If the list picker is placed within a Linked Data Form, the Subject Identifier value indicates whether the selected item in the list picker should be used as an input when the Linked Data component generates a new subject identifier for the thing described by the form.</p>

{:id="LinkedDataListPicker.Text" .text} *Text*
: Specifies the text displayed by the `LinkedDataListPicker`.

{:id="LinkedDataListPicker.TextAlignment" .number .do} *TextAlignment*
: Specifies the alignment of the `LinkedDataListPicker`'s text. Valid values are:
 `0` (normal; e.g., left-justified if text is written left to right),
 `1` (center), or
 `2` (opposite; e.g., right-justified if text is written left to right).

{:id="LinkedDataListPicker.TextColor" .color} *TextColor*
: Specifies the text color of the `LinkedDataListPicker` as an alpha-red-green-blue
 integer.

{:id="LinkedDataListPicker.Visible" .boolean} *Visible*
: Specifies whether the `LinkedDataListPicker` should be visible on the screen.  Value is `true`{:.logic.block}
 if the `LinkedDataListPicker` is showing and `false`{:.logic.block} if hidden.

{:id="LinkedDataListPicker.Width" .number .bo} *Width*
: Specifies the horizontal width of the `LinkedDataListPicker`, measured in pixels.

{:id="LinkedDataListPicker.WidthPercent" .number .wo .bo} *WidthPercent*
: Specifies the horizontal width of the `LinkedDataListPicker` as a percentage
 of the [`Screen`'s `Width`](userinterface.html#Screen.Width).

### Events  {#LinkedDataListPicker-Events}

{:.events}

{:id="LinkedDataListPicker.AfterPicking"} AfterPicking()
: Event to be raised after the `LinkedDataListPicker` activity returns its
 result and the properties have been filled in.

{:id="LinkedDataListPicker.AfterQuery"} AfterQuery()
: This event is raised after the query is executed on the remote endpoint.

{:id="LinkedDataListPicker.BeforePicking"} BeforePicking()
: Event to raise when the `LinkedDataListPicker` is clicked or the picker is shown
 using the [`Open`](#LinkedDataListPicker.Open) method.  This event occurs before the picker is displayed, and
 can be used to prepare the picker before it is shown.

{:id="LinkedDataListPicker.BeforeQuery"} BeforeQuery()
: This event is raised before the query is executed on the remote endpoint.

{:id="LinkedDataListPicker.GotFocus"} GotFocus()
: Indicates the cursor moved over the `LinkedDataListPicker` so it is now possible
 to click it.

{:id="LinkedDataListPicker.LostFocus"} LostFocus()
: Indicates the cursor moved away from the `LinkedDataListPicker` so it is now no
 longer possible to click it.

{:id="LinkedDataListPicker.TouchDown"} TouchDown()
: Indicates that the `LinkedDataListPicker` was pressed down.

{:id="LinkedDataListPicker.TouchUp"} TouchUp()
: Indicates that the `LinkedDataListPicker` has been released.

{:id="LinkedDataListPicker.UnableToRetrieveContent"} UnableToRetrieveContent(*error*{:.text})
: Raised in the event a query fails.

### Methods  {#LinkedDataListPicker-Methods}

{:.methods}

{:id="LinkedDataListPicker.Open" class="method"} <i/> Open()
: Opens the `LinkedDataListPicker`, as though the user clicked on it.

## Reasoner  {#Reasoner}

The <code>Reasoner</code> component derives statements from the contents of a
 <code>LinkedData</code> component.



### Properties  {#Reasoner-Properties}

{:.properties}

{:id="Reasoner.Model" .component} *Model*
: Specifies the base model (A-Box + T-Box) for reasoning.

{:id="Reasoner.RulesEngine" .text} *RulesEngine*
: Specifies the base semantics that should be used for reasoning.

{:id="Reasoner.RulesFile" .text} *RulesFile*
: An optional file containing custom rules to be used during reasoning. The rules are applied
 in addition to any reasoning applied by the choice of [`RulesEngine`](#Reasoner.RulesEngine).

### Events  {#Reasoner-Events}

{:.events}

{:id="Reasoner.ErrorOccurred"} ErrorOccurred(*message*{:.text})
: Runs when the reasoner encounters an error during reasoning.

{:id="Reasoner.ReasoningComplete"} ReasoningComplete()
: Runs when the reasoner has been prepared and any forward-chaining rules have finished.

### Methods  {#Reasoner-Methods}

{:.methods}

{:id="Reasoner.AddRulesFromRuleset" class="method"} <i/> AddRulesFromRuleset(*ruleset*{:.text})
: Adds additional rules to an existing ruleset.

{:id="Reasoner.GetLangStatements" class="method returns list"} <i/> GetLangStatements(*subject*{:.any},*predicate*{:.any},*object*{:.any},*lang*{:.any})
: Method for GetLangStatements

{:id="Reasoner.GetStatements" class="method returns list"} <i/> GetStatements(*subject*{:.any},*predicate*{:.any},*object*{:.any})
: Get statements as a list of triples from the knowledge base. Each argument can either be false
 or a string. False is treated as a wildcard. Strings are interpreted as URIs.

{:id="Reasoner.Query" class="method returns any"} <i/> Query(*query*{:.text})
: Synchronously evaluate a SPARQL query over the knowledge base. The return type depends on the
 type of query run. For SELECT queries, the return value is a JSON-like dictionary containing
 results in the SPARQL 1.1 Query Result format.

{:id="Reasoner.RulesFromRuleset" class="method"} <i/> RulesFromRuleset(*ruleset*{:.text})
: Populate the rule-based reasoner with the given ruleset of custom rules.

{:id="Reasoner.RulesetToString" class="method returns text"} <i/> RulesetToString()
: Return the loaded rules (if any) as a string.

{:id="Reasoner.Run" class="method"} <i/> Run()
: Runs the reasoner in forward chaining model to derive conclusions. On success, the
 [`ReasoningComplete`](#Reasoner.ReasoningComplete) event will run. [`ErrorOccurred`](#Reasoner.ErrorOccurred) event will run if reasoning
 fails.
