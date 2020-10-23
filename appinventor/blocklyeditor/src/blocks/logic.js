// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2013-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
/**
 * @license
 * @fileoverview Logic blocks for Blockly, modified for MIT App Inventor.
 * @author mckinney@mit.edu (Andrew F. McKinney)
 */

'use strict';

goog.provide('Blockly.Blocks.logic');

goog.require('Blockly.Blocks.Utilities');

Blockly.Blocks['logic_boolean'] = {
  // Boolean data type: true and false.
  category: 'Logic',
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown(this.OPERATORS), 'BOOL');
    var thisBlock = this;
    this.setTooltip(function () {
      var op = thisBlock.getFieldValue('BOOL');
      return Blockly.Blocks.logic_boolean.TOOLTIPS()[op];
    });
  },
  helpUrl: function () {
    var op = this.getFieldValue('BOOL');
    return Blockly.Blocks.logic_boolean.HELPURLS()[op];
  },
  typeblock: [{
    translatedName: Blockly.Msg.LANG_LOGIC_BOOLEAN_TRUE,
    dropDown: {
      titleName: 'BOOL',
      value: 'TRUE'
    }
  }, {
    translatedName: Blockly.Msg.LANG_LOGIC_BOOLEAN_FALSE,
    dropDown: {
      titleName: 'BOOL',
      value: 'FALSE'
    }
  }]
};

Blockly.Blocks.logic_boolean.OPERATORS = function () {
  return [
    [Blockly.Msg.LANG_LOGIC_BOOLEAN_TRUE, 'TRUE'],
    [Blockly.Msg.LANG_LOGIC_BOOLEAN_FALSE, 'FALSE']
  ];
};

Blockly.Blocks.logic_boolean.TOOLTIPS = function () {
  return {
    TRUE: Blockly.Msg.LANG_LOGIC_BOOLEAN_TOOLTIP_TRUE,
    FALSE: Blockly.Msg.LANG_LOGIC_BOOLEAN_TOOLTIP_FALSE
  }
};

Blockly.Blocks.logic_boolean.HELPURLS = function () {
  return {
    TRUE: Blockly.Msg.LANG_LOGIC_BOOLEAN_TRUE_HELPURL,
    FALSE: Blockly.Msg.LANG_LOGIC_BOOLEAN_FALSE_HELPURL
  }
};

Blockly.Blocks['logic_false'] = {
  // Boolean data type: true and false.
  category: 'Logic',
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown(Blockly.Blocks.logic_boolean.OPERATORS), 'BOOL');
    this.setFieldValue('FALSE', 'BOOL');
    var thisBlock = this;
    this.setTooltip(function () {
      var op = thisBlock.getFieldValue('BOOL');
      return Blockly.Blocks.logic_boolean.TOOLTIPS()[op];
    });
  },
  helpUrl: function () {
    var op = this.getFieldValue('BOOL');
    return Blockly.Blocks.logic_boolean.HELPURLS()[op];
  }
};

Blockly.Blocks['logic_negate'] = {
  // Negation.
  category: 'Logic',
  helpUrl: Blockly.Msg.LANG_LOGIC_NEGATE_HELPURL,
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendValueInput('BOOL')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT))
        .appendField(Blockly.Msg.LANG_LOGIC_NEGATE_INPUT_NOT);
    this.setTooltip(Blockly.Msg.LANG_LOGIC_NEGATE_TOOLTIP);
  },
  typeblock: [{translatedName: Blockly.Msg.LANG_LOGIC_NEGATE_INPUT_NOT}]
};

Blockly.Blocks['logic_compare'] = {
  // Comparison operator.
  category: 'Logic',
  helpUrl: function () {
    var mode = this.getFieldValue('OP');
    return Blockly.Blocks.logic_compare.HELPURLS()[mode];
  },
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendValueInput('A');
    this.appendValueInput('B')
        .appendField(new Blockly.FieldDropdown(this.OPERATORS), 'OP');
    this.setInputsInline(true);
    // Assign 'this' to a variable for use in the tooltip closure below.
    var thisBlock = this;
    this.setTooltip(function () {
      var mode = thisBlock.getFieldValue('OP');
      return Blockly.Blocks.logic_compare.TOOLTIPS()[mode];
    });
  },
  // Potential clash with Math =, so using 'logic equal' for now
  typeblock: [{translatedName: Blockly.Msg.LANG_LOGIC_COMPARE_TRANSLATED_NAME}]
};

