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

import java.util.Map;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;

/**
 * A configurable, immutable schema with a name.
 *
 * <p>This implementation of {@link Schema} is highly configurable and provides
 * features and settings that allow the translation between Protocol Buffers
 * and JSON to be customized at will. Here is the most basic example of usage:
 * <pre> {@code
 *
 * MyProto proto = MyProto.newBuilder().setFoo("bar").build();
 * Schema.of(MyProto.getDescriptor()).getSerializer().serialize(proto);}</pre>
 *
 * <p>This will return a {@code JsonNode} which looks like:<pre> {@code
 *
 * { "foo": "bar" }}</pre>
 *
 * <p>By default, this implementation implements the following bahavior:
 *
 * <ul>
 * <li>All fields are considered during serialization and parsing phases.
 * <li>Fields that are not explicitly set, empty repeated fields and objects in
 *	which no fields are set are ignored.
 * <li>Fields names are converted between {@code lower_undesrcore} and
 * {@code lowerCamel}.
 * <li>Proto enums values are serialized as strings, formatted as
 * {@code lowerCamel}.
 * </ul>
 *
 * <p>{@code NamedSchema}s are configurable to customize the external JSON
 * representation of a given message:
 *
 * <ul>
 *	<li>{@link #addConstant(String, String)} adds a constant field to the
 *	output.
 *	<li>{@link #skip(String...)} lets you hide fields defined in the protocol
 *	buffer from the outside world.
 *	<li>{@link #substitute(String, String)} lets you change the name of a
 *	protocol buffer field to something more appropriate
 *	<li>{@link #enumCaseFormat(CaseFormat)} allows for changing the case format
 *	used for serializing and parsing enums.
 *	<li>{@link #transform(String, Transformer)} provides full control over
 *	how the value of a field is parsed or serialized.
 *	<li>{@link #mapRepeatedField(String, String)} lets you implement dynamic
 *	JSON objects, where a property's key will be taken from a given field of
 *	a repeated object.
 *	<li>{@link #useSchema(String, String)} lets you specify a schema for fields
 *	that reference other Protocol Buffer message types.
 * </ul>
 *
 * <p><b>Warning</b>: Schema instances are always immutable. Configuration
 * methods such as {@link #skip(String...)} or {@link
 * #substitute(String, String)} always return a new instance and have no effect
 * on the instance they are invoked on. This makes Schemas thread-safe and
 * ideal to be stored as {@code static final} constants:<pre> {@code
 *
 * Schema schema = Schema.of(MyProto.getDescriptorForType());
 * schema.skip("foo"); // don't do this
 * schema.getSerializer().serialize(proto); // will include 'foo'} </pre>
 *
 * @author jsilland
 *
 */
public class NamedSchema implements Schema {

	// This is the presumed case format for the values of an enum
	// defined in a protocol buffer.
	// TODO(jsilland): make this configurable
	static final CaseFormat PROTO_ENUM_CASE_FORMAT = CaseFormat.UPPER_UNDERSCORE;

	private final Descriptor descriptor;
	private final String name;
	private final ImmutableMap<String, FieldDescriptor> fields;
	private final ImmutableSet<String> skippedFields;
	private final ImmutableMap<String, String> constants;
	private final CaseFormat enumCaseFormat;
	private final ImmutableMap<String, String> substitutions;
	private final ImmutableMap<String, FormatTransformer> transforms;
	private final ImmutableMap<String, FieldDescriptor> mappings;
	private final ImmutableMap<String, String> descriptions;
	private final ImmutableMap<String, String> subObjectSchemas;
	private final ImmutableMap<String, String> formats;

