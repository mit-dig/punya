// -*- mode: javascript; js-indent-level: 2; -*-
// Copyright © MIT, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

'use strict';

goog.provide('AI.Blockly.Blocks.gql');
goog.provide('AI.Blockly.GraphQL');
goog.require('AI.Blockly.FieldFlydown');
goog.require('Blockly.Blocks.Utilities');

// Initialize namespace.
Blockly.Blocks.gql = {};
Blockly.GraphQLBlock = {};

// Constants for GraphQL blocks.
Blockly.GraphQLBlock.PRIMARY_COLOR = '#e535ab';
Blockly.GraphQLBlock.SECONDARY_COLOR = '#161e26';

// Constant for special internal root type. Must not be a valid GraphQL name.
Blockly.GraphQLBlock.ROOT_TYPE = '[root]';

// GraphQL introspection query.
// <editor-fold desc="INTROSPECTION_QUERY">
Blockly.GraphQLBlock.INTROSPECTION_QUERY =
  'query IntrospectionQuery { ' +
  '  __schema { ' +
  '    queryType { ' +
  '      name ' +
  '    } ' +
  '    mutationType { ' +
  '      name ' +
  '    } ' +
  '    types { ' +
  '      ...FullType ' +
  '    } ' +
  '  } ' +
  '} ' +
  ' ' +
  'fragment FullType on __Type { ' +
  '  kind ' +
  '  name ' +
  '  description ' +
  '  fields(includeDeprecated: true) { ' +
  '    name ' +
  '    description ' +
  '    args { ' +
  '      ...InputValue ' +
  '    } ' +
  '    type { ' +
  '      ...TypeRef ' +
  '    } ' +
  '    isDeprecated ' +
  '    deprecationReason ' +
  '  } ' +
  '  inputFields { ' +
  '    ...InputValue ' +
  '  } ' +
  '  interfaces { ' +
  '    ...TypeRef ' +
  '  } ' +
  '  enumValues(includeDeprecated: true) { ' +
  '    name ' +
  '    description ' +
  '    isDeprecated ' +
  '    deprecationReason ' +
  '  } ' +
  '  possibleTypes { ' +
  '    ...TypeRef ' +
  '  } ' +
  '} ' +
  ' ' +
  'fragment InputValue on __InputValue { ' +
  '  name ' +
  '  description ' +
  '  type { ' +
  '    ...TypeRef ' +
  '  } ' +
  '  defaultValue ' +
  '} ' +
  ' ' +
  'fragment TypeRef on __Type { ' +
  '  kind ' +
  '  name ' +
  '  ofType { ' +
  '    kind ' +
  '    name ' +
  '    ofType { ' +
  '      kind ' +
  '      name ' +
  '      ofType { ' +
  '        kind ' +
  '        name ' +
  '      } ' +
  '    } ' +
  '  } ' +
  '} ';
// </editor-fold>

// GraphQL component instances.
Blockly.GraphQLBlock.instances = {};

// GraphQL introspection query cache.
Blockly.GraphQLBlock.schemas = {};

// Register an instance with an endpoint.
Blockly.GraphQLBlock.registerInstance = function(uid, endpointUrl, httpHeaders) {
  // Add instance.
  Blockly.GraphQLBlock.instances[uid] = endpointUrl;

  // Update (or fetch) the schema for the associated endpoint
  Blockly.GraphQLBlock.updateSchema(endpointUrl, httpHeaders);
};

// Unregister an instance.
Blockly.GraphQLBlock.unregisterInstance = function(uid) {
  // Get the endpoint associated with the instance.
  var endpointUrl = Blockly.GraphQLBlock.instances[uid];

  // If the instance is not registered, we are done.
  if (endpointUrl === undefined) {
    return;
  }

  // Remove the entry.
  delete Blockly.GraphQLBlock.instances[uid];

  // Find another instance with the same endpoint.
  var otherUid = goog.array.find(Object.values(Blockly.GraphQLBlock.instances), function(url) {
    return url === endpointUrl;
  });

  // Remove the schema if this was the last associated instance.
  if (otherUid === null) {
    delete Blockly.GraphQLBlock.schemas[endpointUrl];
  }
};

// Generates an array of top-level blocks associated with the given instance.
Blockly.GraphQLBlock.instanceBlocks = function(uid) {
  // Keep track of blocks.
  var blocks = [];

  // If the instance is not registered, return.
  if (!Blockly.GraphQLBlock.instances.hasOwnProperty(uid)) {
    return blocks;
  }

  // Get the endpoint associated with the instance.
  var endpoint = Blockly.GraphQLBlock.instances[uid];

  // If the schema for the endpoint does not exist, return.
  if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(endpoint)) {
    return blocks;
  }

  // Add all blocks of the root.
  Array.prototype.push.apply(blocks, Blockly.GraphQLBlock.buildTypeBlocks(endpoint, Blockly.GraphQLBlock.ROOT_TYPE));

  // Return the list of blocks.
  return blocks;
};

// Determine whether a type should be quoted.
Blockly.GraphQLBlock.shouldQuote = function(typeString) {
  // Skip non-null.
  if (typeString.endsWith('!')) {
    typeString = typeString.substring(0, typeString.length - 1);
  }

  // Only scalar strings and ids should be quoted.
  return typeString === 'String' || typeString === 'ID';
};

// Traverses a type reference to get the base type reference.
Blockly.GraphQLBlock.traverseTypeRef = function(typeRef) {
  // Traverse type reference until we reach a base type.
  while (typeRef.kind === 'LIST' || typeRef.kind === 'NON_NULL') {
    typeRef = typeRef.ofType;
  }

  // Return the base type reference.
  return typeRef;
};

// Creates a default argument block if possible.
Blockly.GraphQLBlock.defaultArgument = function(typeRef, defaultValue) {
  // No need to handle null default values.
  if (defaultValue === null) {
    return null;
  }

  // TODO(bobbyluig): This requires a GraphQL parser. Decide if that is necessary later.
  return null;
};

// Traverses a type reference to get a type string.
Blockly.GraphQLBlock.typeString = function(typeRef) {
  // Handle not null types.
  if (typeRef.kind === 'NON_NULL') {
    return Blockly.GraphQLBlock.typeString(typeRef.ofType) + '!';
  }

  // Handle list types.
  if (typeRef.kind === 'LIST') {
    return '[' + Blockly.GraphQLBlock.typeString(typeRef.ofType) + ']';
  }

  // Handle base case.
  return typeRef.name;
};

