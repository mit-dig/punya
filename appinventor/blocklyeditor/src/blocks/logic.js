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

goog.require('Blockly.Mutator');
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
  init: function (op) {
    op = op || 'AND';
    // Assign 'this' to a variable for use in the tooltip closure below.
    var thisBlock = this;
    this.opField = new Blockly.FieldDropdown(
      Blockly.Blocks.logic_operation.OPERATORS, function(op) {
        return thisBlock.updateFields(op);
      });
    /**
     * Reference to the last mutator workspace so we can update the container block's label when
     * the dropdown value changes.
     *
     * @type {Blockly.WorkspaceSvg}
     */
    this.lastMutator = null;
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.OUTPUT));
    this.appendValueInput('A')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT));
    this.appendValueInput('B')
        .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT))
        .appendField(this.opField, 'OP');
    this.setFieldValue(op, 'OP');
    this.setInputsInline(true);
    this.setTooltip(function () {
      return Blockly.Blocks.logic_operation.TOOLTIPS()[thisBlock.getFieldValue('OP')];
    });
    this.setMutator(new Blockly.Mutator(['logic_mutator_item']));
    this.emptyInputName = 'EMPTY';
    this.repeatingInputName = 'BOOL';
    this.itemCount_ = 2;
    this.valuesToSave = {'op': op};
  },
  mutationToDom: Blockly.mutationToDom,
  domToMutation: function(container) {
    if (this.valuesToSave != null) {
      for (var name in this.valuesToSave) {
        this.valuesToSave[name] = this.getFieldValue(name);
      }
    }

    if (this.itemCount_ === 0) {
      this.removeInput(this.emptyInputName, true);
    }
    if (this.itemCount_ > 0) {
      this.removeInput('A', true);
    }
    if (this.itemCount_ > 1) {
      this.removeInput('B', true);
    }
    for (var x = 2; x < this.itemCount_; x++) {
      this.removeInput(this.repeatingInputName + x, true);
    }
    this.itemCount_ = window.parseInt(container.getAttribute('items'), 10);
    for (var x = 0; x < this.itemCount_; x++) {
      this.addInput(x);
    }
    if (this.itemCount_ === 0) {
      this.addEmptyInput();
    }
    // NOTE(ewp): Blockly doesn't trigger the validation function when the field is set during
    // load, so we override setValue here to make sure that the additional and/or labels (if
    // present) match the dropdown's value.
    // TODO: BeksOmega says that this can be removed once we update Blockly.
    var oldSetValue = this.opField.setValue;
    var thisBlock = this;
    this.opField.setValue = function(newValue) {
      oldSetValue.call(this, newValue);
      thisBlock.updateFields(newValue);
    };
  },
  decompose: function(workspace) {
    var containerBlockName = 'mutator_container';
    var containerBlock = workspace.newBlock(containerBlockName);
    containerBlock.setColour(this.getColour());
    containerBlock.setFieldValue(this.opField.getText(), 'CONTAINER_TEXT');
    containerBlock.initSvg();
    var connection = containerBlock.getInput('STACK').connection;
    for (var x = 0; x < this.itemCount_; x++) {
      var itemBlock = workspace.newBlock('logic_mutator_item');
      itemBlock.initSvg();
      connection.connect(itemBlock.previousConnection);
      connection = itemBlock.nextConnection;
    }
    this.lastMutator = workspace;
    return containerBlock;
  },
  countNumberOfInputs: function(containerBlock) {
    var start = containerBlock.getInputTargetBlock('STACK');
    var i = 0;
    while (start) {
      i++;
      start = start.getNextBlock();
    }
    return i;
  },
  compose: function(containerBlock) {
    if (this.valuesToSave != null) {
      for (var name in this.valuesToSave) {
        this.valuesToSave[name] = this.getFieldValue(name);
      }
    }
    if (this.itemCount_ === 0) {
      this.removeInput(this.emptyInputName, true);
    }
    // Disconnect all input blocks and destroy all inputs.
    for (var x = this.itemCount_ - 1; x >= 0; x--) {
      this.removeInput(x > 1 ? this.repeatingInputName + x : ['A', 'B'][x], true);
    }
    // Rebuild the block's inputs.
    var itemBlock = containerBlock.getInputTargetBlock('STACK');
    this.itemCount_ = this.countNumberOfInputs(containerBlock);
    var i = 0;
    while (itemBlock) {

      var input = this.addInput(i);

      // Reconnect any child blocks.
      if (itemBlock.valueConnection_) {
        input.connection.connect(itemBlock.valueConnection_);
      }
      i++;
      itemBlock = itemBlock.nextConnection &&
        itemBlock.nextConnection.targetBlock();
    }
    if (this.itemCount_ === 0) {
      this.addEmptyInput();
    }
  },
  saveConnections: function(containerBlock) {
    // Store a pointer to any connected child blocks.
    var itemBlock = containerBlock.getInputTargetBlock('STACK');
    var x = 0;
    while (itemBlock) {
      var input = this.getInput(x > 1 ? this.repeatingInputName + x : ['A', 'B'][x]);
      itemBlock.valueConnection_ = input && input.connection.targetConnection;
      x++;
      itemBlock = itemBlock.nextConnection && itemBlock.nextConnection.targetBlock();
    }
  },
  addInput: function (inputNum) {
    var name = inputNum > 1 ? this.repeatingInputName + inputNum : ['A', 'B'][inputNum];
    var input = this.appendValueInput(name)
      .setCheck(Blockly.Blocks.Utilities.YailTypeToBlocklyType("boolean", Blockly.Blocks.Utilities.INPUT));
    if (this.getInputsInline()) {
      if (inputNum === 1) {
        this.makeDropdown(input);
      } else if (inputNum > 1) {
        var field = new Blockly.FieldLabel(this.opField.getText());
        input.appendField(field);
        field.init();
      } else if (this.itemCount_ === 1) {
        input.appendField(Blockly.Blocks.logic_operation.IDENTITY(this.opField.getValue()),
          'IDENTITY');
        this.makeDropdown(input);
      }
    } else if (inputNum === 0) {
      this.makeDropdown(input);
    }
    return input;
  },
  addEmptyInput: function() {
    this.makeDropdown(this.appendDummyInput(this.emptyInputName));
  },
  makeDropdown: function(input) {
    var op = this.opField.getValue();
    // Dispose of the old field first (issue #2266)
    this.opField.dispose();
    this.opField = new Blockly.FieldDropdown(
      Blockly.Blocks.logic_operation.OPERATORS(),
      this.updateFields.bind(this));
    this.opField.setValue(op);
    input.appendField(this.opField, 'OP');
    this.opField.init();
    return input;
  },
  helpUrl: function () {
    var op = this.getFieldValue('OP');
    return Blockly.Blocks.logic_operation.HELPURLS()[op];
  },
  setInputsInline: function(inline) {
    if (inline) {
      var ainput = this.getInput('A');
      if (ainput && ainput.fieldRow.length > 0) {
        ainput.fieldRow.splice(0, 1);
        var binput = this.getInput('B');
        this.makeDropdown(binput);
      }
      for (var input, i = 2; (input = this.inputList[i]); i++) {
        var field = new Blockly.FieldLabel(this.opField.getText());
        input.appendField(field);
        field.init();
      }
    } else {
      var binput = this.getInput('B');
      if (binput && binput.fieldRow.length > 0) {
        binput.fieldRow.splice(0, 1);
        var ainput = this.getInput('A');
        this.makeDropdown(ainput);
      }
      for (var input, i = 2; (input = this.inputList[i]); i++) {
        input.fieldRow[0].dispose();
        input.fieldRow.splice(0, 1);
      }
    }
    Blockly.BlockSvg.prototype.setInputsInline.call(this, inline);
  },
  updateFields: function(op) {
    if (this.getInputsInline()) {
      var text = op === 'AND' ? Blockly.Msg.LANG_LOGIC_OPERATION_AND :
        Blockly.Msg.LANG_LOGIC_OPERATION_OR;
      for (var input, i = 2; (input = this.inputList[i]); i++) {
        input.fieldRow[0].setText(text);
      }
    }
    if (this.itemCount_ === 1) {
      var identity = this.getField('IDENTITY');
      if (identity) {
        identity.setText(Blockly.Blocks.logic_operation.IDENTITY(op));
      }
    }
    // Update the mutator container block if the mutator is open
    if (this.lastMutator) {
      var mutatorBlock = this.lastMutator.getTopBlocks(false)[0];
      if (mutatorBlock) {
        var title = op === 'AND' ? Blockly.Msg.LANG_LOGIC_OPERATION_AND :
          Blockly.Msg.LANG_LOGIC_OPERATION_OR;
        mutatorBlock.setFieldValue(title, 'CONTAINER_TEXT');
      }
    }
    return op;
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

Blockly.Blocks.logic_operation.IDENTITY = function(op) {
  return {'AND': Blockly.Msg.LANG_LOGIC_BOOLEAN_TRUE,
    'OR': Blockly.Msg.LANG_LOGIC_BOOLEAN_FALSE}[op];
}

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
    Blockly.Blocks['logic_operation'].init.call(this, 'OR');
  },
  mutationToDom: Blockly.Blocks['logic_operation'].mutationToDom,
  domToMutation: Blockly.Blocks['logic_operation'].domToMutation,
  decompose: Blockly.Blocks['logic_operation'].decompose,
  countNumberOfInputs: Blockly.Blocks['logic_operation'].countNumberOfInputs,
  compose: Blockly.Blocks['logic_operation'].compose,
  saveConnections: Blockly.Blocks['logic_operation'].saveConnections,
  addInput: Blockly.Blocks['logic_operation'].addInput,
  addEmptyInput: Blockly.Blocks['logic_operation'].addEmptyInput,
  makeDropdown: Blockly.Blocks['logic_operation'].makeDropdown,
  helpUrl: Blockly.Blocks['logic_operation'].helpUrl,
  setInputsInline: Blockly.Blocks['logic_operation'].setInputsInline,
  updateFields: Blockly.Blocks['logic_operation'].updateFields
};

