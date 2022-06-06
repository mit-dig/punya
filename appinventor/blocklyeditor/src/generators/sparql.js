// -*- mode: javascript; js-indent-offset: 2; -*-
// Copyright © 2020 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

'use strict';

goog.provide('Blockly.SPARQL');

goog.require('Blockly.Generator');

Blockly.SPARQL = new Blockly.Generator('SPARQL');
Blockly.SPARQL.ORDER_ATOMIC = 0;
Blockly.SPARQL.ORDER_FUNCTION_CALL = 0;
Blockly.SPARQL.ORDER_PARENS = 1;
Blockly.SPARQL.ORDER_UNARY = 2;
Blockly.SPARQL.ORDER_MULTIPLY = 3;
Blockly.SPARQL.ORDER_ADDITION = 4;
Blockly.SPARQL.ORDER_RELATIONAL = 5;
Blockly.SPARQL.ORDER_AND = 6;
Blockly.SPARQL.ORDER_OR = 7;
Blockly.SPARQL.ORDER_NONE = 99;
Blockly.SPARQL.INDENT = '';

Blockly.SPARQL.scrub_ = function(block, code) {
  var next = block.getNextBlock();
  if (next) {
    code = code + '\n' + Blockly.SPARQL.blockToCode(next);
  }
  return code;
}