// Creates a list of block elements from a given type.
Blockly.GraphQLBlock.buildTypeBlocks = function(gqlUrl, gqlBaseType) {
  // Fetch the associated type.
  var schema = Blockly.GraphQLBlock.schemas[gqlUrl];
  var type = schema.types[gqlBaseType];

  // Create an array to store blocks.
  var blocks = [];

  // Get all fields for the type.
  var allFields = [];
  for (var field in type.fields) {
    if (type.fields.hasOwnProperty(field)) {
      allFields.push(field);
    }
  }

  // Go through all fields for the type.
  for (var i = 0, fieldName; fieldName = allFields[i]; i++) {
    // Create a new block.
    var block = document.createElement('block');
    block.setAttribute('type', 'gql');
    blocks.push(block);

    // Get the field.
    var field = type.fields[fieldName];

    // Get field type reference.
    var fieldTypeRef = Blockly.GraphQLBlock.traverseTypeRef(field.type);

    // Create a new mutation.
    var mutation = document.createElement('mutation');
    mutation.setAttribute('gql_url', gqlUrl);
    mutation.setAttribute('gql_parent', gqlBaseType);
    mutation.setAttribute('gql_name', fieldName);
    block.appendChild(mutation);

    // If the field is an object, interface, or union, then it can have fields of its own.
    if (fieldTypeRef.kind === 'OBJECT' || fieldTypeRef.kind === 'INTERFACE' || fieldTypeRef.kind === 'UNION') {
      mutation.setAttribute('gql_fields', '1');
    }

    // If there are arguments, add a default GraphQL dictionary.
    if (field.args.length > 0) {
      mutation.setAttribute('gql_arguments', '');

      var gqlArguments = document.createElement('value');
      gqlArguments.setAttribute('name', 'GQL_TITLE');

      var argumentsBlock = document.createElement('block');
      argumentsBlock.setAttribute('type', 'gql_dict');

      var argumentsMutation = document.createElement('mutation');
      argumentsMutation.setAttribute('items', '1');
      argumentsMutation.setAttribute('gql_url', gqlUrl);
      argumentsMutation.setAttribute('gql_base_type', gqlBaseType + '.' + field.name);

      argumentsBlock.appendChild(argumentsMutation);
      gqlArguments.appendChild(argumentsBlock);
      block.appendChild(gqlArguments);
    }
  }

  // Get all possible types.
  var possibleTypes = type.possibleTypes || [];

  // Build fragments on possible types.
  for (var i = 0, possibleType; possibleType = possibleTypes[i]; i++) {
    // Create a new block.
    var block = document.createElement('block');
    block.setAttribute('type', 'gql');
    blocks.push(block);

    // Get the base type.
    var baseType = Blockly.GraphQLBlock.traverseTypeRef(possibleType);

    // Create a new mutation.
    var mutation = document.createElement('mutation');
    mutation.setAttribute('gql_url', gqlUrl);
    mutation.setAttribute('gql_name', baseType.name);
    mutation.setAttribute('gql_fields', '1');
    block.appendChild(mutation);
  }

  // Return the block elements.
  return blocks;
};

// Method to update cached introspection query and associated blocks.
Blockly.GraphQLBlock.updateSchema = function(endpoint, headers) {
  // Build post data.
  var data = {
    'query': Blockly.GraphQLBlock.INTROSPECTION_QUERY,
    'operationName': 'IntrospectionQuery'
  };

  // Parse headers.
  var headers = (!!headers) ? JSON.parse(headers) : {};

  // Set content type.
  headers['content-type'] = 'application/json; charset=utf-8';

  // Create a new request object.
  var xhr = new XMLHttpRequest();

  // Response handler.
  xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {
      // Check if there were any errors sending the requests.
      if (xhr.status !== 200) {
        console.log('Introspection query for ' + endpoint + ' failed with error code ' + xhr.status + '.');
        return;
      }

      // Get the response, which is in JSON format.
      var response = JSON.parse(xhr.responseText);

      // Check if there was data.
      if (!response.hasOwnProperty('data')) {
        console.log('Introspection query for ' + endpoint + ' failed with GraphQL errors.', response['errors']);
        return;
      }

      // Fetch the raw schema.
      var schema = response['data']['__schema'];

      // Create a type mapping for fast name lookup.
      var newTypes = {};

      // Modify the old type objects and add them to new types.
      for (var i = 0, type; type = schema.types[i]; i++) {
        // Extract the type name as the key.
        var typeName = type['name'];

        // Set the modified type object under the type name.
        newTypes[typeName] = type;

        // Create a field mapping for fast name lookup.
        var newFields = {};

        // Determine if there are any fields.
        if (type['fields'] !== null) {
          // Modify the old field objects and add them to new fields.
          for (var j = 0, field; field = type.fields[j]; j++) {
            // Extract the field name as the key.
            var fieldName = field['name'];

            // Set the modified field object under the field name.
            newFields[fieldName] = field;

            // If there are arguments, create a new anonymous input type.
            if (field.args !== null && field.args.length > 0) {
              newTypes[typeName + '.' + fieldName] = {
                'kind': 'INPUT_OBJECT',
                'name': typeName + '.' + fieldName,
                'description': '',
                'inputFields': field.args
              }
            }
          }
        }

        // Add the __typename field.
        newFields['__typename'] = {
          'name': '___typename',
          'description': null,
          'args': [],
          'type': {
            'kind': 'NON_NULL',
            'name': null,
            'ofType': {
              'kind': 'SCALAR',
              'name': 'String',
              'ofType': null
            }
          },
          'isDeprecated': false,
          'deprecationReason': null
        };

        // Replace the old fields with the new fields.
        type['fields'] = newFields;
      }

      // Get all possible object and interface types for fragment block generation.
      var possibleTypes = goog.array.filter(schema['types'], function(type) {
        return (type.kind === 'OBJECT' || type.kind === 'INTERFACE') && !type.name.startsWith('__');
      });

      // Add the special schema root type, which contains the root fields and all possible types
      newTypes[Blockly.GraphQLBlock.ROOT_TYPE] = {
        'fields': {},
        'possibleTypes': possibleTypes
      };

      // If there is a query type, add it to the root fields.
      if (schema['queryType'] !== null) {
        newTypes[Blockly.GraphQLBlock.ROOT_TYPE]['fields']['query'] = {
          'args': [],
          'description': 'A GraphQL query.',
          'type': {
            'kind': 'OBJECT',
            'name': schema['queryType']['name']
          }
        };
      }

      // If there is a mutation type, add it to the root fields.
      if (schema['mutationType'] !== null) {
        newTypes[Blockly.GraphQLBlock.ROOT_TYPE]['fields']['mutation'] = {
          'args': [],
          'description': 'A GraphQL mutation.',
          'type': {
            'kind': 'OBJECT',
            'name': schema['mutationType']['name']
          }
        };
      }

      // Replace the old types with the new types.
      schema['types'] = newTypes;

      // Store the modified schema in cache.
      Blockly.GraphQLBlock.schemas[endpoint] = schema;

      // If the workspace was already injected, update the schema on the appropriate blocks.
      if (Blockly.mainWorkspace != null) {
        // Fetch all blocks from the current workspace.
        var allBlocks = Blockly.mainWorkspace.getAllBlocks();

        // Go through blocks.
        for (var i = 0, block; block = allBlocks[i]; i++) {
          // Filter by GraphQL blocks.
          if (block.type.startsWith('gql') && typeof block.updateSchema === 'function') {
            // Inform the block that it should update its own schema.
            block.updateSchema();
          }
        }
      } else {
        // TODO(bobbyluig): Schema loaded too quickly.
      }
    }
  };

  // Send an introspection query.
  xhr.open('POST', endpoint);

  // Set headers.
  for (var name in headers) {
    if (headers.hasOwnProperty(name)) {
      xhr.setRequestHeader(name, headers[name]);
    }
  }

  // Send body.
  xhr.send(JSON.stringify(data));
};

