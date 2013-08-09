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
import com.turn.shapeshifter.testing.TestProtos.DefaultValue;
import com.turn.shapeshifter.testing.TestProtos.RequiredValue;
import com.turn.shapeshifter.testing.TestProtos.Union;
import com.turn.shapeshifter.transformers.DateTimeTransformer;

import java.util.Map;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link NamedSchema}.
 * 
 * @author jsilland
 */
public class NamedSchemaTest {

	public static final class ToString implements Transformer {
		
		@Override
		public JsonNode serialize(Object value) {
			return new TextNode(value.toString());
		}
		
		@Override
		public Object parse(JsonNode node) {
			Preconditions.checkArgument(node.asToken().equals(JsonToken.VALUE_STRING));
			String nodeValue = node.asText();
			Integer value = Integer.valueOf(nodeValue);
			return value;
		}
		
		@Override
		public JsonType getJsonType() {
			return JsonType.STRING;
		}
	};

	@Test
	public void testSchemaName() {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		Assert.assertEquals("Union", schema.getName());
	}

	@Test
	public void testPropertyType() {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		Assert.assertEquals(JsonType.STRING, schema.getPropertyType("string_value"));
		Assert.assertEquals(JsonType.INTEGER, schema.getPropertyType("int32_value"));
		Assert.assertEquals(JsonType.INTEGER, schema.getPropertyType("int64_value"));
		Assert.assertEquals(JsonType.BOOLEAN, schema.getPropertyType("bool_value"));
		Assert.assertEquals(JsonType.OBJECT, schema.getPropertyType("union_value"));
		Assert.assertEquals(JsonType.ARRAY, schema.getPropertyType("union_repeated"));
		Assert.assertEquals(JsonType.ARRAY, schema.getPropertyType("int32_repeated"));
		Assert.assertEquals(JsonType.STRING, schema.getPropertyType("enum_value"));
	}

	@Test
	public void testPropertyName() {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		Assert.assertEquals("stringValue", schema.getPropertyName("string_value"));
	}
	
	@Test
	public void testJsonSchemaEmptyRegistry() throws JsonSchemaException {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		try {
			schema.getJsonSchema(ReadableSchemaRegistry.EMPTY);
			Assert.fail();
		} catch (IllegalArgumentException iae) {
			// expected
		}
	}
	
	@Test
	public void testJsonSchemaSelfReference() throws JsonSchemaException {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		try {
			schema.getJsonSchema(registry);
			Assert.fail();
		} catch (JsonSchemaException jse) {
			// expected
		}
	}
	
	@Test(expected = NullPointerException.class)
	public void testJsonSchemasNullRegistry() throws JsonSchemaException {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		schema.getJsonSchema(null);
	}
	
