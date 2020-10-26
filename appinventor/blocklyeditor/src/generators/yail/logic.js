// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2012-2020 Massachusetts Institute of Technology. All rights reserved.

/**
 * @license
 * @fileoverview Logic blocks yail generators for Blockly, modified for MIT App Inventor.
 * @author mckinney@mit.edu (Andrew F. McKinney)
 */

'use strict';

goog.provide('Blockly.Yail.logic');

Blockly.Yail['logic_boolean'] = function() {
  // Boolean values true and false.
  var code = (this.getFieldValue('BOOL') == 'TRUE') ? Blockly.Yail.YAIL_TRUE
      : Blockly.Yail.YAIL_FALSE;
  return [ code, Blockly.Yail.ORDER_ATOMIC ];
};

Blockly.Yail['logic_false'] = function() {
  return Blockly.Yail.logic_boolean.call(this);
}

Blockly.Yail['logic_negate'] = function() {
  // negate operation
  var argument = Blockly.Yail
      .valueToCode(this, 'BOOL', Blockly.Yail.ORDER_NONE)
      || Blockly.Yail.YAIL_FALSE;
  var code = Blockly.Yail.YAIL_CALL_YAIL_PRIMITIVE + "yail-not"
      + Blockly.Yail.YAIL_SPACER;
  code = code + Blockly.Yail.YAIL_OPEN_COMBINATION
      + Blockly.Yail.YAIL_LIST_CONSTRUCTOR + Blockly.Yail.YAIL_SPACER
      + argument + Blockly.Yail.YAIL_CLOSE_COMBINATION;
  code = code + Blockly.Yail.YAIL_SPACER + Blockly.Yail.YAIL_QUOTE
      + Blockly.Yail.YAIL_OPEN_COMBINATION + "boolean"
      + Blockly.Yail.YAIL_CLOSE_COMBINATION + Blockly.Yail.YAIL_SPACER;
  code = code + Blockly.Yail.YAIL_DOUBLE_QUOTE + "not"
      + Blockly.Yail.YAIL_DOUBLE_QUOTE + Blockly.Yail.YAIL_CLOSE_COMBINATION;
  return [ code, Blockly.Yail.ORDER_ATOMIC ];
};

Blockly.Yail['logic_operation'] = function() {
  // The and, or logic operations
  // TODO: (Andrew) Make these take multiple arguments.
  var mode = this.getFieldValue('OP');
  var tuple = Blockly.Yail.logic_operation.OPERATORS[mode];
  var operator = tuple[0];
  var order = tuple[1];
  var argument0 = Blockly.Yail.valueToCode(this, 'A', order) || Blockly.Yail.YAIL_FALSE;
  var argument1 = Blockly.Yail.valueToCode(this, 'B', order) || Blockly.Yail.YAIL_FALSE;
  var code = Blockly.Yail.YAIL_OPEN_COMBINATION + operator
      + Blockly.Yail.YAIL_SPACER + argument0 + Blockly.Yail.YAIL_SPACER
      + argument1 + Blockly.Yail.YAIL_CLOSE_COMBINATION;
  return [ code, Blockly.Yail.ORDER_ATOMIC ];
};

Blockly.Yail.logic_operation.OPERATORS = {
  AND : [ 'and-delayed', Blockly.Yail.ORDER_NONE ],
  OR : [ 'or-delayed', Blockly.Yail.ORDER_NONE ]
};

Blockly.Yail['logic_or'] = function() {
  return Blockly.Yail.logic_operation.call(this);
}

Blockly.Yail['logic_compare'] = function() {
  // Basic logic compare operators
  // // TODO: (Hal) handle any type?
  var argument0 = Blockly.Yail.valueToCode(this, 'A', Blockly.Yail.ORDER_NONE) || Blockly.Yail.YAIL_FALSE;
  var argument1 = Blockly.Yail.valueToCode(this, 'B', Blockly.Yail.ORDER_NONE) || Blockly.Yail.YAIL_FALSE;
  var yailCommand = (this.getFieldValue('OP') == "NEQ" ? 'yail-not-equal?' : "yail-equal?" );
  var code = Blockly.Yail.YAIL_CALL_YAIL_PRIMITIVE + yailCommand
      + Blockly.Yail.YAIL_SPACER;
  code = code + Blockly.Yail.YAIL_OPEN_COMBINATION
      + Blockly.Yail.YAIL_LIST_CONSTRUCTOR + Blockly.Yail.YAIL_SPACER
      + argument0 + Blockly.Yail.YAIL_SPACER + argument1
      + Blockly.Yail.YAIL_CLOSE_COMBINATION;
  code = code + Blockly.Yail.YAIL_SPACER + Blockly.Yail.YAIL_QUOTE
      + Blockly.Yail.YAIL_OPEN_COMBINATION + "any" + Blockly.Yail.YAIL_SPACER
      + "any" + Blockly.Yail.YAIL_CLOSE_COMBINATION + Blockly.Yail.YAIL_SPACER;
  code = code + Blockly.Yail.YAIL_DOUBLE_QUOTE + "="
      + Blockly.Yail.YAIL_DOUBLE_QUOTE + Blockly.Yail.YAIL_CLOSE_COMBINATION;
  return [ code, Blockly.Yail.ORDER_ATOMIC ];
};

/// region Semantic Web

Blockly.Yail['logic_ruleset'] = function() {
  var rules = Blockly.Rules.statementToCode(this, 'RULES');
  var code = Blockly.Yail.quote_(rules);
  return [ code, Blockly.Yail.ORDER_ATOMIC ];
}

/**
 * Defines a global namespace for RDF use.
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.Yail['logic_namespace_decl'] = function() {
  var code = '(define-namespace  ';
  code += Blockly.Yail.quote_(this.getFieldValue('PREFIX') || 'ex');
  code += ' ';
  code += Blockly.Yail.valueToCode(this, 'URI', Blockly.Yail.ORDER_NONE);
  code += ')';
  return code;
}

/**
 * Converts a URI block into a YAIL string.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.Yail['logic_uri'] = function() {
  return [Blockly.Yail.quote_(this.getFieldValue('URI')), Blockly.Yail.ORDER_ATOMIC];
}

/**
 * Converts a variable binding reference into a YAIL string.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.Yail['logic_binding'] = function() {
  return [Blockly.Yail.quote_(this.getFieldValue('VARNAME')), Blockly.Yail.ORDER_ATOMIC];
}

/**
 * Converts a QName to an expansion based on registered namespaces.
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.Yail['logic_qname'] = function() {
  var code = '(expand-qname ';
  code += Blockly.Yail.quote_(this.getFieldValue('PREFIX') + ':' + this.getFieldValue('LOCALNAME'));
  code += ')';
  return [code, Blockly.Yail.ORDER_ATOMIC];
}

/**
 * Converts a SPARQL block into a string for YAIL.
 *
 * @this Blockly.Block
 * @returns {[string, number]}
 */
Blockly.Yail['logic_sparql_select'] = function() {
  var query = Blockly.SPARQL.blockToCode1(this);
  var code;
  if (Blockly.SPARQL.yailBlocks) {
    var parts = query.split('```');
    code = '(string-append';
    for (var i = 0; i < parts.length; i++) {
      code += ' ';
      if (parts[i].substr(0, 4) === 'yail') {
        code += parts[i].substr(4);
      } else {
        code += Blockly.Yail.quote_(parts[i]);
      }
    }
    code += ')';
  } else {
    code = Blockly.Yail.quote_(query);
  }
  return [ code, Blockly.Yail.ORDER_ATOMIC ];
}

/// endregion