// Convert a GraphQL type string to its corresponding Blockly type.
Blockly.GraphQLBlock.blocklyTypes = function(gqlUrl, typeString) {
  // Keep track of valid types.
  var types = [];

  // Handle nullity.
  if (typeString.endsWith('!')) {
    typeString = typeString.substring(0, typeString.length - 1);
  } else {
    types.push('GraphQLNull');
  }

  // Handle lists.
  if (typeString.startsWith('[') && typeString.endsWith(']')) {
    types.push('GraphQLList');
    return types;
  }

  // Fetch the type.
  var schema = Blockly.GraphQLBlock.schemas[gqlUrl];
  var type = schema.types[typeString];

  // Handle non-scalar types.
  switch (type.kind) {
    case 'ENUM':
      types.push('GraphQLEnum');
      break;
    case 'OBJECT':
    case 'INPUT_OBJECT':
      types.push('GraphQLDict');
      break;
    case 'SCALAR':
      switch (type.name) {
        case 'Int':
        case 'Float':
          types.push('Number');
          break;
        case 'ID':
        case 'String':
          types.push('String');
          break;
        case 'Boolean':
          types.push('Boolean');
          break;
      }
      break;
  }

  // Types may be empty or contain up to two types.
  return types;
};

// The GraphQL mutator for adding and removing fields.
Blockly.Blocks['gql_mutator'] = {
  init: function() {
    this.setColour(Blockly.GraphQLBlock.PRIMARY_COLOR);
    this.appendDummyInput().appendField('field');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setTooltip('Add a field to this object.');
    this.contextMenu = false;
  }
};

// The GraphQL null argument block, which represents the default value of a nullable type.
Blockly.Blocks['gql_null'] = {
  init: function() {
    // TODO(bobbyluig): Decide whether this should be colored the same as logic.
    this.setColour(Blockly.GraphQLBlock.PRIMARY_COLOR);
    this.appendDummyInput().appendField('null');
    this.setOutput(true, ['GraphQLNull']);
    this.setTooltip('A GraphQL null value.');
  }
};

