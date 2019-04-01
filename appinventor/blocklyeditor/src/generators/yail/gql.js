// -*- mode: javascript; js-indent-level: 2; -*-
// Copyright © MIT, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

'use strict';

goog.provide('Blockly.Yail.gql');

// Function to join strings.
function gqlJoin(combination) {
  // Call string append primitive.
  var code = Blockly.Yail.YAIL_CALL_YAIL_PRIMITIVE + 'string-append' + Blockly.Yail.YAIL_SPACER;

  // Start list.
  code += Blockly.Yail.YAIL_OPEN_COMBINATION + Blockly.Yail.YAIL_LIST_CONSTRUCTOR;

  // Add items to list.
  for (var i = 0; i < combination.length; i++) {
    code += Blockly.Yail.YAIL_SPACER + combination[i];
  }

  // End list.
  code += Blockly.Yail.YAIL_CLOSE_COMBINATION;

  // Start coercion.
  code += Blockly.Yail.YAIL_SPACER + Blockly.Yail.YAIL_QUOTE + Blockly.Yail.YAIL_OPEN_COMBINATION;

  // Add text types.
  for (var i = 0; i < combination.length; i++) {
    code += 'text';

    // Add a space in between.
    if (i !== combination.length - 1) {
      code += Blockly.Yail.YAIL_SPACER;
    }
  }

  // Close combination.
  code += Blockly.Yail.YAIL_CLOSE_COMBINATION;

  // Indicate that we are performing a join.
  code += Blockly.Yail.YAIL_SPACER + Blockly.Yail.YAIL_DOUBLE_QUOTE + 'join' + Blockly.Yail.YAIL_DOUBLE_QUOTE;

  // Close final combination.
  code += Blockly.Yail.YAIL_CLOSE_COMBINATION;

  // Return code.
  return code;
}

// Code generator for the GraphQL null type.
Blockly.Yail['gql_null'] = function() {
  // Create a null literal.
  var code = Blockly.Yail.YAIL_OPEN_COMBINATION
    + 'GqlLiteral' + Blockly.Yail.YAIL_SPACER + Blockly.Yail.quote_('null')
    + Blockly.Yail.YAIL_CLOSE_COMBINATION;

  // Return code.
  return [code, Blockly.Yail.ORDER_ATOMIC];
};

// Code generator for the GraphQL enumeration type.
Blockly.Yail['gql_enum'] = function() {
  // Create a enum literal.
  var code = Blockly.Yail.YAIL_OPEN_COMBINATION
    + 'GqlLiteral' + Blockly.Yail.YAIL_SPACER + Blockly.Yail.quote_(this.getGqlValue())
    + Blockly.Yail.YAIL_CLOSE_COMBINATION;

  // Return code.
  return [code, Blockly.Yail.ORDER_ATOMIC];
};

// Code generator for the GraphQL pair type.
Blockly.Yail['gql_pair'] = function() {
  // Get the key and value.
  var key = Blockly.Yail.valueToCode(this, 'KEY', Blockly.Yail.ORDER_NONE) || null;
  var value = Blockly.Yail.valueToCode(this, 'VALUE', Blockly.Yail.ORDER_NONE) || null;

  // Ignore pair if either the key or value is not filled.
  if (key === null || value === null) {
    return ['', Blockly.Yail.ORDER_ATOMIC];
  }

  // Quote the value if necessary (null values are handled here).
  if (this.gqlQuote) {
    value = Blockly.Yail.YAIL_OPEN_COMBINATION
      + 'GraphQL:quote' + Blockly.Yail.YAIL_SPACER + value
      + Blockly.Yail.YAIL_CLOSE_COMBINATION;
  }

  // Define combination and generate code.
  var combination = [key, Blockly.Yail.quote_(': '), value];
  var code = gqlJoin(combination);

  // Return code.
  return [code, Blockly.Yail.ORDER_ATOMIC];
};