Blockly.SPARQL.blockToCode1 = function(block) {
  this.yailBlocks = 0;
  return this.blockToCode(block);
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['text'] = function() {
  var text = this.getFieldValue('TEXT');
  text = text.replace(/"/, '\\"');
  text = text.replace(/\n/, '\\n');
  return ['"' + text + '"', Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['math_number'] = function() {
  return [this.getFieldValue('NUM'), Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_boolean'] = function() {
  return [this.getFieldValue('BOOL').toLowerCase(), Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_false'] = function() {
  return Blockly.SPARQL['logic_boolean'].call(this);
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_select'] = function() {
  var code = '';
  var allBlocks= this.workspace.getTopBlocks(true);
  allBlocks.forEach(function (block) {
    if (block.type === 'logic_namespace_decl') {
      code += Blockly.SPARQL.blockToCode(block);
      code += '\n';
    }
  });
  code += 'SELECT ';
  if (this.getFieldValue('DISTINCT') === 'true') {
    code += 'DISTINCT ';
  }
  code += Blockly.SPARQL.statementToCode(this, 'VARS').replace(/\r?\n/g, " ") || '*';
  code += ' WHERE {\n';
  code += Blockly.SPARQL.statementToCode(this, 'WHERE');
  code += '\n}';
  var modifiers = Blockly.SPARQL.statementToCode(this, 'MODIFIERS');
  if (modifiers) {
    code += ' ' + modifiers;
  }
  return code;
}

Blockly.SPARQL['logic_sparql_star'] = function() {
  return '*';
}

/**
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_aggregate'] = function() {
  var op = this.getFieldValue('OP');
  var distinct = this.getFieldValue('DISTINCT') === 'true';
  var code = '(' + op;
  code += distinct ? '(DISTINCT' : '(';
  code += Blockly.SPARQL.valueToCode(this, 'VALUE',
    Blockly.SPARQL.ORDER_FUNCTION_CALL);
  code += ') AS ?';
  code += this.getFieldValue('NAME');
  return code + ')';
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_namespace_decl'] = function() {
  var code = 'PREFIX ';
  code += this.getFieldValue('PREFIX');
  code += ': ';
  code += Blockly.SPARQL.valueToCode(this, 'URI', Blockly.SPARQL.ORDER_NONE);
  code += ' ';
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_optional'] = function() {
  var code = 'OPTIONAL {\n';
  code += Blockly.SPARQL.statementToCode(this, 'CLAUSES');
  code += '}';
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_union'] = function() {
  var code = '{\n';
  var separator = '';
  for (var i = 0; i < this.itemCount_; i++) {
    code += separator;
    code += Blockly.SPARQL.statementToCode(this, this.repeatingInputName + i);
    separator = '\n} UNION {\n';
  }
  code += '\n}';
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_operator'] = function() {
  var code = '';
  code += this.getFieldValue('OPERATOR');
  code += '(';
  code += Blockly.SPARQL.valueToCode(this, 'OPERAND', Blockly.SPARQL.ORDER_NONE) || 'true';
  code += ')';
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_offset'] = function() {
  var code = 'OFFSET ';
  var offset = Blockly.SPARQL.valueToCode(this, 'VALUE', Blockly.SPARQL.ORDER_ATOMIC);
  if (offset) {
    code += offset[0];
  } else {
    code += '0';
  }
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_limit'] = function() {
  var code = 'LIMIT ';
  var limit = Blockly.SPARQL.valueToCode(this, 'VALUE', Blockly.SPARQL.ORDER_ATOMIC);
  if (limit) {
    code += limit[0];
  } else {
    code += '100';
  }
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_groupby'] = function() {
  var code = 'GROUP BY ';
  code += Blockly.SPARQL.valueToCode(this, 'VALUE', Blockly.SPARQL.ORDER_FUNCTION_CALL);
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_having'] = function() {
  var code = 'HAVING ';
  code += Blockly.SPARQL.valueToCode(this, 'VALUE', Blockly.SPARQL.ORDER_FUNCTION_CALL);
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_orderby'] = function() {
  var code = 'ORDER BY ' + this.getFieldValue('DIR') + ' ';
  var field = Blockly.SPARQL.valueToCode(this, 'VALUE', Blockly.SPARQL.ORDER_NONE);
  if (field) {
    code += field;
  } else {
    return '';
  }
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_sparql_uri'] = function() {
  var parent = this.getParent();
  var code = '';
  if (!parent || parent.type !== 'logic_triple_pattern') {
    return ['as']
  } else {

  }
  return [code, Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_var'] = function() {
  return '?' + this.getFieldValue('VARNAME');
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_sparql_graph'] = function() {
  var code = 'GRAPH ';
  var variable = Blockly.SPARQL.valueToCode(this, 'GRAPH', Blockly.SPARQL.ORDER_NONE);
  if (variable) {
    code += variable[0];
  } else {
    code += '<>';
  }
  code += ' {\n';
  code += Blockly.SPARQL.statementToCode(this, 'PATTERN');
  code += '\n}';
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_sparql_builtin_unary'] = function() {
  var a0 = Blockly.SPARQL.valueToCode(this, 'ARG0', Blockly.SPARQL.ORDER_NONE);
  var op = this.getFieldValue('OPERATOR');

  var code = op + "(" + a0 + ")";

  return [code, Blockly.SPARQL.ORDER_FUNCTION_CALL];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_sparql_builtin_binary'] = function() {
  var a0 = Blockly.SPARQL.valueToCode(this, 'ARG0', Blockly.SPARQL.ORDER_NONE);
  var a1 = Blockly.SPARQL.valueToCode(this, 'ARG1', Blockly.SPARQL.ORDER_NONE);
  var op = this.getFieldValue('OPERATOR');

  var code = op + "(" + a0 + "," + a1 + ")";

  return [code, Blockly.SPARQL.ORDER_FUNCTION_CALL];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_sparql_builtin_regex'] = function() {
  var code = 'REGEX(';
  code += Blockly.SPARQL.valueToCode(this, 'TEXT', Blockly.SPARQL.ORDER_FUNCTION_CALL);
  code += ', ';
  code += Blockly.SPARQL.valueToCode(this, 'REGEX', Blockly.SPARQL.ORDER_FUNCTION_CALL);
  code += ', ';
  code += Blockly.SPARQL.valueToCode(this, 'FLAGS', Blockly.SPARQL.ORDER_FUNCTION_CALL);
  code += ')';
  return [code, Blockly.SPARQL.ORDER_FUNCTION_CALL];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_operation'] = function() {
  var op = this.getFieldValue('OP') === 'AND' ? '&&' : '||';
  var identity = this.getFieldValue('OP') === 'AND' ? 'true' : 'false';
  var order = this.getFieldValue('OP') === 'AND' ?
    Blockly.SPARQL.ORDER_AND : Blockly.SPARQL.ORDER_OR;
  var arg0 = Blockly.SPARQL.valueToCode(this, 'A', order);
  var arg1 = Blockly.SPARQL.valueToCode(this, 'B', order);
  var code = '';
  if (arg0) {
    code += arg0[0];
  } else {
    code += identity;
  }
  code += ' ' + op + ' ';
  if (arg1) {
    code += arg1[0];
  } else {
    code += identity;
  }
  return code;
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_or'] = function() {
  var arg0 = Blockly.SPARQL.valueToCode(this, 'A', Blockly.SPARQL.ORDER_OR);
  var arg1 = Blockly.SPARQL.valueToCode(this, 'B', Blockly.SPARQL.ORDER_OR);
  var code = '';
  if (arg0) {
    code += arg0[0];
  } else {
    code += 'false';
  }
  code += ' || ';
  if (arg1) {
    code += arg1[0];
  } else {
    code += 'false';
  }
  return [code, Blockly.SPARQL.ORDER_OR];
}

/**
 * @this Blockly.BlockSvg
 * @returns {string}
 */
Blockly.SPARQL['logic_triple_pattern'] = function() {
  var subject = Blockly.SPARQL.valueToCode(this, 'SUBJECT', Blockly.SPARQL.ORDER_NONE) || ['<>'];
  var predicate = Blockly.SPARQL.valueToCode(this, 'PREDICATE', Blockly.SPARQL.ORDER_NONE) || ['a'];
  var object = Blockly.SPARQL.valueToCode(this, 'OBJECT', Blockly.SPARQL.ORDER_NONE) || ['<>'];
  return subject + ' ' + predicate + ' ' + object + ' .';
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_rdf_type'] = function() {
  return ['a', Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['math_compare'] = function() {
  var op = this.getFieldValue('OP') || 'EQ';
  var arg0 = Blockly.SPARQL.valueToCode(this, 'A', Blockly.SPARQL.ORDER_RELATIONAL) || '0';
  var arg1 = Blockly.SPARQL.valueToCode(this, 'B', Blockly.SPARQL.ORDER_RELATIONAL) || '0';
  return [arg0 + ' ' + Blockly.SPARQL['math_compare'].OPERATORS[op] + ' ' + arg1,
    Blockly.SPARQL.ORDER_RELATIONAL];
}

Blockly.SPARQL['math_compare'].OPERATORS = {
  'EQ': '=',
  'NEQ': '!=',
  'LT': '<',
  'LTE': '<=',
  'GT': '>',
  'GTE': '>='
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['math_add'] = function() {
  var code = '';
  var separator = '';
  for (var i = 0; i < this.itemCount_; i++) {
    code += separator;
    var arg = Blockly.SPARQL.valueToCode(this, 'NUM' + i, Blockly.SPARQL.ORDER_ADDITION) || '0';
    code += arg;
    separator = ' + ';
  }
  return [code, Blockly.SPARQL.ORDER_ADDITION];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['math_subtract'] = function() {
  var arg0 = Blockly.SPARQL.valueToCode(this, 'A', Blockly.SPARQL.ORDER_ADDITION);
  var arg1 = Blockly.SPARQL.valueToCode(this, 'B', Blockly.SPARQL.ORDER_ADDITION);
  return [arg0 + ' - ' + arg1, Blockly.SPARQL.ORDER_ADDITION];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['math_multiply'] = function() {
  var code = '';
  var separator = '';
  for (var i = 0; i < this.itemCount_; i++) {
    code += separator;
    var arg = Blockly.SPARQL.valueToCode(this, 'NUM' + i, Blockly.SPARQL.ORDER_MULTIPLY) || '1';
    code += arg;
    separator = ' * ';
  }
  return [code, Blockly.SPARQL.ORDER_MULTIPLY];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['math_divide'] = function() {
  var arg0 = Blockly.SPARQL.valueToCode(this, 'A', Blockly.SPARQL.ORDER_MULTIPLY) || '1';
  var arg1 = Blockly.SPARQL.valueToCode(this, 'B', Blockly.SPARQL.ORDER_MULTIPLY) || '1';
  return [arg0 + ' / '+ arg1, Blockly.SPARQL.ORDER_MULTIPLY];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_compare'] = function() {
  return Blockly.SPARQL['math_compare'].call(this);
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_binding'] = function() {
  return ['?' + this.getFieldValue('VARNAME'), Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_uri'] = function() {
  return ['<' + this.getFieldValue('URI') + '>', Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_qname'] = function() {
  return ['<' + this.getFieldValue('NAMESPACE') + this.getFieldValue('LOCALNAME') + '>',
     Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_qname_select'] = function() {
  return ['<' + this.getFieldValue('URI') + '>',
     Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['logic_negate'] = function() {
  var code = '!';
  var arg = Blockly.SPARQL.valueToCode(this, 'BOOL', Blockly.SPARQL.ORDER_UNARY);
  return [code + arg, Blockly.SPARQL.ORDER_UNARY];
}

/**
 * Convert component getters into SPARQL.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['component_set_get'] = function() {
  Blockly.SPARQL.yailBlocks++;
  var code = '```yail(sparql-quote ';
  code += Blockly.Yail.blockToCode(this)[0];
  code += ')```';
  return [code, Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 * Convert component blocks to their default value.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['component_component_block'] = function() {
  Blockly.SPARQL.yailBlocks++;
  var code = '```yail';
  code += Blockly.Yail.YAIL_GET_PROPERTY;
  code += Blockly.Yail.YAIL_QUOTE;
  code += this.getFieldValue('COMPONENT_SELECTOR');
  code += Blockly.Yail.YAIL_SPACER;
  code += Blockly.Yail.YAIL_QUOTE;
  code += 'Value';
  code += Blockly.Yail.YAIL_CLOSE_COMBINATION;
  code += '```';
  return [code, Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 * Convert method component calls into SPARQL.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['component_method'] = function() {
  Blockly.SPARQL.yailBlocks++;
  var code = '```yail(sparql-quote ';
  code += Blockly.Yail.blockToCode(this)[0];
  code += ')```';
  return [code, Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 * Convert procedure call results into SPARQL.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['procedures_callreturn'] = function() {
  Blockly.SPARQL.yailBlocks++;
  var code = '```yail';
  code += Blockly.Yail.blockToCode(this)[0];
  code += '```';
  return [code, Blockly.SPARQL.ORDER_ATOMIC];
}

/**
 * Convert variable getters into SPARQL.
 *
 * @this Blockly.BlockSvg
 * @returns {[string, number]}
 */
Blockly.SPARQL['lexical_variable_get'] = function() {
  Blockly.SPARQL.yailBlocks++;
  var code = '```yail';
  code += Blockly.Yail.blockToCode(this)[0];
  code += '```';
  return [code, Blockly.SPARQL.ORDER_ATOMIC];
}
