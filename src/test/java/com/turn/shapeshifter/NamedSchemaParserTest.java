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

import com.turn.shapeshifter.ShapeshifterProtos.JsonType;
import com.turn.shapeshifter.testing.TestProtos.Actor;
import com.turn.shapeshifter.testing.TestProtos.Movie;
import com.turn.shapeshifter.testing.TestProtos.SomeEnum;
import com.turn.shapeshifter.testing.TestProtos.Union;
import com.turn.shapeshifter.transformers.DateTimeTransformer;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link NamedSchemaParser}.
 * 
 * @author jsilland
 */
public class NamedSchemaParserTest {

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
	public void testParse() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.addConstant("kind", "shapeshifter.Union");
		Union union = Union.newBuilder().setBoolValue(true).setEnumValue(SomeEnum.FIRST)
				.setInt32Value(42).setInt64Value(42L).setStringValue("text").build();
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonNode json = new NamedSchemaSerializer(schema).serialize(union, registry);

		Union parsedUnion = Union.newBuilder()
				.mergeFrom(new NamedSchemaParser(schema).parse(json, registry)).build();
		Assert.assertEquals(union, parsedUnion);
	}

	@Test
	public void testParseWithSubobject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.addConstant("kind", "shapeshifter.Union")
				.useSchema("union_value", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union subUnion = Union.newBuilder().setInt32Value(42).build();
		Union union = Union.newBuilder().setEnumValue(SomeEnum.SECOND).setUnionValue(subUnion)
				.build();
		JsonNode json = new NamedSchemaSerializer(schema).serialize(union, registry);

		Union result = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(json, registry)).build();
		Assert.assertEquals(union, result);
	}
	
	@Test
	public void testParseWithSubobjectCustomSchema() {
		
	}

	@Test
	public void testParseWithEmptyObject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder()
				.mergeFrom(new NamedSchemaParser(schema).parse(
						new ObjectNode(JsonNodeFactory.instance), registry)).build();
		Assert.assertEquals(Union.getDefaultInstance(), union);
	}

	@Test
	public void testParseWithRepeatedObject() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union subUnion = Union.newBuilder().setInt32Value(42).build();
		Union union = Union.newBuilder().addUnionRepeated(subUnion).addUnionRepeated(subUnion)
				.build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertEquals(2, parsed.getUnionRepeatedCount());
		Assert.assertEquals(42, parsed.getUnionRepeated(0).getInt32Value());
		Assert.assertEquals(42, parsed.getUnionRepeated(1).getInt32Value());
	}

	@Test
	public void testParseWithRepeatedPrimitive() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().addInt32Repeated(42).addInt32Repeated(42).build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertEquals(2, parsed.getInt32RepeatedCount());
		Assert.assertEquals(42, parsed.getInt32Repeated(0));
		Assert.assertEquals(42, parsed.getInt32Repeated(1));
	}

	@Test
	public void testParseWithEmptyRepeated() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.useSchema("union_repeated", "Union");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setBoolValue(true)
				.addUnionRepeated(Union.getDefaultInstance()).build();
		ObjectNode result = (ObjectNode) new NamedSchemaSerializer(schema).serialize(union, registry);
		result.put("unionRepeated", new ArrayNode(JsonNodeFactory.instance));
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertTrue(parsed.getBoolValue());
		Assert.assertEquals(0, parsed.getUnionRepeatedCount());
	}
	
	@Test
	public void testParseCustomEnumFormat() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union").enumCaseFormat(
				CaseFormat.UPPER_UNDERSCORE);
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setEnumValue(SomeEnum.FIRST).build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertEquals(SomeEnum.FIRST, parsed.getEnumValue());
	}

	@Test
	public void testParseSubtitution() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.substitute("string_value", "$ref");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setStringValue("foo").build();

		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertEquals("foo", parsed.getStringValue());
	}
	
	@Test
	public void testParseTransform() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.transform("int64_value", new DateTimeTransformer());
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		Union union = Union.newBuilder().setInt64Value(1234567890000L).build();
		JsonNode result = new NamedSchemaSerializer(schema).serialize(union, registry);
		
		Assert.assertEquals("2009-02-13T23:31:30.000Z", result.get("int64Value").asText());
		
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertEquals(1234567890000L, parsed.getInt64Value());
	}
	
	@Test
	public void testParseWithRepeatedTransform() throws Exception {
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
		
		ArrayNode repeated = (ArrayNode) result.get("int32Repeated");
		Assert.assertEquals(6, repeated.size());
		Assert.assertEquals(2, repeated.get(0).asInt());
		Assert.assertEquals(2, repeated.get(1).asInt());
		Assert.assertEquals(4, repeated.get(2).asInt());
		Assert.assertEquals(6, repeated.get(3).asInt());
		Assert.assertEquals(10, repeated.get(4).asInt());
		Assert.assertEquals(16, repeated.get(5).asInt());
		
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema).parse(
				result, registry)).build();
		Assert.assertEquals(1, parsed.getInt32Repeated(0));
		Assert.assertEquals(1, parsed.getInt32Repeated(1));
		Assert.assertEquals(2, parsed.getInt32Repeated(2));
		Assert.assertEquals(3, parsed.getInt32Repeated(3));
		Assert.assertEquals(5, parsed.getInt32Repeated(4));
		Assert.assertEquals(8, parsed.getInt32Repeated(5));
	}
	
	@Test
	public void testParseMappings() throws Exception {
		Movie bladeRunner = Movie.newBuilder().setYear(1981).setTitle("Blade Runner").build();
		Movie starWars = Movie.newBuilder().setYear(1978).setTitle("Star Wars").build();
		Actor actor = Actor.newBuilder().addMovies(bladeRunner).addMovies(starWars)
				.setName("Harrison Ford").build();
		
		NamedSchema schema = NamedSchema.of(Actor.getDescriptor(), "Actor")
				.mapRepeatedField("movies", "title");
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonNode result = new NamedSchemaSerializer(schema).serialize(actor, registry);
		
		Actor parsed = Actor.newBuilder().mergeFrom(new NamedSchemaParser(schema)
				.parse(result, registry)).build();
		Assert.assertEquals(actor, parsed);
	}

	@Test
	public void testLongAsString() throws Exception {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union")
				.surfaceLongsAsStrings()
				.useSchema("union_value", "Union")
				.useSchema("union_repeated", "Union");

		Union union = Union.newBuilder().setInt64Value(1234567890L)
				.addInt64Repeated(1234567).build();
		SchemaRegistry registry = new SchemaRegistry();
		registry.register(schema);
		JsonNode result = schema.getSerializer().serialize(union, registry);
		Union parsed = Union.newBuilder().mergeFrom(new NamedSchemaParser(schema).parse(
				result, registry)).build();
		Assert.assertEquals(1234567890L, parsed.getInt64Value());
		Assert.assertEquals(1234567L, (long) parsed.getInt64RepeatedList().get(0));
	}
}