	/**
	 * Private constructor. External clients should use
	 * {@link #of(Descriptor)}.
	 *
	 * @param descriptor the message descriptor this schema is intended to
	 * format
	 * @param skippedFields the set of field names that should be ignored for
	 * the purposes of serialization, parsing and documentation
	 * @param constants a map of constant string pairs that will be part of the
	 * serialization of a message
	 * @param enumCaseFormat the case format to use for the external
	 * representation of enumerated values
	 * @param substitutions the substitution of field names
	 * @param transforms the set of content transformations to perform on
	 * fields' values
	 * @param mappings indicates which repeated object fields should be
	 * externally represented as objects with dynamic keys. Keys in this map
	 * indicate the field's name and values indicate the field to use as a key
	 * in the sub-object
	 * @param descriptions contains the documentation strings for each field
	 * @param subObjectSchemas the mapping of references to other schemas, in
	 * case the field is an object
	 * @param formats the fields' formats metadata
	 */
	private NamedSchema(Descriptor descriptor,
			String name,
			ImmutableSet<String> skippedFields,
			ImmutableMap<String, String> constants,
			CaseFormat enumCaseFormat,
			ImmutableMap<String, String> substitutions,
			ImmutableMap<String, FormatTransformer> transforms,
			ImmutableMap<String, FieldDescriptor> mappings,
			ImmutableMap<String, String> descriptions,
			ImmutableMap<String, String> subObjectSchemas,
			ImmutableMap<String, String> formats) {
		this.descriptor = descriptor;
		this.name = name;
		ImmutableMap.Builder<String, FieldDescriptor> fieldsBuilder = ImmutableMap.builder();
		for (FieldDescriptor field : descriptor.getFields()) {
			fieldsBuilder.put(field.getName(), field);
		}
		fields = fieldsBuilder.build();
		this.skippedFields = skippedFields;
		this.constants = constants;
		this.enumCaseFormat = enumCaseFormat;
		this.substitutions = substitutions;
		this.transforms = transforms;
		this.mappings = mappings;
		this.descriptions = descriptions;
		this.subObjectSchemas = subObjectSchemas;
		this.formats = formats;
	}

	/**
	 * Returns a {@code Schema} that will serialize and parse {@link Message}
	 * corresponding to the given {@link Descriptor}.
	 *
	 * @param descriptor a descriptor of message.
	 * @see Message#getDescriptorForType()
	 */
	public static NamedSchema of(Descriptor descriptor, String name) {
		Preconditions.checkNotNull(descriptor, "The descriptor should not be null");
		Preconditions.checkNotNull(name, "The name should not be null");
		Preconditions.checkArgument(!name.isEmpty(), "The name should not be empty");
		return new NamedSchema(descriptor,
				name,
				ImmutableSet.<String>of(),
				ImmutableMap.<String, String>of(),
				CaseFormat.LOWER_CAMEL,
				ImmutableMap.<String, String>of(),
				ImmutableMap.<String, FormatTransformer>of(),
				ImmutableMap.<String, FieldDescriptor>of(),
				ImmutableMap.<String, String>of(),
				ImmutableMap.<String, String>of(),
				ImmutableMap.<String, String>of());
	}

	/**
	 * Returns a schema that will not ignore the given field names for
	 * serialization and parsing purposes.
	 *
	 * @param names the names of the fields to ignore
	 */
	public NamedSchema skip(String... names) {
		Preconditions.checkNotNull(names);
		for (String name : names) {
			Preconditions.checkArgument(has(name));
		}
		ImmutableSet.Builder<String> skippedCopy = ImmutableSet.builder();
		skippedCopy.addAll(skippedFields);
		skippedCopy.add(names);
		return new NamedSchema(descriptor, name, skippedCopy.build(), constants, enumCaseFormat,
				substitutions, transforms, mappings, descriptions, subObjectSchemas, formats);
	}

	/**
	 * Returns a schema that will output an extra constant field in the JSON
	 * serialization.
	 *
	 * @param key the key of the field to output
	 * @param value the value of the field to output
	 */
	public NamedSchema addConstant(String key, String value) {
		Preconditions.checkNotNull(key);
		Preconditions.checkArgument(!has(key));
		ImmutableMap.Builder<String, String> constantsCopy = ImmutableMap.builder();
		constantsCopy.putAll(constants);
		constantsCopy.put(key, value);
		return new NamedSchema(descriptor, name, skippedFields, constantsCopy.build(),
				enumCaseFormat, substitutions, transforms, mappings, descriptions,
				subObjectSchemas, formats);
	}

	/**
	 * Returns a schema that will use the specified casing to format
	 * enumeration values.
	 *
	 * @param caseFormat the desired external case format
	 */
	public NamedSchema enumCaseFormat(CaseFormat caseFormat) {
		return new NamedSchema(descriptor, name, skippedFields, constants, caseFormat,
				substitutions, transforms, mappings, descriptions, subObjectSchemas, formats);
	}

