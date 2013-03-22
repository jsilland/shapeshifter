/**
 * Copyright 2012, 2013 Turn, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turn.shapeshifter;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

/**
 * Implementation of {@link Parser} based on the configuration contained in a
 * {@link NamedSchema}.
 *
 * @author jsilland
 */
public class NamedSchemaParser implements Parser {

	private final NamedSchema schema;

	NamedSchemaParser(NamedSchema schema) {
		this.schema = schema;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This variation allows for the inclusion of schemas for serializing
	 * sub-objects that may appear in {@code message}. If no suitable schema
	 * is found in the registry, a schema with default settings is generated
	 * on the fly using {@link
	 * SchemaSource#get(com.google.protobuf.Descriptors.Descriptor)}.
	 *
	 */
	public Message parse(JsonNode node, ReadableSchemaRegistry registry) throws ParsingException {
		Message.Builder builder = DynamicMessage.newBuilder(schema.getDescriptor());

		for (Map.Entry<String, FieldDescriptor> fieldEntry : schema.getFields().entrySet()) {
			String fieldName = schema.getPropertyName(fieldEntry.getKey());
			FieldDescriptor field = fieldEntry.getValue();
			if (node.has(fieldName) && !node.get(fieldName).isNull()) {
				JsonNode valueNode = node.get(fieldName);
				if (field.isRepeated()) {
					if (schema.getMappings().containsKey(field.getName())) {
						parseMappedField(registry, builder, fieldName, field, valueNode);
					} else {
						parseRepeatedField(registry, builder, fieldName, field, valueNode);
					}
				} else {
					Object value = parseValue(valueNode, field, registry);
					if (value != null) {
						builder.setField(field, value);
					}
				}
			}
		}

		return builder.build();
	}

	/**
	 * Parses a repeated mapped field.
	 *
	 * @param registry a registry of schemas, used for parsing enclosed objects
	 * @param builder the builder in which the parsed field should be set
	 * @param field the descriptor of the repeated field being parsed
	 * @param fieldName the JSON name of the field
	 * @param valueNode the JSON node being parsed
	 * @throws ParsingException
	 * @see NamedSchema#mapRepeatedField(String,String)
	 */
	private void parseMappedField(ReadableSchemaRegistry registry, Message.Builder builder,
			String fieldName, FieldDescriptor field, JsonNode valueNode) throws ParsingException {
		if (!valueNode.isObject()) {
			throw new IllegalArgumentException(
					"Field '" + fieldName +
					"' is expected to be an object, but was " +
					valueNode.asToken());
		}
		ObjectNode objectNode = (ObjectNode) valueNode;
		Iterator<Map.Entry<String, JsonNode>> subObjectsIterator =
				objectNode.fields();
		while (subObjectsIterator.hasNext()) {
			Map.Entry<String, JsonNode> subObject = subObjectsIterator.next();
			Message message = (Message) parseValue(
					subObject.getValue(), field, registry);
			DynamicMessage.Builder dynamicMessage = DynamicMessage.newBuilder(
					field.getMessageType());
			dynamicMessage.mergeFrom(message);
			dynamicMessage.setField(
					schema.getMappings().get(field.getName()), subObject.getKey());
			builder.addRepeatedField(field, dynamicMessage.build());
		}
	}

	/**
	 * Parses a repeated field.
	 *
	 * @param registry a registry of schemas, used for parsing enclosed objects
	 * @param builder the builder in which the parsed field should be set
	 * @param fieldName the JSON name of the field
	 * @param field the descriptor of the repeated field being parsed
	 * @param valueNode the JSON node being parsed
	 * @throws ParsingException
	 */
	private void parseRepeatedField(ReadableSchemaRegistry registry, Message.Builder builder,
			String fieldName, FieldDescriptor field, JsonNode valueNode) throws ParsingException {
		if (!valueNode.isArray()) {
			throw new IllegalArgumentException(
					"Field '" + fieldName +
					"' is expected to be an array, but was " +
					valueNode.asToken());
		}
		ArrayNode array = (ArrayNode) valueNode;
		if (array.size() != 0) {
			for (JsonNode item : array) {
				Object value = parseValue(item, field, registry);
				if (value != null) {
					builder.addRepeatedField(field, value);
				}
			}
		}
	}

	/**
	 * Reads the value of a JSON node and returns the corresponding Java object.
	 *
	 * @param jsonNode the node to convert to a Java object
	 * @param field the protocol buffer field to which the resulting value will be assigned
	 * @param registry a schema registry in which enclosed message fields are expected to
	 * be defined
	 * @return a Java object representing the field's value
	 * @throws ParsingException In case the JSON node cannot be parsed
	 * @throws UnmappableValueException in case the JSON value node cannot be converted
	 * to a Java object that could be assigned to {@code field}
	 */
	private Object parseValue(JsonNode jsonNode, FieldDescriptor field,
			ReadableSchemaRegistry registry) throws ParsingException {
		Object value = null;
		if (schema.getTransforms().containsKey(field.getName())) {
			return schema.getTransforms().get(field.getName()).parse(jsonNode);
		}
		switch (field.getType()) {
		case BOOL:
			JsonTokens.checkJsonValueConformance(jsonNode, JsonTokens.VALID_BOOLEAN_TOKENS);
			value = Boolean.valueOf(jsonNode.asBoolean());
			break;
		case BYTES:
			break;
		case DOUBLE:
			value = new Double(jsonNode.asDouble());
			break;
		case ENUM:
			JsonTokens.checkJsonValueConformance(jsonNode, JsonTokens.VALID_ENUM_TOKENS);
			String enumValue = schema.getEnumCaseFormat().to(
					NamedSchema.PROTO_ENUM_CASE_FORMAT, jsonNode.asText());
			value = field.getEnumType().findValueByName(enumValue);
			break;
		case FLOAT:
			JsonTokens.checkJsonValueConformance(jsonNode, JsonTokens.VALID_FLOAT_TOKENS);
			value = new Float(jsonNode.asDouble());
			break;
		case GROUP:
			break;
		case FIXED32:
		case INT32:
		case SFIXED32:
		case SINT32:
		case UINT32:
			JsonTokens.checkJsonValueConformance(jsonNode, JsonTokens.VALID_INTEGER_TOKENS);
			value = new Integer(jsonNode.asInt());
			break;
		case FIXED64:
		case INT64:
		case SFIXED64:
		case SINT64:
		case UINT64:
			JsonTokens.checkJsonValueConformance(jsonNode, JsonTokens.VALID_INTEGER_TOKENS);
			value = new Long(jsonNode.asLong());
			break;
		case MESSAGE:
			if (!jsonNode.isObject()) {
				throw new IllegalArgumentException(
						"Expected to parse object, found value of type " + jsonNode.asToken());
			}
			Schema subSchema = null;
			if (schema.getSubObjectsSchemas().containsKey(field.getName())) {
				String schemaName = schema.getSubObjectsSchemas().get(field.getName());
				if (registry.contains(schemaName)) {
					subSchema = registry.get(schemaName);
				} else {
					throw new IllegalStateException();
				}
			} else {
				try {
					subSchema = registry.get(field.getMessageType());
				} catch (SchemaObtentionException soe) {
					throw new ParsingException(soe);
				}
			}
			value = subSchema.getParser().parse(jsonNode, registry);
			break;
		case STRING:
			JsonTokens.checkJsonValueConformance(jsonNode, JsonTokens.VALID_STRING_TOKENS);
			value = jsonNode.asText();
			break;
		default:
			break;
		}
		return value;
	}
}