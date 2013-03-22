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

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

/**
 * Implementation of {@link Serializer} based on the configuration of a
 * {@link NamedSchema}.
 *
 * @author jsilland
 */
class NamedSchemaSerializer implements Serializer {

	private final NamedSchema schema;

	NamedSchemaSerializer(NamedSchema schema) {
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
	@Override
	public JsonNode serialize(Message message, ReadableSchemaRegistry registry)
			throws SerializationException {
		ObjectNode object = new ObjectNode(JsonNodeFactory.instance);
		for (Map.Entry<String, String> constant : schema.getConstants().entrySet()) {
			object.put(constant.getKey(), constant.getValue());
		}
		for (Map.Entry<String, FieldDescriptor> fieldEntry : schema.getFields().entrySet()) {
			if (schema.getSkippedFields().contains(fieldEntry.getKey())) {
				continue;
			}
			FieldDescriptor field = fieldEntry.getValue();
			if (field.isRepeated()) {
				int count = message.getRepeatedFieldCount(field);
				if (count > 0) {
					if (schema.getMappings().containsKey(field.getName())) {
						ObjectNode objectNode = serializeMappedField(
								message, registry, field, count);
						if (objectNode.size() > 0) {
							object.put(schema.getPropertyName(field.getName()), objectNode);
						}
					} else {
						ArrayNode array = serializeRepeatedField(message, registry, field, count);
						if (array.size() > 0) {
							object.put(schema.getPropertyName(field.getName()), array);
						}
					}
				}
			} else if (message.hasField(field)) {
				Object value = message.getField(field);
				JsonNode fieldNode = serializeValue(value, field, registry);
				object.put(schema.getPropertyName(field.getName()), fieldNode);
			}
		}
		if (object.size() == 0) {
			return NullNode.instance;
		}
		return object;
	}

	/**
	 * Serializes a repeated mapped field.
	 *
	 * @param message the message being serialized
	 * @param registry a registry of schemas, for enclosed object types
	 * @param field the descriptor of the repeated field to serialize
	 * @param count the count of repeated items in the field
	 * @return the JSON representation of the serialized mapped field
	 * @throws SerializationException
	 * @see {@link NamedSchema#mapRepeatedField(String, String)}
	 */
	private ObjectNode serializeMappedField(Message message, ReadableSchemaRegistry registry,
			FieldDescriptor field, int count) throws SerializationException {
		ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
		for (int i = 0; i < count; i++) {
			Message value = (Message) message.getRepeatedField(field, i);
			String key = String.valueOf(
					value.getField(schema.getMappings().get(field.getName())));
			JsonNode valueNode = serializeValue(value, field, registry);
			if (!valueNode.isNull() && key != null) {
				objectNode.put(key, valueNode);
			}
		}
		return objectNode;
	}

	/**
	 * Serializes a repeated field.
	 *
	 * @param message the message being serialized
	 * @param registry a registry of schemas, for enclosed object types
	 * @param field the descriptor of the repeated field to serialize
	 * @param count the count of repeated items in the field
	 * @return the JSON representation of the serialized
	 * @throws SerializationException
	 */
	private ArrayNode serializeRepeatedField(Message message, ReadableSchemaRegistry registry,
			FieldDescriptor field, int count) throws SerializationException {
		ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
		for (int i = 0; i < count; i++) {
			Object value = message.getRepeatedField(field, i);
			JsonNode valueNode = serializeValue(value, field, registry);
			if (!valueNode.isNull()) {
				array.add(valueNode);
			}
		}
		return array;
	}

	/**
	 * Returns the JSON representation of the value of a message's field.
	 *
	 * @param value the value to represent in JSON
	 * @param field the descriptor of the value's field.
	 * @param schemas a container for object schemas to use for formatting
	 * fields that refer to other messages
	 * @throws SerializationException
	 */
	private JsonNode serializeValue(Object value, FieldDescriptor field,
			ReadableSchemaRegistry schemas) throws SerializationException {
		JsonNode valueNode = NullNode.instance;
		if (schema.getTransforms().containsKey(field.getName())) {
			return schema.getTransforms().get(field.getName()).serialize(value);
		}
		switch (field.getType()) {
		case BOOL:
			valueNode = BooleanNode.valueOf((Boolean) value);
			break;
		case BYTES:
			break;
		case DOUBLE:
			valueNode = new DoubleNode((Double) value);
			break;
		case ENUM:
			EnumValueDescriptor enumValueDescriptor = (EnumValueDescriptor) value;
			String enumValue = NamedSchema.PROTO_ENUM_CASE_FORMAT.to(schema.getEnumCaseFormat(),
					enumValueDescriptor.getName());
			valueNode = new TextNode(enumValue);
			break;
		case FLOAT:
			valueNode = new DoubleNode((Float) value);
			break;
		case GROUP:
			break;
		case FIXED32:
		case INT32:
		case SFIXED32:
		case SINT32:
		case UINT32:
			valueNode = new IntNode((Integer) value);
			break;
		case FIXED64:
		case INT64:
		case SFIXED64:
		case SINT64:
		case UINT64:
			valueNode = new LongNode((Long) value);
			break;
		case MESSAGE:
			Message messageValue = (Message) value;
			Schema subSchema = null;
			if (schema.getSubObjectsSchemas().containsKey(field.getName())) {
				String schemaName = schema.getSubObjectsSchemas().get(field.getName());
				if (schemas.contains(schemaName)) {
					subSchema = schemas.get(schemaName);
				} else {
					throw new IllegalStateException();
				}
			} else {
				try {
					subSchema = schemas.get(field.getMessageType());
				} catch (SchemaObtentionException soe) {
					throw new SerializationException(soe);
				}
			}
			valueNode = subSchema.getSerializer().serialize(messageValue, schemas);
			valueNode = valueNode.size() == 0 ? NullNode.instance : valueNode;
			break;
		case STRING:
			valueNode = new TextNode((String) value);
			break;
		default:
			break;
		}
		return valueNode;
	}
}