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

import com.turn.shapeshifter.ShapeshifterProtos.JsonType;
import com.turn.shapeshifter.testing.TestProtos.Actor;
import com.turn.shapeshifter.testing.TestProtos.DefaultValue;
import com.turn.shapeshifter.testing.TestProtos.Movie;
import com.turn.shapeshifter.testing.TestProtos.SomeEnum;
import com.turn.shapeshifter.testing.TestProtos.Union;
import com.turn.shapeshifter.transformers.DateTimeTransformer;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link NamedSchemaSerializer}.
 * 
 * @author jsilland
 */
public class NamedSchemaSerializerTest {

	private static final Transformer TWO = new Transformer() {

		@Override
		public JsonType getJsonType() {
			return JsonType.INTEGER;
		}

		@Override
		public JsonNode serialize(Object value) {
			Preconditions.checkArgument(value instanceof Integer);
			Integer integer = (Integer) value;
			return new IntNode(integer * 2);
		}

		@Override
		public Object parse(JsonNode node) {
			Preconditions.checkArgument(JsonToken.VALUE_NUMBER_INT.equals(node.asToken()));
			int value = node.asInt();
			return new Integer(value / 2);
		}
	};
	
	@Test
	public void testSerialize() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.addConstant("kind", "shapeshifter.Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setBoolValue(true).setEnumValue(SomeEnum.FIRST)
				.setInt32Value(42).setInt64Value(42L).setStringValue("text").build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);