// The base GraphQL block type.
Blockly.Blocks['gql'] = {
  helpUrl: function() {
    var prefix = 'https://graphql-docs.com/docs/';

    if (this.gqlParent === null) {
      prefix += this.gqlName + '/';
    } else if (this.gqlParent !== Blockly.GraphQLBlock.ROOT_TYPE) {
      prefix += this.gqlParent + '/';
    }

    return prefix + '?graphqlUrl=' + this.gqlUrl;
  },

  mutationToDom: function() {
    // Create a new mutation element to store data.
    var mutation = document.createElement('mutation');

    // Set basic attributes for this block shared by all GraphQL blocks.
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_name', this.gqlName);

    // Only non-fragments have parents.
    if (this.gqlParent !== null) {
      mutation.setAttribute('gql_parent', this.gqlParent);
    }

    // If this block has fields, store its field count.
    if (this.gqlHasFields) {
      mutation.setAttribute('gql_fields', this.itemCount_);
    }

    // If the block has arguments, store a sentinel value.
    if (this.gqlHasArguments) {
      mutation.setAttribute('gql_arguments', '');
    }

    return mutation;
  },

  domToMutation: function(xmlElement) {
    // Mutations must be idempotent for undo and redo to work correctly. The only mutation that can be applied by the
    // user is a change in the number of fields.
    if (this.gqlHasFields) {
      // Remove all old fields.
      for (var i = 0; i < this.itemCount_; i++) {
        this.removeInput(this.repeatingInputName + i);
      }

      // Update field count.
      this.itemCount_ = parseInt(xmlElement.getAttribute('gql_fields'));

      // Create new fields.
      for (var i = 0; i < this.itemCount_; i++) {
        this.addInput(i);
      }

      // There are no more mutations to apply. Nothing else could have been changed by the user.
      return;
    }

    // Extract basic mutation attributes shared by all GraphQL blocks.
    this.gqlUrl = xmlElement.getAttribute('gql_url');
    this.gqlName = xmlElement.getAttribute('gql_name');
    this.gqlParent = xmlElement.getAttribute('gql_parent') || null;

    // Determine whether the block is an object or a scalar.
    this.gqlHasFields = xmlElement.hasAttribute('gql_fields');

    // Determine whether the block has any arguments.
    this.gqlHasArguments = xmlElement.hasAttribute('gql_arguments');

    // Set the color of the block to a beautiful GraphQL pink.
    this.setColour(Blockly.GraphQLBlock.PRIMARY_COLOR);

    // Add the title row, which either has an input for arguments or is a dummy input.
    var title;
    if (this.gqlHasArguments) {
      title = this.appendValueInput('GQL_TITLE');
      title.setCheck(['GraphQLDict']);
    } else {
      title = this.appendDummyInput('GQL_TITLE');
    }

    // For fragments, add prefix.
    if (this.gqlParent === null) {
      title.appendField('... on');
    }

    // Add title field.
    title.appendField(this.gqlName, 'GQL_TITLE_FIELD');

    // The output type of the block is initially GraphQL.
    this.setOutput(true, ['GraphQL', 'String']);

    // For non-scalar blocks, users should be able add and remove fields.
    if (this.gqlHasFields) {
      // Initialize required mutator parameters.
      this.emptyInputName = null;
      this.repeatingInputName = 'GQL_FIELD';
      this.itemCount_ = parseInt(xmlElement.getAttribute('gql_fields'));

      // Set mutator.
      this.setMutator(new Blockly.Mutator(['gql_mutator']));

      // Populate initial field value inputs.
      for (var i = 0; i < this.itemCount_; i++) {
        this.addInput(i);
      }
    }

    // Set the initial tooltip.
    this.setTooltip('A dynamically generated GraphQL block.');

    // Try to perform a schema update.
    this.updateSchema();
  },

  updateContainerBlock: function(containerBlock) {
    containerBlock.setFieldValue('object', 'CONTAINER_TEXT');
    containerBlock.setTooltip('Add, remove, or reorder fields to reconfigure this GraphQL block.');
  },

  compose: Blockly.compose,

  decompose: function(workspace) {
    return Blockly.decompose(workspace, 'gql_mutator', this);
  },

  saveConnections: Blockly.saveConnections,

  addEmptyInput: function() {
  },

  addInput: function(inputNumber) {
    return this
      .appendIndentedValueInput(this.repeatingInputName + inputNumber)
      .setCheck(['GraphQL']);
  },

  customContextMenu: function(options) {
    // If there is no schema, don't add the custom context menu.
    if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)) {
      return options;
    }

    // Properties for getter generation.
    var option = {enabled: true};
    option.text = 'Generate Getter';

    // Alias this block block and this workspace.
    var thisBlock = this;
    var workspace = this.workspace;

    // Function to create and render.
    var newBlock = function(type) {
      var block = workspace.newBlock(type);
      block.initSvg();
      block.render();
      return block;
    };

    // Callback to generate getter.
    option.callback = function() {
      // Keep track of the path as a list of tuples in the form of (name, isList).
      var path = [];

      // Fetch the schema (assuming fixed schema for path).
      var schema = Blockly.GraphQLBlock.schemas[thisBlock.gqlUrl];

      // Continue until we reach a non-GraphQL block or a root block.
      var block = thisBlock;
      while (block && block.type === 'gql' && block.gqlParent !== Blockly.GraphQLBlock.ROOT_TYPE) {
        // Skip fragments.
        if (!block.gqlParent) {
          block = block.getParent();
          continue;
        }

        // Get the type of the block.
        var parentType = schema.types[block.gqlParent];
        var type = parentType.fields[block.gqlName].type;

        // Determine whether the type is a list.
        type = (type.kind === 'NON_NULL') ? type.ofType : type;
        var isList = (type.kind === 'LIST');

        // Add to path.
        path.push([block.gqlName, isList]);

        // Move to parent block.
        block = block.getParent();
      }

      // Begin group.
      Blockly.Events.setGroup(true);

      // Build path into blocks.
      var previousBlock = null;
      while (path.length > 0) {
        // Pop the last tuple.
        var tuple = path.pop();

        // Always need to fetch the field from the dictionary.
        var dictBlock = newBlock('dictionary_lookup');
        var keyBlock = newBlock('text');
        var notFoundBlock = newBlock('text');
        keyBlock.getField('TEXT').setText(tuple[0]);
        notFoundBlock.getField('TEXT').setText('not found');
        dictBlock.getInput('KEY').connection.connect(keyBlock.outputConnection);
        dictBlock.getInput('NOTFOUND').connection.connect(notFoundBlock.outputConnection);

        // Set the current block.
        var currentBlock = dictBlock;

        // Fetch an index from the list if necessary.
        if (tuple[1] && path.length > 1) {
          var listBlock = newBlock('lists_select_item');
          var numBlock = newBlock('math_number');
          listBlock.getInput('NUM').connection.connect(numBlock.outputConnection);
          listBlock.getInput('LIST').connection.connect(dictBlock.outputConnection);
          currentBlock = listBlock;
        }

        // Connect previous block to current block and update previous block.
        previousBlock && dictBlock.getInput('DICT').connection.connect(previousBlock.outputConnection);
        previousBlock = currentBlock;
      }

      // Select the block and center.
      if (previousBlock) {
        // Center the generated block.
        var metrics = previousBlock.workspace.getMetrics();
        var scale = previousBlock.workspace.scale;
        previousBlock.moveBy(metrics.viewLeft / scale, metrics.viewTop / scale);
        workspace.getParentSvg().parentElement.focus();

        // Select the block.
        previousBlock.select();
      }
    };

    // End group.
    Blockly.Events.setGroup(false);

    // Add option.
    options.push(option);
  },

  updateSchema: function() {
    // If there is no schema, we can't update yet.
    if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)) {
      return;
    }

    // Fetch the schema.
    var schema = Blockly.GraphQLBlock.schemas[this.gqlUrl];

    // Get the base type for fragments.
    if (this.gqlParent === null) {
      // Perform type existence check on fragments.
      if (!schema.types.hasOwnProperty(this.gqlName)) {
        console.log('The fragment base type "' + this.gqlName + '" no longer exists.');
        return;
      }

      // The base type is the name.
      this.gqlBaseType = this.gqlName;
    }

    // Get the base type for non-fragments.
    else {
      // Perform parent type existence check on non-fragments.
      if (this.gqlParent !== null && !schema.types.hasOwnProperty(this.gqlParent)) {
        console.log('The parent base type "' + this.gqlParent + '" no longer exists.');
        return;
      }

      // Get the parent type.
      var parentType = schema.types[this.gqlParent];

      // Perform field existence check.
      if (!parentType.fields.hasOwnProperty(this.gqlName)) {
        console.log('The field "' + this.gqlName + '" no longer exists for the type "' + this.gqlParent + '".');
        return;
      }

      // Get own type reference, which must exist relative to parent assuming that the schema is well-formed.
      var rootTypeRef = parentType.fields[this.gqlName].type;
      var typeRef = Blockly.GraphQLBlock.traverseTypeRef(rootTypeRef);

      // Set the type name.
      this.gqlBaseType = typeRef.name;
    }

    // Fetch the actual type object associated with this block's GraphQL type.
    var type = schema.types[this.gqlBaseType];

    // Perform field existence type check.
    if (type.kind === 'OBJECT' && !this.gqlHasFields || type.kind === 'SCALAR' && this.gqlHasFields) {
      console.log('Field existence mismatch.');
      return;
    }

    // Get the title input.
    var titleInput = this.getInput('GQL_TITLE');

    // If we are an object, enable field autocompletion.
    if (this.gqlHasFields) {
      // Remove the old title field and replace it with a flydown field.
      var flydown = new Blockly.GqlFlydown(this.gqlName, this.gqlUrl, this.gqlBaseType);
      titleInput.removeField('GQL_TITLE_FIELD');
      titleInput.appendField(flydown, 'GQL_TITLE_FIELD');

      // In order to correctly compute the width of the flydown, this block needs to be rendered when the workspace is
      // shown the next time.
      if (this.rendered) {
        this.workspace.blocksNeedingRendering.push(this);
      }
    }

    // Get the description for fragments.
    if (this.gqlParent === null) {
      // TODO(bobbyluig)
    }

    // Get the description for non-fragments.
    else {
      // Fetch the description from the parent type information.
      var description = parentType.fields[this.gqlName].description;

      // Update description if available.
      if (description) {
        this.setTooltip(description);
      }
    }

    // Set the output to allow for enhanced type checking.
    this.setOutput(true, [function(sourceConnection, targetConnection) {
      // Get the source and target blocks.
      var sourceBlock = sourceConnection.sourceBlock_;
      var targetBlock = targetConnection.sourceBlock_;

      // Degrade to a normal check for parent blocks that do not require special validation.
      if (targetBlock.typeName !== 'GraphQL' && targetBlock.type !== 'gql') {
        return targetConnection.check_.indexOf('GraphQL') !== -1;
      }

      // If the parent block is the query method, perform root and endpoint checking.
      if (targetBlock.typeName === 'GraphQL') {
        // Fetch the endpoint of the instance block.
        var uid = sourceBlock.workspace.componentDb_.getUidForName(targetBlock.instanceName);
        var endpoint = Blockly.GraphQLBlock.instances[uid];

        // Determine if the endpoints match.
        if (endpoint !== sourceBlock.gqlUrl) {
          return false;
        }

        // Fetch the schema for the endpoint.
        var schema = Blockly.GraphQLBlock.schemas[endpoint];

        // Only perform root checking for updated schemas.
        if (schema === undefined) {
          return true;
        }

        // Determine if the block is a valid root field.
        if (!schema.types[Blockly.GraphQLBlock.ROOT_TYPE].fields.hasOwnProperty(sourceBlock.gqlName)) {
          return false;
        }

        // All checks passed.
        return true;
      }

      // Check endpoint compatibility.
      if (sourceBlock.gqlUrl !== targetBlock.gqlUrl) {
        return false;
      }

      // Ensure connection to the body and not the title.
      if (targetConnection.subtype !== Blockly.INDENTED_VALUE) {
        return false;
      }

      // Object type checking is only valid on blocks with an updated schema.
      if (sourceBlock.gqlBaseType === undefined || targetBlock.gqlBaseType === undefined) {
        return true;
      }

      // Fetch the schema.
      var schema = Blockly.GraphQLBlock.schemas[sourceBlock.gqlUrl];

      // Perform checking on fragments.
      if (sourceBlock.gqlParent === null) {
        // Get the parent type reference.
        var parentTypeRef = schema.types[targetBlock.gqlBaseType];

        // Get the parent type name and possible types.
        var allPossibleTypes = (parentTypeRef.possibleTypes || []).slice(0);
        allPossibleTypes.push(parentTypeRef);

        // Locate valid type.
        var validType = goog.array.find(allPossibleTypes, function(type) {
          return Blockly.GraphQLBlock.traverseTypeRef(type).name === sourceBlock.gqlBaseType;
        });

        // Check for type validity.
        if (!validType) {
          return false;
        }
      }

      // Perform checking on non-fragments.
      else {
        // Fetch the parent type, which must exist.
        var parentType = schema.types[sourceBlock.gqlParent];

        // Check for field validity.
        if (sourceBlock.gqlParent !== targetBlock.gqlBaseType ||
          !parentType.fields.hasOwnProperty(sourceBlock.gqlName)) {
          return false;
        }
      }

      // All checks passed.
      return true;
    }]);
  }
};

