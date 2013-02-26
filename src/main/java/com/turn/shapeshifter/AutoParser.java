/**
 * Copyright 2012, 2013 Turn, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turn.shapeshifter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * Default implementation of {@link Parser}.
 *
 * @author jsilland
 */
final class AutoParser implements Parser {

	private final Descriptors.Descriptor descriptor;

	/**
	 * Creates a new instance of this class with the given message descriptor.
	 */
	AutoParser(Descriptors.Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Message parse(JsonNode node, ReadableSchemaRegistry registry) throws ParsingException {
		Message.Builder builder = DynamicMessage.newBuilder(descriptor);

		for (FieldDescriptor field : descriptor.getFields()) {
			String fieldName = AutoSchema.PROTO_FIELD_CASE_FORMAT.to(
					AutoSchema.JSON_FIELD_CASE_FORMAT, field.getName());
			if (node.has(fieldName) && !node.get(fieldName).isNull()) {
				JsonNode valueNode = node.get(fieldName);
				if (field.isRepeated()) {
					parseRepeatedField(builder, fieldName, field, valueNode, registry);
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
	 * Parses a repeated field.
	 *
	 * @param builder the builder in which the parsed field should be set
	 * @param fieldName the JSON name of the field
	 * @param field the descriptor of the repeated field being parsed
	 * @param valueNode the JSON node being parsed
	 * @param registry used as a source of schemas generated on the fly for
	 * sub-objects
	 * @throws ParsingException
	 */
	private void parseRepeatedField(Message.Builder builder,
			String fieldName, FieldDescriptor field, JsonNode valueNode,
			ReadableSchemaRegistry registry) throws ParsingException {
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
	 * Reads the value of a JSON node and returns the corresponding Java
	 * object.
	 *
	 * @param jsonNode the node to convert to a Java object
	 * @param field the protocol buffer field to which the resulting value will
	 * be assigned
	 * @param registry used as a source of schemas generated on the fly for
	 * sub-objects
	 * @return a Java object representing the field's value
	 * @throws ParsingException
	 * @throws UnmappableValueException in case the JSON value node cannot be
	 * converted
	 * to a Java object that could be assigned to {@code field}
	 */
	private Object parseValue(JsonNode jsonNode, FieldDescriptor field,
			ReadableSchemaRegistry registry) throws ParsingException {
		Object value = null;
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
			String enumValue = jsonNode.asText();
			String convertedValue = AutoSchema.JSON_ENUM_CASE_FORMAT
					.to(AutoSchema.PROTO_ENUM_CASE_FORMAT, enumValue);
			value = field.getEnumType().findValueByName(convertedValue);
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
			if (jsonNode.size() != 0) {
				try {
					value = registry.get(field.getMessageType()).getParser()
							.parse(jsonNode, registry);
				} catch (SchemaObtentionException soe) {
					throw new ParsingException(soe);
				}
			}
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