Blockly.Blocks.logic_compare.TOOLTIPS = function () {
  return {
    EQ: Blockly.Msg.LANG_LOGIC_COMPARE_TOOLTIP_EQ,
    NEQ: Blockly.Msg.LANG_LOGIC_COMPARE_TOOLTIP_NEQ
  }
};

Blockly.Blocks.logic_compare.HELPURLS = function () {
  return {
    EQ: Blockly.Msg.LANG_LOGIC_COMPARE_HELPURL_EQ,
    NEQ: Blockly.Msg.LANG_LOGIC_COMPARE_HELPURL_NEQ
  }
};

Blockly.Blocks.logic_compare.OPERATORS = function () {
  return [
    [Blockly.Msg.LANG_LOGIC_COMPARE_EQ, 'EQ'],
    [Blockly.Msg.LANG_LOGIC_COMPARE_NEQ, 'NEQ']
  ];
};

Blockly.Blocks['logic_operation'] = {
  // Logical operations: 'and', 'or'.
  category: 'Logic',
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendValueInput('A')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT));
    this.appendValueInput('B')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT))
        .appendField(new Blockly.FieldDropdown(this.OPERATORS), 'OP');
    this.setInputsInline(true);
    // Assign 'this' to a variable for use in the tooltip closure below.
    var thisBlock = this;
    this.setTooltip(function () {
      var op = thisBlock.getFieldValue('OP');
      return Blockly.Blocks.logic_operation.TOOLTIPS()[op];
    });
  },
  helpUrl: function () {
    var op = this.getFieldValue('OP');
    return Blockly.Blocks.logic_operation.HELPURLS()[op];
  },
  typeblock: [{
    translatedName: Blockly.Msg.LANG_LOGIC_OPERATION_AND,
    dropDown: {
      titleName: 'OP',
      value: 'AND'
    }
  }, {
    translatedName: Blockly.Msg.LANG_LOGIC_OPERATION_OR,
    dropDown: {
      titleName: 'OP',
      value: 'OR'
    }
  }]
};

Blockly.Blocks.logic_operation.OPERATORS = function () {
  return [
    [Blockly.Msg.LANG_LOGIC_OPERATION_AND, 'AND'],
    [Blockly.Msg.LANG_LOGIC_OPERATION_OR, 'OR']
  ]
};

Blockly.Blocks.logic_operation.HELPURLS = function () {
  return {
    AND: Blockly.Msg.LANG_LOGIC_OPERATION_HELPURL_AND,
    OR: Blockly.Msg.LANG_LOGIC_OPERATION_HELPURL_OR
  }
};
Blockly.Blocks.logic_operation.TOOLTIPS = function () {
  return {
    AND: Blockly.Msg.LANG_LOGIC_OPERATION_TOOLTIP_AND,
    OR: Blockly.Msg.LANG_LOGIC_OPERATION_TOOLTIP_OR
  }
};

Blockly.Blocks['logic_or'] = {
  // Logical operations: 'and', 'or'.
  category: 'Logic',
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendValueInput('A')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT));
    this.appendValueInput('B')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT))
        .appendField(new Blockly.FieldDropdown(Blockly.Blocks.logic_operation.OPERATORS), 'OP');
    this.setFieldValue('OR', 'OP');
    this.setInputsInline(true);
    // Assign 'this' to a variable for use in the tooltip closure below.
    var thisBlock = this;
    this.setTooltip(function () {
      var op = thisBlock.getFieldValue('OP');
      return Blockly.Blocks.logic_operation.TOOLTIPS()[op];
    });
  },
  helpUrl: function () {
    var op = this.getFieldValue('OP');
    return Blockly.Blocks.logic_operation.HELPURLS()[op];
  }
};

goog.require('Blockly.Rules');

Blockly.Blocks['logic_ruleset'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType('text', Blockly.Blocks.Utilities.OUTPUT));
    this.appendDummyInput().appendField('ruleset');
    this.appendStatementInput('RULES')
      .setCheck(['frule', 'brule']);
  }
}

Blockly.Blocks['logic_forward_rule'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'frule');
    this.setNextStatement(true, ['frule', 'brule']);
    this.appendDummyInput().appendField('define forward rule')
//      .appendField(new Blockly.FieldGlobalFlydown('rulename'), 'rulename');
    this.appendStatementInput('ANTECEDENT')
      .appendField('body', ['statement']);
    this.appendDummyInput().appendField('⇒')
    this.appendStatementInput('CONSEQUENT')
      .appendField('head', ['statement', 'brule']);
  }
};

