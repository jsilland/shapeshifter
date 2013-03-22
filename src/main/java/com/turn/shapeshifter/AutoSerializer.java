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
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * Default implementation of {@link Serializer}.
 *
 * @author jsilland
 */
final class AutoSerializer implements Serializer {

	private final Descriptors.Descriptor descriptor;

	AutoSerializer(Descriptors.Descriptor descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * {@inheritDoc}
	 * @throws SerializationException
	 */
	@Override
	public JsonNode serialize(Message message, ReadableSchemaRegistry registry)
			throws SerializationException {
		ObjectNode object = new ObjectNode(JsonNodeFactory.instance);

		for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
			String propertyName = AutoSchema.PROTO_FIELD_CASE_FORMAT
					.to(AutoSchema.JSON_FIELD_CASE_FORMAT, field.getName());
			if (field.isRepeated()) {
				if (message.getRepeatedFieldCount(field) > 0) {
						ArrayNode array = serializeRepeatedField(message, field, registry);
						if (array.size() != 0) {
							object.put(propertyName, array);
						}
				}
			} else if (message.hasField(field)) {
				Object value = message.getField(field);
				JsonNode fieldNode = serializeValue(value, field, registry);
				if (!fieldNode.isNull()) {
					object.put(propertyName, fieldNode);
				}
			}
		}

		if (object.size() == 0) {
			return NullNode.instance;
		}

		return object;
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
	private ArrayNode serializeRepeatedField(Message message,
			FieldDescriptor field, ReadableSchemaRegistry registry)
					throws SerializationException {
		ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
		int count = message.getRepeatedFieldCount(field);
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
			ReadableSchemaRegistry registry) throws SerializationException {
		JsonNode valueNode = NullNode.instance;
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
			String enumValue = enumValueDescriptor.getName();
			String convertedValue = AutoSchema.PROTO_ENUM_CASE_FORMAT
					.to(AutoSchema.JSON_ENUM_CASE_FORMAT, enumValue);
			valueNode = new TextNode(convertedValue);
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
			try {
				valueNode = registry.get(messageValue.getDescriptorForType())
						.getSerializer().serialize(messageValue, registry);
				valueNode = valueNode.size() == 0 ? NullNode.instance : valueNode;
			} catch (SchemaObtentionException soe) {
				throw new SerializationException(soe);
			}
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