// -*- mode: javascript; js-indent-offset: 2; -*-
// Copyight 2020 Massachusetts Institute of Technology, All rights reserved.

'use strict';

goog.provide('Blockly.Rules');

goog.require('Blockly.Generator');

Blockly.Rules = new Blockly.Generator('Rules');
Blockly.Rules.ORDER_ATOMIC = 0;
Blockly.Rules.ORDER_NONE = 99;
Blockly.Rules.RULES_EMTPY_URI = '<>';
Blockly.Rules.INDENT = '';

/**
 *
 * @param {Blockly.Block} block
 * @param {string} code
 * @private
 */
Blockly.Rules.scrub_ = function(block, code) {
  var next = block.getNextBlock();
  if (next) {
    code = code + '\n' + Blockly.Rules.blockToCode(next);
  }
  return code;
}

/**
 *
 * @returns {Array}
 * @this {Blockly.Block}
 */
Blockly.Rules['text'] = function() {
  var text = this.getFieldValue('TEXT');
  text = text.replace(/"/, '\\"');
  text = text.replace(/\n/, '\\n');
  return ['"' + text + '"', Blockly.Rules.ORDER_ATOMIC];
}

/**
 *
 * @returns {Array}
 * @this {Blockly.Block}
 */
Blockly.Rules['math_number'] = function() {
  return [this.getFieldValue('NUM'), Blockly.Rules.ORDER_ATOMIC];
}

/**
 *
 * @returns {Array}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_boolean'] = function() {
  return [this.getFieldValue('logic_boolean').toLowerCase(), Blockly.Rules.ORDER_ATOMIC];
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_forward_rule'] = function() {
  var body = Blockly.Rules.statementToCode(this, 'ANTECEDENT');
  var head = Blockly.Rules.statementToCode(this, 'CONSEQUENT');
  return body + ' -> ' + head + ' .';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_backward_rule'] = function() {
  var head = Blockly.Rules.statementToCode(this, 'CONSEQUENT');
  var body = Blockly.Rules.statementToCode(this, 'ANTECEDENT');
  return head + ' <- ' + body + ' .';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_triple_pattern'] = function() {
  var subject = Blockly.Rules.valueToCode(this, 'SUBJECT', Blockly.Rules.ORDER_NONE) || Blockly.Rules.RULES_EMTPY_URI;
  var predicate = Blockly.Rules.valueToCode(this, 'PREDICATE', Blockly.Rules.ORDER_NONE) || Blockly.Rules.RULES_EMTPY_URI;
  var object = Blockly.Rules.valueToCode(this, 'OBJECT', Blockly.Rules.ORDER_NONE) || Blockly.Rules.RULES_EMTPY_URI;
  return '(' + subject + ' ' + predicate + ' ' + object + ')';
}

/**
 *
 * @returns {Array}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_uri'] = function() {
  var uri = this.getFieldValue('URI');
  return ['<' + uri + '>', Blockly.Rules.ORDER_ATOMIC];
}

/**
 *
 * @returns {Array}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_qname'] = function() {
  var prefix = this.getFieldValue('PREFIX');
  var local = this.getFieldValue('LOCALNAME');
  return [prefix + ':' + local, Blockly.Rules.ORDER_ATOMIC];
}

/**
 *
 * @returns {Array}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_binding'] = function() {
  var name = this.getFieldValue('VARNAME');
  return ['?' + name, Blockly.Rules.ORDER_ATOMIC];
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_binding_binary_infix'] = function() {
  var a = Blockly.Rules.valueToCode(this, 'A', Blockly.Rules.ORDER_NONE);
  var b = Blockly.Rules.valueToCode(this, 'B', Blockly.Rules.ORDER_NONE);
  var c = Blockly.Rules.valueToCode(this, 'C', Blockly.Rules.ORDER_NONE);
  var op = {
    'add': 'sum',
    'sub': 'difference',
    'mul': 'product',
    'div': 'quotient'
  }[this.getFieldValue('OP')];
  return op + '(' + a + ', ' + b + ', ' + c + ')';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_binding_binary_prefix'] = function() {
  var a = Blockly.Rules.valueToCode(this, 'A', Blockly.Rules.ORDER_NONE);
  var b = Blockly.Rules.valueToCode(this, 'B', Blockly.Rules.ORDER_NONE);
  var c = Blockly.Rules.valueToCode(this, 'C', Blockly.Rules.ORDER_NONE);
  var op = this.getFieldValue('OP');
  return op + '(' + a + ', ' + b + ', ' + c + ')';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_binding_unary_prefix'] = function() {
  var a = Blockly.Rules.valueToCode(this, 'A', Blockly.Rules.ORDER_NONE);
  var c = Blockly.Rules.valueToCode(this, 'C', Blockly.Rules.ORDER_NONE);
  return 'addOne(' + a + ', ' + c + ')';
}

Blockly.Rules['logic_equality_check'] = function() {
  var x = Blockly.Rules.valueToCode(this, 'X', Blockly.Rules.ORDER_NONE);
  var y = Blockly.Rules.valueToCode(this, 'Y', Blockly.Rules.ORDER_NONE);
  var op = this.dropdown.getValue();
  return op + '(' + x + ', ' + y + ')';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_typecheck'] = function() {
  var method = this.getFieldValue('OP');
  var value = Blockly.Rules.valueToCode(this, 'VALUE', Blockly.Rules.ORDER_NONE);
  return method + '(' + value[0] + ')';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_table'] = function() {
  var prop = Blockly.Rules.valueToCode(this, 'PROPERTY', Blockly.Rules.ORDER_NONE);
  return '-> table(' + prop[0] + ') .';
}

/**
 *
 * @returns {string}
 * @this {Blockly.Block}
 */
Blockly.Rules['logic_table_all'] = function() {
  return '-> tableAll() .';
}

Blockly.Rules['logic_rdf_type'] = function() {
  return ['a', Blockly.Rules.ORDER_ATOMIC];
}
