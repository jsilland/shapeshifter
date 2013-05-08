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

import com.turn.shapeshifter.ShapeshifterProtos.JsonSchema;
import com.turn.shapeshifter.ShapeshifterProtos.JsonType;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;

/**
 * {@code AutoSchema} instances are schemas of the simplest kind. They can be
 * used in contexts where the customization of the external representation of a
 * type is either not needed or undesirable. As such, the implementation of
 * such a schema's {@link Parser} and {@link Serializer} are lean and fast.
 *
 * <p>Default conversions and conventions still apply for the {@link Parser} and
 * {@link Serializer} returned by an instance of this class. Namely:
 *
 * <ul>
 *	<li>Field names are converted between Protocol Buffer
 *	{@code lower_underscore} to JSON's {@code camelCase}.
 *	<li>Enum names are converted between {@code PROTO_FORMAT} and
 *	 {@code jsonFormat}.
 *	<li>Empty arrays and objects without properties are ignored
 * </ul>
 *
 * <p>An instance of {@code AutoSchema} is anonymous and defined uniquely by
 * the {@link Descriptor} from which it was instantiated. Since
 * Protocol Buffer message descriptors are immutable, instances of this class
 * are prime candidates for being cached throughout the lifetime of an
 * application.
 *
 * <p>This class takes its name from its use within the {@code Shapeshifter}
 * library, where instances of this class are generated on the fly as other
 * schema types find the need to handle types for which no schema was defined
 * by the user.
 *
 * <p>Instances of this class recursively build schemas for all message types
 * referred from the root descriptor. This is impossible to achieve when a loop
 * occurs in the descriptors. Assuming an instance of this class is built with
 * the descriptor for a message named ProtoA, an exception will be thrown when:
 *
 * <ul>
 *	<li>ProtoA contains a field of type ProtoA
 *	<li>ProtoA contains a field a type ProtoB, and ProtoB contains a field of
 *	type ProtoA.
 *	<li>More generally when there exists a chain of fields between ProtoA and
 *	ProtoX, and ProtoX refers to any message type that is part of that chain,
 *	including ProtoA and ProtoX themselves.
 * </ul>
 *
 * @author jsilland
 */
public class AutoSchema implements Schema {

	// This is the presumed case format for the values of an enum
	// defined in a protocol buffer.
	static final CaseFormat PROTO_ENUM_CASE_FORMAT = CaseFormat.UPPER_UNDERSCORE;
	static final CaseFormat JSON_ENUM_CASE_FORMAT = CaseFormat.LOWER_CAMEL;

	static final CaseFormat PROTO_FIELD_CASE_FORMAT = CaseFormat.LOWER_UNDERSCORE;
	static final CaseFormat JSON_FIELD_CASE_FORMAT = CaseFormat.LOWER_CAMEL;

	private final Descriptors.Descriptor descriptor;
	private final Parser parser;
	private final Serializer serializer;

	/**
	 * Creates a new instance of this class that will use the given descriptor.
	 */
	public static AutoSchema of(Descriptors.Descriptor descriptor) {
		return new AutoSchema(Preconditions.checkNotNull(descriptor));
	}

	/**
	 * Package-private exhaustive constructor.
	 *
	 * @param descriptor the descriptor
	 */
	AutoSchema(Descriptor descriptor) {
		this.descriptor = descriptor;
		Preconditions.checkArgument(!isDescriptorLooping(descriptor),
				"Auto-schemas cannot describe types that have self-references");
		this.parser = new AutoParser(descriptor);
		this.serializer = new AutoSerializer(descriptor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializer getSerializer() {
		return serializer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Parser getParser() {
		return parser;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonSchema getJsonSchema(ReadableSchemaRegistry registry) throws JsonSchemaException {
		JsonSchema.Builder jsonSchemaBuilder = JsonSchema.newBuilder();
		jsonSchemaBuilder.setType(JsonType.OBJECT);
		for (FieldDescriptor field : descriptor.getFields()) {
			JsonSchema.Builder property = JsonSchema.newBuilder();
			property.setName(PROTO_FIELD_CASE_FORMAT.to(JSON_FIELD_CASE_FORMAT, field.getName()));

			if (field.hasDefaultValue()) {
				if (field.getType().equals(Type.ENUM)) {
					EnumValueDescriptor defaultValue = (EnumValueDescriptor) field.getDefaultValue();
					property.setDefault(PROTO_ENUM_CASE_FORMAT.to(
							JSON_ENUM_CASE_FORMAT, defaultValue.getName()));
				} else {
					property.setDefault(field.getDefaultValue().toString());
				}
			}
			
			if (field.isRequired()) {
				property.setRequired(true);
			}
			
			
			if (field.isRepeated()) {
				property.setType(JsonType.ARRAY);
				if (field.getType().equals(Type.MESSAGE)) {
					try {
						property.setItems(
								registry.get(field.getMessageType()).getJsonSchema(registry));
					} catch (SchemaObtentionException soe) {
						throw new JsonSchemaException(soe);
					}
				} else {
					property.setItems(
							JsonSchema.newBuilder().setType(getFieldType(field)));
					if (field.getType().equals(Type.ENUM)) {
						for (EnumValueDescriptor enumValue : field.getEnumType().getValues()) {
							property.addEnum(PROTO_ENUM_CASE_FORMAT.to(
									JSON_ENUM_CASE_FORMAT, enumValue.getName()));
						}
					}
				}
			} else {
				if (field.getType().equals(Type.MESSAGE)) {
					try {
						property.mergeFrom(registry.get(field.getMessageType())
								.getJsonSchema(registry));
					} catch (SchemaObtentionException soe) {
						throw new JsonSchemaException(soe);
					}
				} else {
					property.setType(getFieldType(field));
					if (field.getType().equals(Type.ENUM)) {
						for (EnumValueDescriptor enumValue : field.getEnumType().getValues()) {
							property.addEnum(PROTO_ENUM_CASE_FORMAT.to(
									JSON_ENUM_CASE_FORMAT, enumValue.getName()));
						}
					}
				}
			}

			jsonSchemaBuilder.addProperties(property);
		}
		return jsonSchemaBuilder.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Descriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Returns the reified jon type of the given protocol buffer field.
	 *
	 * @param field the field to determine the JSON type of
	 */
	public static ShapeshifterProtos.JsonType getReifiedFieldType(FieldDescriptor field) {
		Preconditions.checkNotNull(field);
		return field.isRepeated() ? JsonType.ARRAY : getFieldType(field);
	}

	/**
	 * Returns the JSON schema type of a given protocol buffer field.
	 *
	 * @param field the field to obtain the JSON type for
	 */
	private static ShapeshifterProtos.JsonType getFieldType(FieldDescriptor field) {
		Preconditions.checkNotNull(field);
		switch (field.getJavaType()) {
			case BOOLEAN:
				return JsonType.BOOLEAN;
			case BYTE_STRING:
			case STRING:
			case ENUM:
				return JsonType.STRING;
			case DOUBLE:
			case FLOAT:
				return JsonType.NUMBER;
			case INT:
			case LONG:
				return JsonType.INTEGER;
			case MESSAGE:
				return JsonType.OBJECT;
			default:
				break;
		}
		throw new IllegalStateException();
	}

	static boolean isDescriptorLooping(Descriptors.Descriptor descriptor) {
		return ProtoDescriptorGraph.of(descriptor).isLooping();
	}
}