// The GraphQL enumeration type.
Blockly.Blocks['gql_enum'] = {
  mutationToDom: function() {
    var mutation = document.createElement('mutation');

    // Set the attributes for the block. Note that we need to store the current dropdown value.
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_base_type', this.gqlBaseType);
    mutation.setAttribute('gql_value', this.getGqlValue());

    return mutation;
  },

  domToMutation: function(xmlElement) {
    // TODO(bobbyluig): Decide whether this should be colored the same as a constant.
    this.setColour(Blockly.GraphQLBlock.PRIMARY_COLOR);

    // Get the attributes for the block. The value is used as an initial value for the dropdown.
    this.gqlUrl = xmlElement.getAttribute('gql_url');
    this.gqlBaseType = xmlElement.getAttribute('gql_base_type');
    this.gqlValue = xmlElement.getAttribute('gql_value');

    // Add the static value (not the dropdown).
    this.appendDummyInput('SELECTION').appendField(this.gqlValue, 'DROPDOWN');

    // The initial output type is GraphQLEnum.
    this.setOutput(true, ['GraphQLEnum']);

    // Set the initial tooltip.
    this.setTooltip('A dynamically generated GraphQL enumeration.');

    // Try to update the schema.
    this.updateSchema();
  },

  getGqlValue: function() {
    return (this.dropdown) ? this.dropdown.getValue() : this.gqlValue;
  },

  updateParent: function(parent, gqlName) {
    // Fetch the type.
    var schema = Blockly.GraphQLBlock.schemas[this.gqlUrl];
    var type = schema.types[this.gqlBaseType];

    // Locate the input field.
    var inputField = goog.array.find(type.inputFields, function(item) {
      return item.name === gqlName;
    });

    // Enable value autocompletion.
    var input = parent.getInput('VALUE');
    var typeString = Blockly.GraphQLBlock.typeString(inputField.type);
    var valueName = input.fieldRow[0].getText();
    var flydown = new Blockly.GqlPairFlydown(valueName, parent.gqlUrl, typeString);
    input.removeField(input.fieldRow[0].name);
    input.appendField(flydown)
      .setAlign(Blockly.ALIGN_RIGHT)
      .setCheck(Blockly.GraphQLBlock.blocklyTypes(parent.gqlUrl, typeString));

    // Determine if pair value should be quoted.
    parent.gqlQuote = Blockly.GraphQLBlock.shouldQuote(typeString);

    // Hide on collapsed.
    if (parent.isCollapsed()) {
      flydown.setVisible(false);
    }

    // Set the appropriate documentation.
    if (inputField.description) {
      parent.setTooltip(inputField.description);
    }

    // Always return since this is a validator.
    return gqlName;
  },

  updateSchema: function() {
    // If there is no schema, we can't update yet.
    if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)) {
      return;
    }

    // Fetch all values in the enum.
    var schema = Blockly.GraphQLBlock.schemas[this.gqlUrl];
    var type = schema.types[this.gqlBaseType];
    var enumValues = (type.kind === 'ENUM') ? type.enumValues : type.inputFields;

    // Function to get all values for the dropdown.
    var getValues = function() {
      var values = [];
      for (var i = 0, value; value = enumValues[i]; i++) {
        values.push([value.name, value.name]);
      }
      return values;
    };

    // Create a dropdown and set the default value.
    this.dropdown = new Blockly.FieldDropdown(getValues);
    this.dropdown.setValue(this.gqlValue);

    // Replace the field text with the dropdown.
    var input = this.getInput('SELECTION');
    input.removeField('DROPDOWN');
    input.appendField(this.dropdown, 'DROPDOWN');

    // Set the output to check for valid attachment point.
    if (type.kind === 'ENUM') {
      // Standard enum type check.
      this.setOutput(true, [function(sourceConnection, targetConnection) {
        // Get the source and target blocks.
        var sourceBlock = sourceConnection.sourceBlock_;
        var targetBlock = targetConnection.sourceBlock_;

        // Degrade to a normal check for non-specific parent blocks.
        if (targetBlock.type !== 'gql_pair') {
          return targetConnection.check_.indexOf('GraphQLEnum') !== -1;
        }

        // Perform endpoint validation.
        if (sourceBlock.gqlUrl !== targetBlock.gqlUrl) {
          return false;
        }

        // Fetch the parent input field.
        var schema = Blockly.GraphQLBlock.schemas[targetBlock.gqlUrl];
        var type = schema.types[targetBlock.gqlBaseType];
        var inputField = goog.array.find(type.inputFields, function(item) {
          return item.name === targetBlock.getGqlKey();
        });

        // Perform type validation.
        var typeString = (inputField.type.kind === 'NON_NULL')
          ? Blockly.GraphQLBlock.typeString(inputField.type.ofType)
          : Blockly.GraphQLBlock.typeString(inputField.type);
        return typeString === sourceBlock.gqlBaseType;
      }]);
    } else {
      // Fixed enum key. Use the output check as a mechanism to ensure correct initialization of autocompletion.
      // TODO(bobbyluig): Fix this hack.
      this.setOutput(true, [function(sourceConnection, targetConnection) {
        // Get the source and target blocks.
        var sourceBlock = sourceConnection.sourceBlock_;
        var targetBlock = targetConnection.sourceBlock_;

        // Initialize autocompletion.
        sourceBlock.updateParent(targetBlock, sourceBlock.gqlValue);

        // Eliminate the check once the block is connected.
        sourceBlock.setOutput(true);

        // The fixed enum cannot be created or attached by the user.
        return true;
      }]);
    }
  },

  onchange: function(e) {
    // Update parent on dropdown change.
    if (e.blockId === this.id && e.type === Blockly.Events.CHANGE) {
      this.updateParent(this.getParent(), e.newValue);
    }
  }
};