Blockly.Blocks['logic_mutator_item'] = {
  // Add items.
  init: function () {
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.appendDummyInput().appendField("boolean");
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.contextMenu = false;
  }
};

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

function getDeclaredNamespaces(workspace) {
    var topBlocks = workspace.getTopBlocks(false);
    var items = [];
    for (var i = 0; i < topBlocks.length; i++) {
        if (topBlocks[i].type === 'logic_namespace_decl') {
            var block = topBlocks[i];
            if (block.childBlocks_.length > 0) {
              var prefix = block.getFieldValue('PREFIX');
              var uriBlock = block.childBlocks_[0];
              var uri = uriBlock.getFieldValue('URI');
              items.push([prefix, uri]);
            }
        }
    }
    if (items.length === 0) {
        items.push(['', '']);
    }
    return items;
}

Blockly.Blocks['logic_qname'] = {
  category: 'Logic',
  /**
   * @this Blockly.BlockSvg
   */
  init: function() {
    var workspace = this.workspace;
    var dropDown = new Blockly.FieldDropdown(function() {
        return getDeclaredNamespaces(workspace);
    });
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);
    this.appendDummyInput().appendField(dropDown, 'NAMESPACE')
      .appendField(':').appendField(new Blockly.FieldTextInput(''), 'LOCALNAME');
  }
}

