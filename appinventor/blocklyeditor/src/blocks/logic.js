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
      .setCheck(['qname', 'variable', 'bnode']);
    this.appendValueInput('PREDICATE').appendField('predicate')
      .setCheck(['qname', 'variable', 'bnode']);
    this.appendValueInput('OBJECT').appendField('object')
      .setCheck(['qname', 'variable', 'bnode', 'Number', 'String', 'Boolean']);
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
  /**
   * @this Blockly.BlockSvg
   */
  init: function() {
    var workspace = this.workspace;
    this.dropDown = new Blockly.FieldDropdown(function() {
      var topBlocks = workspace.getTopBlocks(false);
      var items = [];
      for (var i = 0; i < topBlocks.length; i++) {
        if (topBlocks[i].type === 'logic_namespace_decl') {
          var prefix = topBlocks[i].getFieldValue('PREFIX');
          items.push([prefix, prefix]);
        }
      }
      if (items.length === 0) {
        items.push(['', '']);
      }
      return items;
    });
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);
    this.appendDummyInput().appendField(this.dropDown, 'PREFIX')
      .appendField(':').appendField(new Blockly.FieldTextInput(''), 'LOCALNAME');
  }
}

Blockly.Blocks['logic_bnode'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(['bnode']);
    this.appendDummyInput().appendField('Blank Node')
      .appendField(new Blockly.FieldTextInput(''));
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

Blockly.Blocks['logic_sparql_select'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['String']);
    var star = this.workspace.newBlock('logic_sparql_star');
    star.setShadow(true);
    this.appendDummyInput().appendField().appendField('SELECT')
      .appendField(new Blockly.FieldCheckbox('false'), 'DISTINCT')
      .appendField('DISTINCT');
    this.appendStatementInput('VARS')
      .setCheck('varlist');
    this.appendDummyInput().appendField('WHERE');
    this.appendStatementInput('WHERE')
      .setCheck(['statement']);
    this.appendDummyInput().appendField('MODIFIERS');
    this.appendStatementInput('MODIFIERS')
      .setCheck(['modifier']);
    this.getInput('VARS').connection.connect(star.previousConnection);
  }
};

Blockly.Blocks['logic_sparql_star'] = {
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['varlist', 'variable']);
    this.setNextStatement(true, ['varlist', 'variable']);
    this.appendDummyInput().appendField('<all variables>');
  }
};

Blockly.Blocks['logic_sparql_optional'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['statement']);
    this.setNextStatement(true, ['statement'])
    this.appendDummyInput().appendField('OPTIONAL');
    this.appendStatementInput('CLAUSES')
      .setCheck(['statement']);
    this.itemCount_ = 2;
  }
}

Blockly.Blocks['logic_sparql_union'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['statement']);
    this.setNextStatement(true, ['statement']);
    this.setMutator(new Blockly.Mutator(['logic_graph_item']));
    this.addInput(0);
    this.addInput(1);
    this.itemCount_ = 2;
  },
  mutationToDom: Blockly.mutationToDom,
  domToMutation: Blockly.domToMutation,
  decompose: function(workspace) {
    var containerBlock = Blockly.decompose(workspace, 'logic_graph_item', this);
    containerBlock.setFieldValue('UNION', 'CONTAINER_TEXT');
    return containerBlock;
  },
  compose: Blockly.compose,
  saveConnections: Blockly.saveConnections,
  emptyInputName: 'EMPTY',
  repeatingInputName: 'GRAPH',
  removeInput: function(name) {
    if (name.indexOf(this.repeatingInputName) === 0) {
      var index = parseInt(name.substr(this.repeatingInputName.length));
      if (index) {
        Blockly.BlockSvg.prototype.removeInput.call(this, 'UNION' + index);
      }
    }
    Blockly.BlockSvg.prototype.removeInput.call(this, name);
  },
  addInput: function(inputNum) {
    if (inputNum) {
      this.appendDummyInput('UNION' + inputNum).appendField('UNION');
    }
    return this.appendStatementInput(this.repeatingInputName + inputNum).setCheck(['statement']);
  },
  addEmptyInput: function() {
    this.appendDummyInput(this.emptyInputName).appendField('EMPTY UNION');
  }
}