// The GraphQL dictionary type.
Blockly.Blocks['gql_dict'] = goog.object.clone(Blockly.Blocks['dictionaries_create_with']);
goog.object.extend(Blockly.Blocks['gql_dict'], {
  category: undefined,

  addInput: function(inputNum) {
    var input = this.appendValueInput(this.repeatingInputName + inputNum)
      .setCheck(['GraphQLPair']);

    // Handle the first input by enabling autocompletion when possible.
    if (inputNum === 0) {
      var flydown = Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)
        ? new Blockly.GqlDictionaryFlydown(this.gqlUrl, this.gqlBaseType)
        : Blockly.Msg.LANG_DICTIONARIES_MAKE_DICTIONARY_TITLE;
      input.appendField(flydown, 'FLYDOWN');

      // Hide on collapsed.
      if (this.isCollapsed()) {
        flydown.setVisible(false);
      }
    }

    return input;
  },

  mutationToDom: function() {
    var mutation = Blockly.mutationToDom.call(this);

    // Set the attributes, where the base type is the parent type.
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_base_type', this.gqlBaseType);

    return mutation;
  },

  domToMutation: function(xmlElement) {
    // Get the attributes.
    this.gqlUrl = xmlElement.getAttribute('gql_url');
    this.gqlBaseType = xmlElement.getAttribute('gql_base_type');

    // The parent class needs to initialize.
    Blockly.domToMutation.call(this, xmlElement);

    // The output type is initially GraphQLDict.
    this.setOutput(true, ['GraphQLDict']);

    // Set the tooltip.
    this.setTooltip('A dynamically generated GraphQL dictionary.');

    // Try to update the schema.
    this.updateSchema();
  },

  updateSchema: function() {
    // If there is no schema, we can't update yet.
    if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)) {
      return;
    }

    // Enable autocompletion.
    var flydown = new Blockly.GqlDictionaryFlydown(this.gqlUrl, this.gqlBaseType);
    var input = this.getInput(this.repeatingInputName + 0);
    input.removeField('FLYDOWN');
    input.appendField(flydown, 'FLYDOWN');

    // Hide on collapsed.
    if (this.isCollapsed()) {
      flydown.setVisible(false);
    }

    // Set the output to check for valid attachment point.
    this.setOutput(true, [function(sourceConnection, targetConnection) {
      // Get the source and target blocks.
      var sourceBlock = sourceConnection.sourceBlock_;
      var targetBlock = targetConnection.sourceBlock_;

      // Degrade to a normal check for non-specific parent blocks.
      if (targetBlock.type !== 'gql' && targetBlock.type !== 'gql_pair') {
        return targetConnection.check_.indexOf('GraphQLDict') !== -1;
      }

      // Perform endpoint validation.
      if (sourceBlock.gqlUrl !== targetBlock.gqlUrl) {
        return false;
      }

      // Perform base type validation for anonymous input object.
      if (targetBlock.type === 'gql') {
        return sourceBlock.gqlBaseType === targetBlock.gqlParent + '.' + targetBlock.gqlName;
      }

      // Deal with regular input objects.
      var schema = Blockly.GraphQLBlock.schemas[targetBlock.gqlUrl];
      var type = schema.types[targetBlock.gqlBaseType];
      var inputField = goog.array.find(type.inputFields, function(item) {
        return item.name === targetBlock.getGqlKey();
      });
      var baseType = (inputField.type.kind === 'NON_NULL')
        ? Blockly.GraphQLBlock.typeString(inputField.type.ofType)
        : Blockly.GraphQLBlock.typeString(inputField.type);

      // Perform base type validation.
      return sourceBlock.gqlBaseType === baseType;
    }]);
  },

  warnings: [{name: 'checkGraphQLArgs'}],

  onchange: function(e) {
    // Don't trigger error or warning checks on transient actions.
    if (e.isTransient) {
      return false;
    }

    // Perform error and warning checking.
    return this.workspace.getWarningHandler() && this.workspace.getWarningHandler().checkErrors(this);
  }
});