var NonRepeatingAlert = {
  last: -1,

  alert: function(msg) {
    var now = new Date().getTime();
    if (this.last == -1 || (now - this.last) >= 2500) {
      this.last = now;
      alert(msg);
    }
  }
}

var SwTerms = {
	map: {},

	getTerms: function(uri, onLoadStart, onLoadComplete) {
		if (this.map[uri]) {
			onLoadComplete(this.map[uri]);
			return;
		} else
		  onLoadStart();

		var query =
			"PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"SELECT ?uri WHERE { " +
			"{ ?uri a owl:Class } UNION " +
			"{ ?uri a rdfs:Class } UNION " +
			"{ ?uri a owl:ObjectProperty } UNION " +
			"{ ?uri a owl:DatatypeProperty } UNION " +
			"{ ?uri a rdf:Property  } UNION " +
			"{ ?uri a owl:NamedIndividual }" +
			"FILTER (STRSTARTS(str(?uri), '" + uri + "')) " +
			"} ORDER BY ?uri";
		// console.log(query);

    var endpoint = "http://ppr.cs.dal.ca:3010/terms/query";
		var url = endpoint + "?query=" + encodeURIComponent(query);
		console.log("calling: " + url);

		var xhttp = new XMLHttpRequest();
		xhttp.onreadystatechange = function() {
		  if (this.readyState == 4) {
        if (this.status == 200) {
          var results = JSON.parse(this.responseText);
          var terms = SwTerms.collectTerms(uri, results);
          SwTerms.map[uri] = terms;

          // if retrieval is very quick, just to show there's some loading going on..
          // ideally onLoadStart() would only be called if retrieval is being slow
          setTimeout(function() {
            onLoadComplete(terms);
          }, 100);
        } else {
          NonRepeatingAlert.alert("Cannot reach SPARQL endpoint: " + endpoint + ". " +
            "Dropdowns for qualified-names will not display properly.");
          console.log("SwTerms: not-ok xhttp response:", this.status);
        }
      }
		};
		xhttp.onerror = function () {
      console.log("SwTerms: error occurred in xhttp");
    };
		xhttp.open("GET", url, true);
		xhttp.send();
	},

	collectTerms: function(namespace, results) {
		var bindings = results.results.bindings;
		if (bindings.length == 0) {
			console.log("no results found");
			return [[ '', '' ]];
		} else
			console.log(bindings.length + " results found");

		var array = [];
		for (var i = 0; i < bindings.length; i++) {
			var binding = bindings[i];
			var uri = binding.uri.value;
			var name = uri.substring(namespace.length);
			if (name)
				array.push([ name, uri ]);
		}
		return array;
	}
};

// inspired by
// https://stackoverflow.com/questions/51667300/how-to-hide-remove-field-in-blockly

Blockly.Blocks['logic_qname_select'] = {
  category: 'Logic',
  curNs: "",

  /**
  * @this Blockly.BlockSvg
  */
  init: function() {
    this.setInputsInline(true);
    this.setColour(Blockly.LOGIC_CATEGORY_HUE);
    this.setOutput(true, ['qname']);

    var self = this;
    var dropdown = new Blockly.FieldDropdown(function() {
      // need to re-load these each time the dropdown is accessed
      return getDeclaredNamespaces(self.workspace);
    }, function(newNs) {
      // each time prefix selection changes
      self.onNsChange.call(self, newNs, false);
    });

    this.appendDummyInput()
      .appendField(dropdown, 'NAMESPACE')
      .appendField(':');

    this.appendDummyInput('LOCALNAME')
      .appendField(new Blockly.FieldDropdown([[ '', '' ]]), 'URI');
  },

  onNsChange: function(newNs, callback) {
    if (newNs !== '' && newNs !== this.curNs) {
      this.curNs = newNs;
      this.updateShape(callback);
    }
  },

  updateShape: function(callback) {
    var block = this;
    SwTerms.getTerms(this.curNs,
      function() { block.updateLNField(null); },
      function(terms) {
        // console.log(terms);
        block.updateLNField(terms);
        if (callback)
          callback();
      });
  },

  updateLNField: function(terms) {
    if (this.getInput('LOCALNAME')) {
      this.removeInput('LOCALNAME');
    }
    var input = this.appendDummyInput('LOCALNAME');
    if (terms === null)
      input.appendField("...", 'URI');
    else
      input.appendField(new Blockly.FieldDropdown(terms), 'URI');
  },

  mutationToDom: function () {
    var container = document.createElement('mutation');
    container.setAttribute('cur_ns', this.curNs);
    container.setAttribute('cur_ln', this.getFieldValue('URI'));
    return container;
  },

  domToMutation: function (container) {
    var curNs = container.getAttribute('cur_ns');
    var curLn = container.getAttribute('cur_ln');
    if (curNs != '') {
      var self = this;
      this.onNsChange(curNs, function() { self.setFieldValue(curLn, 'URI'); });
    }
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