	/**
	 * Returns a schema that will substitute a given field name in the
	 * serialized JSON.
	 *
	 * <p>The substituted name remains subject to the configured case formatting.
	 *
	 * @param fieldName the name of the field to substitute
	 * @param substitution the string to use for the substitution
	 */
	public NamedSchema substitute(String fieldName, String substitution) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkArgument(has(fieldName));
		Preconditions.checkNotNull(substitution);
		ImmutableMap.Builder<String, String> substitutionsCopy = ImmutableMap.builder();
		substitutionsCopy.putAll(substitutions);
		substitutionsCopy.put(fieldName, substitution);
		return new NamedSchema(descriptor, name, skippedFields, constants, enumCaseFormat,
				substitutionsCopy.build(), transforms, mappings, descriptions, subObjectSchemas, formats);
	}

	/**
	 * Returns a schema that will transforms the given field's value.
	 *
	 * @param fieldName the name of the field to transform
	 * @param transformer the transformation to apply
	 */
	public NamedSchema transform(String fieldName, Transformer transformer) {
		return transform(fieldName, Transformers.format(transformer));
	}
	
	/**
	 * Returns a schema that will transforms the given field's value, with an
	 * added format specified in the resulting JSON Schema.
	 *
	 * @param fieldName the name of the field to transform
	 * @param transformer the transformation to apply
	 */
	public NamedSchema transform(String fieldName, FormatTransformer transformer) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkArgument(has(fieldName));
		Preconditions.checkNotNull(transformer);
		ImmutableMap.Builder<String, FormatTransformer> transformsCopy = ImmutableMap.builder();
		transformsCopy.putAll(transforms);
		transformsCopy.put(fieldName, transformer);
		return new NamedSchema(descriptor, name, skippedFields, constants, enumCaseFormat,
				substitutions, transformsCopy.build(), mappings, descriptions,
				subObjectSchemas, formats);
	}

	/**
	 * Returns a new schema that will serialize repeated objects as a JSON
	 * object instead of a JSON array.
	 *
	 * <p>This transformation is useful in cases where the source object
	 * contains a list of objects identified by a given field. Normally, such
	 * a list would be serialized as:<pre>{@code
	 *
	 * "users": [
	 *		{"username": "lrichie", name: "Lionel Richie"},
	 *		{"username": "stwain", "name": "Shania Twain"}
	 * ]}</pre>
	 *
	 * <p>A common JSON idiom is to represent such collections as pseudo-maps,
	 *	i.e.objects with non-predefined keys:<pre>{@code
	 *
	 * "users": {
	 *	"lrichie": {"name": "Lionel Richie"},
	 *	"stwain": {"name": "Shania Twain"}
	 * }}</pre>
	 *
	 * <p>To achieve this effect, call this method with {@code "users"} and
	 * {@code "username"} as parameters, respectively.
	 *
	 * @param fieldName Must point to a repeated object field in this schema
	 * @param keyFieldName Must be a string field in the repeated field
	 */
	public NamedSchema mapRepeatedField(String fieldName, String keyFieldName) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkArgument(has(fieldName));
		Preconditions.checkNotNull(keyFieldName);
		FieldDescriptor field = fields.get(fieldName);
		Preconditions.checkArgument(field.isRepeated() && Type.MESSAGE.equals(field.getType()));
		Descriptor fieldDescriptor = field.getMessageType();
		boolean found = false;
		FieldDescriptor keyField = null;
		for (FieldDescriptor subField : fieldDescriptor.getFields()) {
			if (subField.getName().equals(keyFieldName)) {
				found = true;
				keyField = subField;
			}
		}
		if (!found || keyField.isRepeated() || !Type.STRING.equals(keyField.getType())) {
			throw new IllegalArgumentException();
		}
		ImmutableMap.Builder<String, FieldDescriptor> mappingsCopy =
				ImmutableMap.<String, FieldDescriptor>builder();
		mappingsCopy.putAll(mappings);
		mappingsCopy.put(fieldName, keyField);
		return new NamedSchema(descriptor, name, skippedFields, constants, enumCaseFormat,
				substitutions, transforms, mappingsCopy.build(), descriptions,
				subObjectSchemas, formats);
	}

	/**
	 * Returns a new schema in which {@code fieldName} will be documented.
	 *
	 * @param fieldName the name of the field to document
	 * @param description the documentation of the field
	 */
	public NamedSchema describe(String fieldName, String description) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkNotNull(description);
		Preconditions.checkState(has(fieldName));
		ImmutableMap.Builder<String, String> descriptionsCopy = ImmutableMap.builder();
		descriptionsCopy.putAll(descriptions);
		descriptionsCopy.put(fieldName, description);
		return new NamedSchema(descriptor, name, skippedFields, constants, enumCaseFormat,
				substitutions, transforms, mappings, descriptionsCopy.build(), subObjectSchemas, formats);
	}

	/**
	 * Returns a new schema in which the given field will be delegated to the
	 * specified schema.
	 *
	 * <p>The field must be a submessage and its type must match that of
	 * {@code schema}.
	 *
	 * @param fieldName the name of the field, which must be of object type
	 * @param schemaName the name of the schema to delegate this field to
	 */
	public NamedSchema useSchema(String fieldName, String schemaName) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkNotNull(schemaName);
		Preconditions.checkState(has(fieldName));
		FieldDescriptor field = fields.get(fieldName);
		Preconditions.checkArgument(Type.MESSAGE.equals(field.getType()));
		ImmutableMap.Builder<String, String> subObjectSchemasCopy = ImmutableMap.builder();
		subObjectSchemasCopy.putAll(subObjectSchemas);
		subObjectSchemasCopy.put(fieldName, schemaName);
		return new NamedSchema(descriptor, name, skippedFields, constants, enumCaseFormat,
				substitutions, transforms, mappings, descriptions, subObjectSchemasCopy.build(), formats);
	}
	
	/**
	 * Returns a new schema in which the given field with have a format
	 * attribute.
	 * 
	 * @param fieldName the name of the field to specify a format for
	 * @param format the format of the property
	 */
	public NamedSchema setFormat(String fieldName, String format) {
		Preconditions.checkNotNull(fieldName);
		Preconditions.checkNotNull(format);
		Preconditions.checkArgument(!format.isEmpty()
				&& !CharMatcher.WHITESPACE.matchesAllOf(format));
		Preconditions.checkState(has(fieldName));
		ImmutableMap.Builder<String, String> formatsCopy = ImmutableMap.builder();
		formatsCopy.putAll(formats);
		formatsCopy.put(fieldName, format);
		return new NamedSchema(descriptor, name, skippedFields, constants, enumCaseFormat,
				substitutions, transforms, mappings, descriptions, subObjectSchemas, formatsCopy.build());
	}

	/**
	 * Returns the identifier of this schema's underlying descriptor.
	 */
	public String getId() {
		return descriptor.getFullName();
	}

	/**
	 * Returns the name of this schema.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns {@code true} if this schema references a field with the given
	 * name.
	 *
	 * @param name the name to look up
	 */
	private boolean has(String name) {
		return (fields.containsKey(name) && !skippedFields.contains(name)) ||
				constants.containsKey(name);
	}

	/**
	 * Returns the type of the JSON-Schema property that corresponds to a
	 * field of this schema.
	 *
	 * @param name the name of the field to look up
	 */
	public ShapeshifterProtos.JsonType getPropertyType(String name) {
		Preconditions.checkArgument(has(name));
		if (constants.containsKey(name)) {
			return JsonType.STRING;
		}
		if (transforms.containsKey(name)) {
			return transforms.get(name).getJsonType();
		}
		FieldDescriptor field = fields.get(name);
		if (field.isRepeated()) {
			if (mappings.containsKey(name)) {
				return JsonType.OBJECT;
			}
			return JsonType.ARRAY;
		}
		return getReifiedFieldType(field);
	}

	/**
	 * Returns the JSON type of a given protocol message field, regardless of
	 * whether the field is repeated or not.
	 *
	 * @param field the protocol buffer field to consider
	 */
	public ShapeshifterProtos.JsonType getReifiedFieldType(FieldDescriptor field) {
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

	/**
	 * Returns the external name of a given field of this schema.
	 *
	 * @param name the name of the field to look up
	 */
	public String getPropertyName(String name) {
		Preconditions.checkArgument(has(name));
		if (substitutions.containsKey(name)) {
			name = substitutions.get(name);
		}
		return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
	}

	/**
	 * Returns the descriptor on which this schema is based.
	 */
	@Override
	public Descriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Returns the entirety of the fields defined in this schema's descriptor.
	 */
	public ImmutableMap<String, FieldDescriptor> getFields() {
		return fields;
	}

	/**
	 * Returns the set of fields' names to skip upon serialization.
	 */
	public ImmutableSet<String> getSkippedFields() {
		return skippedFields;
	}

	/**
	 * Returns the key-value map of constants fields to include upon
	 * serialization.
	 */
	public ImmutableMap<String, String> getConstants() {
		return constants;
	}

	/**
	 * Returns the external casing formats to use for enumerated fields.
	 */
	public CaseFormat getEnumCaseFormat() {
		return enumCaseFormat;
	}

	/**
	 * Returns a map containing the external names to substitute, keyed by
	 * field name.
	 */
	public ImmutableMap<String, String> getSubstitutions() {
		return substitutions;
	}

	/**
	 * Returns the transforms to apply to the values, keyed by field name.
	 */
	public ImmutableMap<String, Transformer> getTransforms() {
		return ImmutableMap.<String, Transformer>copyOf(transforms);
	}

	/**
	 * Returns the repeated object fields for which serialization should be
	 * done as an object with dynamic keys instead of an array.
	 */
	public ImmutableMap<String, FieldDescriptor> getMappings() {
		return mappings;
	}

	/**
	 * Returns the descriptions of each field, keyed by field name.
	 */
	public ImmutableMap<String, String> getDescriptions() {
		return descriptions;
	}

	/**
	 * Returns the optional schema references for individual object fields.
	 */
	public ImmutableMap<String, String> getSubObjectsSchemas() {
		return subObjectSchemas;
	}
	
	/**
	 * Returns the formats of individual object fields.
	 */
	public ImmutableMap<String, String> getFormats() {
		return formats;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializer getSerializer() {
		return new NamedSchemaSerializer(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Parser getParser() {
		return new NamedSchemaParser(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonSchema getJsonSchema(ReadableSchemaRegistry schemas) throws JsonSchemaException {
		Preconditions.checkNotNull(schemas);
		JsonSchema.Builder schema = JsonSchema.newBuilder();
		schema.setType(JsonType.OBJECT);
		schema.setId(getName());

		for (String constantKey : constants.keySet()) {
			JsonSchema.Builder property = JsonSchema.newBuilder()
					.setName(constantKey)
					.setType(JsonType.STRING);
			if (descriptions.containsKey(constantKey)) {
				property.setDescription(descriptions.get(constantKey));
			}
			schema.addProperties(property);
		}

		for (Map.Entry<String, FieldDescriptor> fieldEntry : fields.entrySet()) {
			if (skippedFields.contains(fieldEntry.getKey())) {
				continue;
			}
			FieldDescriptor field = fieldEntry.getValue();
			
			JsonSchema.Builder property = JsonSchema.newBuilder();

			// Start with the simple stuff: description, required, format, default
			if (descriptions.containsKey(field.getName())) {
				property.setDescription(descriptions.get(field.getName()));
			}

			if (field.hasDefaultValue()) {
				if (field.getType().equals(Type.ENUM)) {
					EnumValueDescriptor defaultValue = (EnumValueDescriptor) field.getDefaultValue();
					property.setDefault(PROTO_ENUM_CASE_FORMAT.to(
							enumCaseFormat, defaultValue.getName()));
				} else {
					property.setDefault(field.getDefaultValue().toString());
				}
			}
			
			if (field.isRequired()) {
				property.setRequired(true);
			}
			
			if (formats.containsKey(field.getName())) {
				property.setFormat(formats.get(field.getName()));
			} else if (transforms.containsKey(field.getName())) {
				String transformedFormat = transforms.get(field.getName()).getExternalFormat();
				if (transformedFormat != null) {
					property.setFormat(transformedFormat);
				}
			}

			property.setName(getPropertyName(fieldEntry.getKey()));

			// This is where most of the work happens. This block of code
			// determines the correct type of the property, and sets the schema
			// references for objects.
			if (field.isRepeated()) {
				if (field.getType().equals(Type.MESSAGE)) {
					if (mappings.containsKey(field.getName())) {
						populateMappedFieldSchema(field, property, schemas);
					} else {
						populateRepeatedObjectSchema(field, property, schemas);
					}
				} else {
					populateRepeatedPrimitiveSchema(field, property);
				}
			} else {
				if (field.getType().equals(Type.MESSAGE)) {
					populateObjectSchema(field, property, schemas);
				} else {
					// Regular primitive field
					if (field.getType().equals(Type.ENUM)) {
						for (EnumValueDescriptor enumValue : field.getEnumType().getValues()) {
							property.addEnum(PROTO_ENUM_CASE_FORMAT.to(
									enumCaseFormat, enumValue.getName()));
						}
					}
					property.setType(getPropertyType(fieldEntry.getKey()));
				}
			}

			schema.addProperties(property);
		}
		return schema.build();
	}

	/**
	 * Populates a JSON Schema for a repeated, mapped object field.
	 * 
	 * @param field the proto field considered
	 * @param property the JSON schema being built
	 * @param schemas the set of known schemas
	 * @throws JsonSchemaException
	 */
	private void populateMappedFieldSchema(FieldDescriptor field,
			JsonSchema.Builder property, ReadableSchemaRegistry schemas) throws JsonSchemaException {
		property.setType(JsonType.OBJECT);
		if (subObjectSchemas.containsKey(field.getName())) {
			String schemaName = subObjectSchemas.get(field.getName());
			if (!schemas.contains(schemaName)) {
				throw new IllegalStateException();
			}
			// TODO(jsilland): validate type!
			property.setAdditionalProperties(
					JsonSchema.newBuilder().setSchemaReference(schemaName));

		} else {
			try {
				property = schemas.get(field.getMessageType())
						.getJsonSchema(schemas).toBuilder();
			} catch (SchemaObtentionException soe) {
				throw new JsonSchemaException(soe);
			}
		}
	}
	
	/**
	 * Populates a JSON Schema for a repeated object field.
	 * 
	 * @param field the proto field for which a JSON Schema whould be generated
	 * @param property the property being built
	 * @param schemas the set of known schemas
	 * @throws JsonSchemaException
	 */
	private void populateRepeatedObjectSchema(FieldDescriptor field, JsonSchema.Builder property,
			ReadableSchemaRegistry schemas) throws JsonSchemaException {
		property.setType(JsonType.ARRAY);
		if (subObjectSchemas.containsKey(field.getName())) {
			String schemaName = subObjectSchemas.get(field.getName());
			if (!schemas.contains(schemaName)) {
				throw new JsonSchemaException(new IllegalStateException(
						String.format("Schema %s refers to schema %s to format"
								+ " field %s but no such schema can be found in "
								+ "the registry", getName(), schemaName,
								field.getName())));
			}
			if (!schemas.get(schemaName).getDescriptor().getFullName()
					.equals(field.getMessageType().getFullName())) {
				throw new JsonSchemaException(new IllegalStateException(
						String.format("Schema %s refers to schema %s to format"
								+ " field %s but types do no match", getName(), schemaName,
								field.getName())));
			}
			property.setItems(JsonSchema.newBuilder().setSchemaReference(
					schemaName));
		} else {
			try {
				property.setItems(schemas.get(field.getMessageType())
						.getJsonSchema(schemas));
			} catch (SchemaObtentionException soe) {
				throw new JsonSchemaException(soe);
			}
		}
	}
	
	/**
	 * Populates a JSON schema for a repeated primitive proto field.
	 * 
	 * @param field the field being considered
	 * @param property the JSON schema being built
	 */
	private void populateRepeatedPrimitiveSchema(FieldDescriptor field, JsonSchema.Builder property) {
		property.setType(JsonType.ARRAY);
		property.setItems(JsonSchema.newBuilder().setType(getReifiedFieldType(field)));
		if (field.getType().equals(Type.ENUM)) {
			for (EnumValueDescriptor enumValue : field.getEnumType().getValues()) {
				property.addEnum(PROTO_ENUM_CASE_FORMAT.to(
						enumCaseFormat, enumValue.getName()));
			}
		}
	}
	
	/**
	 * Populates a JSON schema for a non-repeated object proto field.
	 * 
	 * @param field the field being considered
	 * @param property the JSON Schema being built
	 * @param schemas the set of known schemas
	 * @throws JsonSchemaException
	 */
	private void populateObjectSchema(FieldDescriptor field,
			JsonSchema.Builder property, ReadableSchemaRegistry schemas) throws JsonSchemaException {
		if (subObjectSchemas.containsKey(field.getName())) {
			String schemaName = subObjectSchemas.get(field.getName());
			if (!schemas.contains(schemaName)) {
				throw new JsonSchemaException(new IllegalStateException(
						String.format("Schema %s refers to schema %s to format"
								+ " field %s but no such schema can be found in "
								+ "the registry", getName(), schemaName,
								field.getName())));
			}
			if (!schemas.get(schemaName).getDescriptor().getFullName()
					.equals(field.getMessageType().getFullName())) {
				throw new JsonSchemaException(new IllegalStateException(
						String.format("Schema %s refers to schema %s to format"
								+ " field %s but types do no match", getName(),
								schemaName, field.getName())));
			}
			property.setSchemaReference(schemaName);
		} else {
			try {
				property = schemas.get(field.getMessageType())
						.getJsonSchema(schemas).toBuilder();
			} catch (SchemaObtentionException soe) {
				throw new JsonSchemaException(soe);
			}
		}
	}
}