// The GraphQL pair type.
Blockly.Blocks['gql_pair'] = goog.object.clone(Blockly.Blocks['pair']);
goog.object.extend(Blockly.Blocks['gql_pair'], {
  category: undefined,

  getGqlKey: function() {
    return this.getInputTargetBlock('KEY').getGqlValue();
  },

  mutationToDom: function() {
    var mutation = document.createElement('mutation');

    // Set attributes, where the base type is the parent type.
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_base_type', this.gqlBaseType);
    mutation.setAttribute('gql_quote', this.gqlQuote);

    return mutation;
  },

  domToMutation: function(xmlElement) {
    // Get the attributes.
    this.gqlUrl = xmlElement.getAttribute('gql_url');
    this.gqlBaseType = xmlElement.getAttribute('gql_base_type');
    this.gqlQuote = xmlElement.getAttribute('gql_quote') === 'true';

    // Allow only enum keys.
    this.getInput('KEY').setCheck(['GraphQLEnum']);

    // The initial output type is GraphQLPair.
    this.setOutput(true, ['GraphQLPair']);

    // Set the initial tooltip.
    this.setTooltip('A dynamically generated GraphQL pair with the key and value provided.');

    // Try to update the schema.
    this.updateSchema();
  },

  updateSchema: function() {
    // If there is no schema, we can't update yet.
    if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)) {
      return;
    }

    // Set the output to check for valid attachment point.
    this.setOutput(true, [function(sourceConnection, targetConnection) {
      // Get the source and target blocks.
      var sourceBlock = sourceConnection.sourceBlock_;
      var targetBlock = targetConnection.sourceBlock_;

      // Degrade to a normal check for non-specific parent blocks.
      if (targetBlock.type !== 'gql_dict') {
        return targetConnection.check_.indexOf('GraphQLPair') !== -1;
      }

      // Perform endpoint and base type validation.
      return sourceBlock.gqlUrl === targetBlock.gqlUrl && sourceBlock.gqlBaseType === targetBlock.gqlBaseType;
    }]);
  }
});

// The GraphQL list type.
Blockly.Blocks['gql_list'] = goog.object.clone(Blockly.Blocks['lists_create_with']);
goog.object.extend(Blockly.Blocks['gql_list'], {
  category: undefined,

  addInput: function(inputNum) {
    var input = this.appendValueInput(this.repeatingInputName + inputNum);

    // Handle the first input by enabling autocompletion when possible.
    if (inputNum === 0) {
      var flydown = Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)
        ? new Blockly.GqlListFlydown(this.gqlUrl, this.gqlType)
        : Blockly.Msg.LANG_LISTS_CREATE_WITH_TITLE_MAKE_LIST;
      input.appendField(flydown, 'FLYDOWN');

      // Hide on collapsed.
      if (this.isCollapsed()) {
        flydown.setVisible(false);
      }
    }

    return input;
  },

  mutationToDom: function() {
    var mutation = Blockly.mutationToDom.call(this);

    // Set the attributes, where the type is the type string associated with the autocompletion value.
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_type', this.gqlType);
    mutation.setAttribute('gql_quote', this.gqlQuote);

    return mutation;
  },

  domToMutation: function(xmlElement) {
    // Get the attributes.
    this.gqlUrl = xmlElement.getAttribute('gql_url');
    this.gqlType = xmlElement.getAttribute('gql_type');
    this.gqlQuote = xmlElement.getAttribute('gql_quote') === 'true';

    // There are additional actions that the parent class needs to perform.
    Blockly.domToMutation.call(this, xmlElement);

    // The output type is initially GraphQLList.
    this.setOutput(true, ['GraphQLList']);

    // Set the tooltip.
    this.setTooltip('A dynamically generated GraphQL list.');

    // Try to update the schema.
    this.updateSchema();
  },

  updateSchema: function() {
    // If there is no schema, we can't update yet.
    if (!Blockly.GraphQLBlock.schemas.hasOwnProperty(this.gqlUrl)) {
      return;
    }

    // Enable autocompletion.
    var flydown = new Blockly.GqlListFlydown(this.gqlUrl, this.gqlType);
    var input = this.getInput(this.repeatingInputName + 0);
    input.removeField('FLYDOWN');
    input.appendField(flydown, 'FLYDOWN')
      .setCheck(Blockly.GraphQLBlock.blocklyTypes(this.gqlUrl, this.gqlType));

    // Hide on collapsed.
    if (this.isCollapsed()) {
      flydown.setVisible(false);
    }

    // Set the output to check for valid attachment point.
    this.setOutput(true, [function(sourceConnection, targetConnection) {
      // Get the source and target blocks.
      var sourceBlock = sourceConnection.sourceBlock_;
      var targetBlock = targetConnection.sourceBlock_;

      // Degrade to a normal check for non-specific parent blocks.
      if (targetBlock.type !== 'gql_pair') {
        return targetConnection.check_.indexOf('GraphQLList') !== -1;
      }

      // Fetch the parent input field.
      var schema = Blockly.GraphQLBlock.schemas[targetBlock.gqlUrl];
      var type = schema.types[targetBlock.gqlBaseType];
      var inputField = goog.array.find(type.inputFields, function(item) {
        return item.name === targetBlock.getGqlKey();
      });

      // Perform type validation.
      var typeString = (inputField.type.kind === 'NON_NULL')
        ? Blockly.GraphQLBlock.typeString(inputField.type.ofType)
        : Blockly.GraphQLBlock.typeString(inputField.type);
      return typeString.substring(1, typeString.length - 1) === sourceBlock.gqlType;
    }]);
  }
});

Blockly.GqlFlydown = function(name, gqlUrl, gqlBaseType) {
  this.gqlUrl = gqlUrl;
  this.gqlBaseType = gqlBaseType;

  Blockly.GqlFlydown.superClass_.constructor.call(this, name, false, null);
};
goog.inherits(Blockly.GqlFlydown, Blockly.FieldFlydown);
Blockly.GqlFlydown.prototype.fieldCSSClassName = 'blocklyGqlField';
Blockly.GqlFlydown.prototype.flyoutCSSClassName = 'blocklyGqlFlydown';
Blockly.GqlFlydown.prototype.flydownBlocksXML_ = function() {
  // Create a new root element.
  var xml = document.createElement('xml');

  // Get all blocks.
  var blocks = Blockly.GraphQLBlock.buildTypeBlocks(this.gqlUrl, this.gqlBaseType);

  // Add all blocks to the root.
  for (var i = 0, block; block = blocks[i]; i++) {
    xml.appendChild(block);
  }

  // Return the string representation of the element.
  return xml.outerHTML;
};