Blockly.Blocks['logic_graph_item'] = {
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.appendDummyInput().appendField('subgraph');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.contextMenu = false;
  }
}

Blockly.Blocks['logic_sparql_operator'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['statement']);
    this.setNextStatement(true, ['statement']);
    this.appendValueInput('OPERAND')
      .appendField(new Blockly.FieldDropdown([['Filter', 'FILTER']]), 'OPERATOR')
      .setCheck(null);
  }
}

Blockly.Blocks['logic_sparql_offset'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['modifier']);
    this.setNextStatement(true, ['modifier']);
    this.appendValueInput('VALUE').appendField('OFFSET')
      .setCheck(['number']);
  }
}

Blockly.Blocks['logic_sparql_limit'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['modifier']);
    this.setNextStatement(true, ['modifier']);
    this.appendValueInput('VALUE').appendField('LIMIT')
      .setCheck(['number']);
  }
}

Blockly.Blocks['logic_sparql_groupby'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['modifier']);
    this.setNextStatement(true, ['modifier']);
    this.appendValueInput('VALUE').appendField('GROUP BY')
      .setCheck(['variable']);
  }
}

Blockly.Blocks['logic_sparql_orderby'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['modifier']);
    this.setNextStatement(true, ['modifier']);
    this.appendValueInput('VALUE').appendField('ORDER BY')
      .appendField(new Blockly.FieldDropdown([['ASC', 'ASC'], ['DESC', 'DESC']]), 'DIR')
      .setCheck(['variable']);
  }
}

Blockly.Blocks['logic_sparql_uri'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);
    this.appendValueInput('URI').appendField('as uri')
      .setCheck(['text']);
  }
}

Blockly.Blocks['logic_sparql_var'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['varlist']);
    this.setNextStatement(true, ['varlist']);
    this.appendDummyInput().appendField('?')
      .appendField(new Blockly.FieldTextInput('var'), 'VARNAME');
  }
}

Blockly.Blocks['logic_sparql_graph'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setPreviousStatement(true, ['statement']);
    this.setNextStatement(true, ['statement']);
    this.appendValueInput('GRAPH').appendField('GRAPH')
      .setCheck(['qname', 'variable']);
    this.appendStatementInput('PATTERN')
      .setCheck(['statement']);
  }
}

Blockly.Blocks['logic_sparql_builtin_unary'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['boolean']);
    this.appendValueInput('ARG0').appendField(new Blockly.FieldDropdown(
      [['as string', 'STR'], ['language of', 'LANG'], ['datatype of', 'DATATYPE'],
      ['is bound?', 'BOUND'], ['is IRI?', 'isIRI'], ['is URI?', 'isURI'], ['is blank node?', 'isBLANK'],
      ['is literal?', 'isLITERAL']]
    ), 'OPERATOR');
  }
}

Blockly.Blocks['logic_sparql_builtin_binary'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['boolean']);
    this.appendValueInput('ARG0').appendField(new Blockly.FieldDropdown(
      [['language matches?', 'LANGMATCHES'], ['same term?', 'sameTerm']]
    ), 'OPERATOR');
    this.appendValueInput('ARG1');
    this.setInputsInline(true);
  }
}

Blockly.Blocks['logic_sparql_builtin_regex'] = {
  category: 'Logic',
  init: function() {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['boolean']);
    this.appendValueInput('TEXT').appendField('match string').setCheck(['String', 'qname', 'variable']);
    this.appendValueInput('REGEX').appendField('to regex').setCheck(['String', 'qname', 'variable']);
    this.appendValueInput('FLAGS').appendField('flags').setCheck('String');
    var text = this.workspace.newBlock('text');
    text.setShadow(true);
    this.getInput('FLAGS').connection.connect(text.outputConnection);
  }
}