	@Test
	public void testJsonSchema() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		
		Assert.assertEquals(11, jsonSchema.getPropertiesCount());
		Map<String, JsonSchema> properties = Maps.newHashMap();
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			properties.put(property.getName(), property);
		}
		Assert.assertTrue(properties.containsKey("stringValue"));
		Assert.assertTrue(properties.containsKey("int32Value"));
		Assert.assertTrue(properties.containsKey("int64Value"));
		Assert.assertTrue(properties.containsKey("boolValue"));
		Assert.assertTrue(properties.containsKey("unionValue"));
		Assert.assertTrue(properties.containsKey("unionRepeated"));
		Assert.assertTrue(properties.containsKey("int32Repeated"));
		Assert.assertTrue(properties.containsKey("enumValue"));
		
		Assert.assertEquals(JsonType.STRING, properties.get("stringValue").getType());
		Assert.assertEquals(JsonType.INTEGER, properties.get("int32Value").getType());
		Assert.assertEquals(JsonType.INTEGER, properties.get("int64Value").getType());
		Assert.assertEquals(JsonType.BOOLEAN, properties.get("boolValue").getType());
		Assert.assertEquals(JsonType.OBJECT, properties.get("unionValue").getType());
		Assert.assertEquals(JsonType.ARRAY, properties.get("unionRepeated").getType());
		Assert.assertEquals(JsonType.ARRAY, properties.get("int32Repeated").getType());
		Assert.assertEquals(JsonType.STRING, properties.get("enumValue").getType());
		
		Assert.assertEquals(JsonType.OBJECT, properties.get("unionRepeated").getItems().getType());
		Assert.assertEquals(JsonType.INTEGER, properties.get("int32Repeated").getItems().getType());

		Assert.assertEquals("Union",
				properties.get("unionRepeated").getItems().getSchemaReference());
		
		Assert.assertEquals(2, properties.get("enumValue").getEnumList().size());
		Assert.assertTrue(properties.get("enumValue").getEnumList().contains("first"));
		Assert.assertTrue(properties.get("enumValue").getEnumList().contains("second"));
	}
	
	@Test
	public void testJsonSchemaWithTransform() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.transform("int32_value", new ToString())
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("int32Value")) {
				Assert.assertEquals(JsonType.STRING, property.getType());
				checked = true;
			}
		}
		
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithSkippedField() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.skip("int32_value")
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		Assert.assertEquals(10, jsonSchema.getPropertiesCount());
	}
	
	@Test
	public void testJsonSchemaWithConstant() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.addConstant("key", "value")
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		Assert.assertEquals(12, jsonSchema.getPropertiesCount());
	}
	
	@Test
	public void testJsonSchemaWithSubstitution() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.substitute("string_value", "foobar")
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			Assert.assertFalse(property.getName().equals("stringValue"));
			if (property.getName().equals("foobar")) {
				Assert.assertEquals(JsonType.STRING, property.getType());
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithMapping() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.mapRepeatedField("union_repeated", "string_value")
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("unionRepeated")) {
				Assert.assertEquals(JsonType.OBJECT, property.getType());
				Assert.assertEquals("Union",
						property.getAdditionalProperties().getSchemaReference());
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithEnumCaseFormat() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.enumCaseFormat(CaseFormat.UPPER_UNDERSCORE)
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("enumValue")) {
				Assert.assertEquals(JsonType.STRING, property.getType());
				Assert.assertTrue(property.getEnumList().contains("FIRST"));
				Assert.assertTrue(property.getEnumList().contains("SECOND"));
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithFormatTransformer() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.transform("int64_value", new DateTimeTransformer())
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		
		Assert.assertEquals("Union", jsonSchema.getId());
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("int64Value")) {
				Assert.assertEquals(JsonType.STRING, property.getType());
				Assert.assertEquals("date-time", property.getFormat());
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithDefaultValue() throws Exception {
		NamedSchema schema = NamedSchema.of(DefaultValue.getDescriptor(), "DefaultValue");
		JsonSchema jsonSchema = schema.getJsonSchema(SchemaRegistry.EMPTY);
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("stringValue")) {
				Assert.assertEquals("foo", property.getDefault());
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithDefaultValueEnumCaseFormat() throws Exception {
		NamedSchema schema = NamedSchema.of(DefaultValue.getDescriptor(), "DefaultValue");
		JsonSchema jsonSchema = schema.getJsonSchema(SchemaRegistry.EMPTY);
				
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("enumValue")) {
				Assert.assertEquals(property.getDefault(), "second");
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithDefaultValueCustomEnumCaseFormat() throws Exception {
		NamedSchema schema = NamedSchema.of(DefaultValue.getDescriptor(), "DefaultValue")
				.enumCaseFormat(CaseFormat.UPPER_UNDERSCORE);
		JsonSchema jsonSchema = schema.getJsonSchema(SchemaRegistry.EMPTY);
				
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("enumValue")) {
				Assert.assertEquals(property.getDefault(), "SECOND");
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithRequiredField() throws Exception {
		NamedSchema schema = NamedSchema.of(RequiredValue.getDescriptor(), "RequiredValue");
		JsonSchema jsonSchema = schema.getJsonSchema(SchemaRegistry.EMPTY);
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("requiredString")) {
				Assert.assertTrue(property.getRequired());
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}
	
	@Test
	public void testJsonSchemaWithFormat() throws Exception {
		NamedSchema schema = NamedSchema.of(DefaultValue.getDescriptor(), "DefaultValue")
				.setFormat("string_value", "int");

		JsonSchema jsonSchema = schema.getJsonSchema(SchemaRegistry.EMPTY);
		
		boolean checked = false;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("stringValue")) {
				Assert.assertEquals("int", property.getFormat());
				checked = true;
			}
		}
		Assert.assertTrue(checked);
	}

	@Test
	public void testJsonSchemaWithLongAsString() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.surfaceLongsAsStrings()
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");

		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonSchema jsonSchema = schema.getJsonSchema(registry);
		int checks = 0;
		for (JsonSchema property : jsonSchema.getPropertiesList()) {
			if (property.getName().equals("int64Value")) {
				Assert.assertEquals("int64", property.getFormat());
				Assert.assertEquals(JsonType.STRING, property.getType());
				checks++;
			} else if (property.getName().equals("int64Repeated")) {
				JsonSchema items = property.getItems();
				Assert.assertEquals("int64", items.getFormat());
				Assert.assertEquals(JsonType.STRING, items.getType());
				checks++;
			} else if (property.getName().equals("uint64Value")) {
				Assert.assertEquals("uint64", property.getFormat());
				Assert.assertEquals(JsonType.STRING, property.getType());
				checks++;
			}
		}
		Assert.assertEquals(3, checks);
	}

}