Blockly.Blocks['logic_backward_rule'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'brule');
    this.setNextStatement(true, ['frule', 'brule']);
    this.appendDummyInput()
      .appendField('define backward rule')
//      .appendField(new Blockly.FieldGlobalFlydown('rulename'), 'rulename');
    this.appendStatementInput('CONSEQUENT')
      .appendField('head')
      .setCheck('statement');
    this.appendDummyInput().appendField('⇐');
    this.appendStatementInput('ANTECEDENT')
      .appendField('body')
      .setCheck('statement');
  }
};

Blockly.Blocks['logic_triple_pattern'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendValueInput('SUBJECT').appendField('subject')
      .setCheck(['qname', 'variable']);
    this.appendValueInput('PREDICATE').appendField('predicate')
      .setCheck(['qname', 'variable']);
    this.appendValueInput('OBJECT').appendField('object')
      .setCheck(['qname', 'variable']);
    this.setInputsInline(true);
    var type = this.workspace.newBlock('logic_rdf_type');
    type.setShadow(true);
    this.getInput('PREDICATE').connection.connect(type.outputConnection);
  }
}

Blockly.Blocks['logic_uri'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);
    this.appendDummyInput().appendField('<')
      .appendField(new Blockly.FieldTextInput(''), 'URI')
      .appendField('>');
  }
}

Blockly.Blocks['logic_qname'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);
    this.appendDummyInput().appendField(new Blockly.FieldTextInput(''), 'PREFIX')
      .appendField(':').appendField(new Blockly.FieldTextInput(''), 'LOCALNAME');
  }
}

Blockly.Blocks['logic_binding'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['variable']);
    this.appendDummyInput().appendField('?')
      .appendField(new Blockly.FieldTextInput('var'), 'VARNAME');
  }
}

Blockly.Blocks['logic_binding_binary_infix'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.appendValueInput('C').appendField('let')
      .setCheck('variable');
    this.appendValueInput('A')
      .appendField(':=')
      .setCheck(['qname', 'variable']);
    this.appendValueInput('B')
      .appendField(new Blockly.FieldDropdown([['+', 'add'], ['-', 'sub'], ['✖️', 'mul'], ['➗', 'div']]), 'OP');
    this.setInputsInline(true);
  }
}

Blockly.Blocks['logic_binding_binary_prefix'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendValueInput('C').appendField('let');
    this.appendValueInput('A')
      .appendField(':=')
      .appendField(new Blockly.FieldDropdown([['min', 'min'], ['max', 'max']]), 'OP');
    this.appendValueInput('B');
    this.setInputsInline(true);
  }
}

Blockly.Blocks['logic_binding_unary_prefix'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendValueInput('C').appendField('let');
    this.appendValueInput('A')
      .appendField(':= 1 + ');
    this.setInputsInline(true);
  }
}

Blockly.Blocks['logic_equality_check'] = {
  category: 'Logic',
  init: function() {
    this.dropdown = new Blockly.FieldDropdown([
      ['=', 'equal'],
      ['≠', 'notEqual'],
      ['<', 'lessThan'],
      ['≤', 'le'],
      ['>', 'greaterThan'],
      ['≥', 'ge']
    ]);
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendValueInput('X');
    this.appendValueInput('Y').appendField(this.dropdown, 'OP');
    this.setInputsInline(true);
  }
}

Blockly.Blocks['logic_typecheck'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendValueInput('VALUE')
      .appendField(new Blockly.FieldDropdown([
        ['isLiteral', 'isLiteral'], ['isFunctor', 'isFunctor'], ['isBnode', 'isBnode'],
        ['bound', 'bound']
      ]), 'OP');
    this.setInputsInline(true);
  }
}

Blockly.Blocks['logic_table'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendValueInput('PROPERTY').appendField('table');
  }
}

Blockly.Blocks['logic_table_all'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, 'statement');
    this.setNextStatement(true, 'statement');
    this.appendDummyInput().appendField('table all');
  }
}

Blockly.Blocks['logic_rdf_type'] = {
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);
    this.appendDummyInput().appendField('a');
  }
}

Blockly.Blocks['logic_namespace_decl'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.appendValueInput('URI').appendField('define namespace')
      .appendField(new Blockly.FieldTextInput('prefix'), 'PREFIX')
      .appendField('as')
      .setCheck(['qname']);
  }
}