		Assert.assertTrue(result.isObject());
		Assert.assertEquals(JsonToken.VALUE_STRING, result.get("kind").asToken());
		Assert.assertEquals("shapeshifter.Union", result.get("kind").asText());
		Assert.assertEquals(JsonToken.VALUE_TRUE, result.get("boolValue").asToken());
		Assert.assertEquals(true, result.get("boolValue").asBoolean());
		Assert.assertEquals(JsonToken.VALUE_STRING, result.get("enumValue").asToken());
		Assert.assertEquals("first", result.get("enumValue").asText());
		Assert.assertEquals(JsonToken.VALUE_NUMBER_INT, result.get("int32Value").asToken());
		Assert.assertEquals(42, result.get("int32Value").asInt());
		Assert.assertEquals(JsonToken.VALUE_NUMBER_INT, result.get("int64Value").asToken());
		Assert.assertEquals(42, result.get("int64Value").asInt());
		Assert.assertEquals(JsonToken.VALUE_STRING, result.get("stringValue").asToken());
		Assert.assertEquals("text", result.get("stringValue").asText());
	}

	@Test
	public void testSerializeWithSubObject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.addConstant("kind", "Union")
				.useSchema("union_value", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union subUnion = Union.newBuilder().setInt32Value(42).build();
		Union union = Union.newBuilder().setEnumValue(SomeEnum.SECOND).setUnionValue(subUnion)
				.build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertTrue(result.isObject());
		Assert.assertEquals(JsonToken.VALUE_STRING, result.get("kind").asToken());
		Assert.assertEquals("Union", result.get("kind").asText());
		Assert.assertEquals(JsonToken.VALUE_STRING, result.get("enumValue").asToken());
		Assert.assertEquals("second", result.get("enumValue").asText());

		JsonNode subUnionJson = result.get("unionValue");
		Assert.assertTrue(subUnionJson.isObject());
		Assert.assertEquals(JsonToken.VALUE_NUMBER_INT, subUnionJson.get("int32Value").asToken());
		Assert.assertEquals(42, subUnionJson.get("int32Value").asInt());
		Assert.assertEquals(JsonToken.VALUE_STRING, subUnionJson.get("kind").asToken());
		Assert.assertEquals("Union", subUnionJson.get("kind").asText());
	}

	@Test
	public void testSerializeWithEmptyObject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.getDefaultInstance();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertTrue(result.isNull());
	}

	@Test
	public void testSerializeWithEmptySubObject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setBoolValue(true).build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertTrue(result.isObject());
		Assert.assertEquals(1, result.size());
		Assert.assertNull(result.get("unionValue"));
	}

	@Test
	public void testSerializeWithRepeatedObject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union subUnion = Union.newBuilder().setInt32Value(42).build();
		Union union = Union.newBuilder().addUnionRepeated(subUnion).addUnionRepeated(subUnion)
				.build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		JsonNode array = result.get("unionRepeated");
		Assert.assertTrue(array.isArray());
		Assert.assertEquals(2, array.size());
		for (JsonNode item : array) {
			Assert.assertTrue(item.isObject());
			Assert.assertEquals(42, item.get("int32Value").intValue());
		}
	}

	@Test
	public void testSerializeWithRepeatedPrimitive() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().addInt32Repeated(42).addInt32Repeated(42).build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		JsonNode array = result.get("int32Repeated");
		Assert.assertTrue(array.isArray());
		Assert.assertEquals(2, array.size());
		for (JsonNode item : array) {
			Assert.assertEquals(JsonToken.VALUE_NUMBER_INT, item.asToken());
			Assert.assertEquals(42, item.intValue());
		}
	}

	@Test
	public void testSerializeWithEmptyRepeated() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setBoolValue(true)
				.addUnionRepeated(Union.getDefaultInstance()).build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertTrue(result.isObject());
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(JsonToken.VALUE_TRUE, result.get("boolValue").asToken());
		Assert.assertEquals(true, result.get("boolValue").asBoolean());
		Assert.assertNull(result.get("unionRepeated"));
	}

	@Test
	public void testSerializeMessageWithDefaultValue() throws Exception {
		// This test ensures that we do not serialize default values when they haven't
		// been explicitly set. This mirrors the behavior of protocol buffers
		// themselves.
		NamedSchema schema = NamedSchema.of(DefaultValue.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		DefaultValue defaultValue = DefaultValue.newBuilder().build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(defaultValue, registry);
		Assert.assertTrue(result.isNull());
	}

	@Test
	public void testSerializeCustomEnumFormat() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.enumCaseFormat(CaseFormat.UPPER_UNDERSCORE);
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setEnumValue(SomeEnum.FIRST).build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertEquals("FIRST", result.get("enumValue").asText());
	}

	@Test
	public void testSerializeSubtitution() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.substitute("string_value", "$ref");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setStringValue("foo").build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertNotNull(result.get("$ref"));
	}
	
	@Test
	public void testSerializeWithTransform() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.transform("int64_value", new DateTimeTransformer());
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setInt64Value(1234567890000L).build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Assert.assertEquals("2009-02-13T23:31:30.000Z", result.get("int64Value").asText());
	}
	
	@Test
	public void testSerializeWithRepeatedTransform() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.transform("int32_repeated", TWO);
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union fibonacci = Union.newBuilder()
				.addInt32Repeated(1)
				.addInt32Repeated(1)
				.addInt32Repeated(2)
				.addInt32Repeated(3)
				.addInt32Repeated(5)
				.addInt32Repeated(8)
				.build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(fibonacci, registry);
		Assert.assertTrue(result.isObject());
		ArrayNode array = (ArrayNode) result.get("int32Repeated");
		Assert.assertEquals(2, array.get(0).asInt());
		Assert.assertEquals(2, array.get(1).asInt());
		Assert.assertEquals(4, array.get(2).asInt());
		Assert.assertEquals(6, array.get(3).asInt());
		Assert.assertEquals(10, array.get(4).asInt());
		Assert.assertEquals(16, array.get(5).asInt());
	}

	@Test
	public void testSerializeMappings() throws Exception {
		Movie bladeRunner = Movie.newBuilder().setYear(1981).setTitle("Blade Runner").build();
		Movie starWars = Movie.newBuilder().setYear(1978).setTitle("Star Wars").build();
		Actor actor = Actor.newBuilder().addMovies(bladeRunner).addMovies(starWars)
				.setName("Harrison Ford").build();
		
		NamedSchema schema = NamedSchema.of(Actor.getDescriptor(), "Actor")
				.mapRepeatedField("movies", "title");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonNode result = new NamedSchemaSerializer(schema).serialize(actor, registry);
		Assert.assertTrue(result.isObject());
		Assert.assertEquals("Harrison Ford", result.get("name").asText());
		JsonNode movies = result.get("movies");
		Assert.assertTrue(movies.isObject());
		Assert.assertNotNull(movies.get("Blade Runner"));
		Assert.assertEquals(1981, movies.get("Blade Runner").get("year").asInt());
		Assert.assertNotNull(movies.get("Star Wars"));
		Assert.assertEquals(1978, movies.get("Star Wars").get("year").asInt());
	}
}