// Code generator for the GraphQL dictionary type.
Blockly.Yail['gql_dict'] = function() {
  // Create a list for strings.
  var combination = [];

  // Determine if the dictionary is anonymous (argument versus input object).
  var isInputObject = this.gqlBaseType.indexOf('.') === -1;

  // Start with a open brace or parenthesis.
  combination.push(Blockly.Yail.quote_(isInputObject ? '{' : '('));

  // Go through all items.
  for (var i = 0; i < this.itemCount_; i++) {
    // Get the argument.
    var argument = Blockly.Yail.valueToCode(this, 'ADD' + i, Blockly.Yail.ORDER_NONE) || null;

    // Ignore null arguments.
    if (argument != null) {
      combination.push(argument);

      // Add a comma in between.
      if (i !== this.itemCount_ - 1) {
        combination.push(Blockly.Yail.quote_(', '));
      }
    }
  }

  // Close brace or parenthesis.
  combination.push(Blockly.Yail.quote_(isInputObject ? '}' : ')'));

  // Generate code from as a string join.
  var code = gqlJoin(combination);

  // Return code.
  return [code, Blockly.Yail.ORDER_ATOMIC];
};

// Code generator for the GraphQL list type.
Blockly.Yail['gql_list'] = function() {
  // Create a list for strings.
  var combination = [];

  // Start with a open bracket.
  combination.push(Blockly.Yail.quote_('['));

  // Go through all items.
  for (var i = 0; i < this.itemCount_; i++) {
    // Get the argument.
    var argument = Blockly.Yail.valueToCode(this, 'ADD' + i, Blockly.Yail.ORDER_NONE) || null;

    // Ignore null arguments.
    if (argument != null) {
      // Quote the value if necessary (null values are handled here).
      if (this.gqlQuote) {
        argument = Blockly.Yail.YAIL_OPEN_COMBINATION
          + 'GraphQL:quote' + Blockly.Yail.YAIL_SPACER + argument
          + Blockly.Yail.YAIL_CLOSE_COMBINATION;
      }

      // Add argument.
      combination.push(argument);

      // Add a comma in between.
      if (i !== this.itemCount_ - 1) {
        combination.push(Blockly.Yail.quote_(', '));
      }
    }
  }

  // Close bracket.
  combination.push(Blockly.Yail.quote_(']'));

  // Generate code from as a string join.
  var code = gqlJoin(combination);

  // Return code.
  return [code, Blockly.Yail.ORDER_ATOMIC];
};

// Code generator for GraphQL blocks.
Blockly.Yail['gql'] = function() {
  // If the blocks is a scalar, then the code is just the field name.
  if (!this.gqlHasFields) {
    return [Blockly.Yail.quote_(this.gqlName), Blockly.Yail.ORDER_ATOMIC];
  }

  // Create a list for strings.
  var combination = [];

  // Get the selection name.
  var selectionName = (this.gqlParent === null) ? '... on ' + this.gqlName : this.gqlName;
  combination.push(Blockly.Yail.quote_(selectionName));

  // Add arguments if necessary.
  if (this.gqlHasArguments) {
    // Get the argument string.
    var argumentString = Blockly.Yail.valueToCode(this, 'GQL_TITLE', Blockly.Yail.ORDER_NONE) || null;

    // Add the argument string if there is an attached dictionary.
    if (argumentString !== null) {
      combination.push(argumentString);
    }
  }

  // Open selection set.
  combination.push(Blockly.Yail.quote_(' { '));

  // Go through all selections.
  for (var i = 0; i < this.itemCount_; i++) {
    // Get the selection.
    var selection = Blockly.Yail.valueToCode(this, 'GQL_FIELD' + i, Blockly.Yail.ORDER_NONE) || null;

    // Ignore empty selections.
    if (selection != null) {
      combination.push(selection);

      // Add a space in between.
      if (i !== this.itemCount_ - 1) {
        combination.push(Blockly.Yail.quote_(' '));
      }
    }
  }

  // Close selection set.
  combination.push(Blockly.Yail.quote_(' }'));

  // Generate code from as a string join.
  var code = gqlJoin(combination);

  // Return code.
  return [code, Blockly.Yail.ORDER_ATOMIC];
};