Blockly.GqlDictionaryFlydown = function(gqlUrl, gqlBaseType) {
  this.gqlUrl = gqlUrl;
  this.gqlBaseType = gqlBaseType;

  Blockly.GqlDictionaryFlydown.superClass_.constructor.call(this,
    Blockly.Msg.LANG_DICTIONARIES_MAKE_DICTIONARY_TITLE, false, null);
};
goog.inherits(Blockly.GqlDictionaryFlydown, Blockly.FieldFlydown);
Blockly.GqlDictionaryFlydown.prototype.fieldCSSClassName = 'blocklyDictField';
Blockly.GqlDictionaryFlydown.prototype.flyoutCSSClassName = 'blocklyDictFlydown';
Blockly.GqlDictionaryFlydown.prototype.flydownBlocksXML_ = function() {
  // Create a new root element.
  var xml = document.createElement('xml');

  // Fetch the associated field.
  var schema = Blockly.GraphQLBlock.schemas[this.gqlUrl];
  var type = schema.types[this.gqlBaseType];

  // Create an array to store blocks.
  var blocks = [];

  // Add one pair for each argument.
  for (var i = 0, arg; arg = type.inputFields[i]; i++) {
    // Create a key for the pair.
    var key = document.createElement('value');
    key.setAttribute('name', 'KEY');

    // Create an attached enum block as the key, but allow editing.
    var keyBlock = document.createElement('block');
    keyBlock.setAttribute('type', 'gql_enum');
    keyBlock.setAttribute('deletable', 'false');
    keyBlock.setAttribute('movable', 'false');

    // Set the properties of the key.
    var keyBlockMutation = document.createElement('mutation');
    keyBlockMutation.setAttribute('gql_url', this.gqlUrl);
    keyBlockMutation.setAttribute('gql_base_type', this.gqlBaseType);
    keyBlockMutation.setAttribute('gql_value', arg.name);

    // Append key.
    keyBlock.appendChild(keyBlockMutation);
    key.appendChild(keyBlock);

    // Create a value for the pair.
    var value = document.createElement('value');
    value.setAttribute('name', 'VALUE');

    // Create a default argument block if possible.
    var defaultArgument = Blockly.GraphQLBlock.defaultArgument(arg.type, arg.defaultValue);

    // If the default argument block exists, add it to the value.
    if (defaultArgument !== null) {
      value.appendChild(defaultArgument);
    }

    // Create a new pair.
    var pair = document.createElement('block');
    pair.setAttribute('type', 'gql_pair');

    // Set the attributes of the pair.
    var pairMutation = document.createElement('mutation');
    pairMutation.setAttribute('gql_url', this.gqlUrl);
    pairMutation.setAttribute('gql_base_type', this.gqlBaseType);

    // Add mutation, key, and value.
    pair.appendChild(pairMutation);
    pair.appendChild(key);
    pair.appendChild(value);

    // Add pair to blocks.
    blocks.push(pair);
  }

  // Add all blocks to the root.
  for (var i = 0, block; block = blocks[i]; i++) {
    xml.appendChild(block);
  }

  // Return the string representation of the element.
  return xml.outerHTML;
};

Blockly.GqlPairFlydown = function(name, gqlUrl, gqlType) {
  this.gqlUrl = gqlUrl;
  this.gqlType = gqlType;

  Blockly.GqlPairFlydown.superClass_.constructor.call(this, name, false, null);
};
goog.inherits(Blockly.GqlPairFlydown, Blockly.FieldFlydown);
Blockly.GqlPairFlydown.prototype.fieldCSSClassName = 'blocklyDictField';
Blockly.GqlPairFlydown.prototype.flyoutCSSClassName = 'blocklyDictFlydown';
Blockly.GqlPairFlydown.prototype.flydownBlocksXML_ = function() {
  // Create a new root element.
  var xml = document.createElement('xml');

  // Create a block.
  var block = document.createElement('block');

  // Fetch the type and schema.
  var type = this.gqlType;
  var schema = Blockly.GraphQLBlock.schemas[this.gqlUrl];

  // Remove trailing nullity identifier if it exists.
  if (type.endsWith('!')) {
    type = type.substring(0, type.length - 1);
  }

  // Handle booleans.
  if (type === 'Boolean') {
    // Set type.
    block.setAttribute('type', 'logic_boolean');

    // Create field with value.
    var field = document.createElement('field');
    field.setAttribute('name', 'BOOL');
    field.innerText = 'FALSE';

    // Add field to block.
    block.appendChild(field);
  }

  // Handle numbers.
  else if (type === 'Int' || type === 'Float') {
    // Set type.
    block.setAttribute('type', 'math_number');

    // Create field with value.
    var field = document.createElement('field');
    field.setAttribute('name', 'NUM');
    field.innerText = '0';

    // Add field to block.
    block.appendChild(field);
  }

  // Handle strings.
  else if (type === 'String' || type === 'ID') {
    // Set type.
    block.setAttribute('type', 'text');

    // Create field with value.
    var field = document.createElement('field');
    field.setAttribute('name', 'TEXT');
    field.innerText = '';

    // Add field to block.
    block.appendChild(field);
  }

  // Handle lists.
  else if (type.startsWith('[')) {
    type = type.substring(1, type.length - 1);
    block.setAttribute('type', 'gql_list');

    var mutation = document.createElement('mutation');
    mutation.setAttribute('items', '1');
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_type', type);
    mutation.setAttribute('gql_quote', Blockly.GraphQLBlock.shouldQuote(type));

    block.appendChild(mutation);
  }

  // Handle enumerations.
  else if (schema.types[type].kind === 'ENUM') {
    block.setAttribute('type', 'gql_enum');

    var mutation = document.createElement('mutation');
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_base_type', type);
    mutation.setAttribute('gql_value', schema.types[type].enumValues[0].name);
    block.appendChild(mutation);
  }

  // Handle input objects.
  else {
    block.setAttribute('type', 'gql_dict');

    var mutation = document.createElement('mutation');
    mutation.setAttribute('items', '1');
    mutation.setAttribute('gql_url', this.gqlUrl);
    mutation.setAttribute('gql_base_type', type);
    block.appendChild(mutation);
  }

  // Add the block.
  xml.appendChild(block);

  // Add null block if necessary.
  if (!this.gqlType.endsWith('!')) {
    var null_block = document.createElement('block');
    null_block.setAttribute('type', 'gql_null');
    xml.appendChild(null_block);
  }

  // Return the string representation of the element.
  return xml.outerHTML;
};

Blockly.GqlListFlydown = function(gqlUrl, gqlType) {
  this.gqlUrl = gqlUrl;
  this.gqlType = gqlType;

  Blockly.GqlListFlydown.superClass_.constructor.call(this,
    Blockly.Msg.LANG_LISTS_CREATE_WITH_TITLE_MAKE_LIST, false, null);
};
goog.inherits(Blockly.GqlListFlydown, Blockly.FieldFlydown);
Blockly.GqlListFlydown.prototype.fieldCSSClassName = 'blocklyListField';
Blockly.GqlListFlydown.prototype.flyoutCSSClassName = 'blocklyListFlydown';
Blockly.GqlListFlydown.prototype.flydownBlocksXML_ = Blockly.GqlPairFlydown.prototype.flydownBlocksXML_;
