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
import com.turn.shapeshifter.testing.TestProtos.Actor;
import com.turn.shapeshifter.testing.TestProtos.Bar;
import com.turn.shapeshifter.testing.TestProtos.DefaultValue;
import com.turn.shapeshifter.testing.TestProtos.Foo;
import com.turn.shapeshifter.testing.TestProtos.Movie;
import com.turn.shapeshifter.testing.TestProtos.RequiredValue;
import com.turn.shapeshifter.testing.TestProtos.Union;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AutoSchema}.
 * 
 * @author jsilland
 */
public class AutoSchemaTest {

	@Test(expected = IllegalArgumentException.class)
	public void testAutoSchemaCannotReferItself() {
		// Union is a type that contains fields of type Union
		new AutoSchema(Union.getDescriptor());
	}

	@Test
	public void testGetJsonSchema() throws Exception {
		Schema schema = new AutoSchema(Actor.getDescriptor());
		ShapeshifterProtos.JsonSchema jsonSchema = schema.getJsonSchema(ReadableSchemaRegistry.EMPTY);
				
		Assert.assertEquals(JsonType.OBJECT, jsonSchema.getType());
		Assert.assertEquals(3, jsonSchema.getPropertiesCount());
		Assert.assertEquals("name", jsonSchema.getProperties(0).getName());
		Assert.assertEquals(JsonType.STRING, jsonSchema.getProperties(0).getType());
		Assert.assertEquals("movies", jsonSchema.getProperties(1).getName());
		Assert.assertEquals(JsonType.ARRAY, jsonSchema.getProperties(1).getType());
		Assert.assertEquals("quotes", jsonSchema.getProperties(2).getName());
		Assert.assertEquals(JsonType.ARRAY, jsonSchema.getProperties(2).getType());
		
		// The schema for the Movie message should be inlined.
		ShapeshifterProtos.JsonSchema movieItems = jsonSchema.getProperties(1).getItems();
		Assert.assertEquals(JsonType.OBJECT, movieItems.getType());
		Assert.assertEquals(4, movieItems.getPropertiesCount());
		Assert.assertEquals("title", movieItems.getProperties(0).getName());
		Assert.assertEquals(JsonType.STRING, movieItems.getProperties(0).getType());
		Assert.assertEquals("year", movieItems.getProperties(1).getName());
		Assert.assertEquals(JsonType.INTEGER, movieItems.getProperties(1).getType());
		Assert.assertEquals("genre", movieItems.getProperties(2).getName());
		Assert.assertEquals(JsonType.STRING, movieItems.getProperties(2).getType());
		Assert.assertEquals("productionYear", movieItems.getProperties(3).getName());
		Assert.assertEquals(JsonType.INTEGER, movieItems.getProperties(3).getType());
	}
	
	@Test
	public void testGetJsonSchemaRequiredField() throws Exception {
		Schema schema = new AutoSchema(RequiredValue.getDescriptor());
		ShapeshifterProtos.JsonSchema jsonSchema = schema.getJsonSchema(ReadableSchemaRegistry.EMPTY);
				
		Assert.assertEquals(JsonType.OBJECT, jsonSchema.getType());
		Assert.assertEquals(1, jsonSchema.getPropertiesCount());
		Assert.assertTrue(jsonSchema.getProperties(0).getRequired());
	}
	
	@Test
	public void testJsonSchemaWithDefaultValue() throws Exception {
		Schema schema = new AutoSchema(DefaultValue.getDescriptor());
		JsonSchema jsonSchema = schema.getJsonSchema(SchemaRegistry.EMPTY);
	 
		Assert.assertEquals(JsonType.OBJECT, jsonSchema.getType());
		Assert.assertEquals(3, jsonSchema.getPropertiesCount());
		
		Assert.assertEquals("stringValue", jsonSchema.getProperties(0).getName());
		Assert.assertEquals(JsonType.STRING, jsonSchema.getProperties(0).getType());
		Assert.assertEquals("foo", jsonSchema.getProperties(0).getDefault());

		Assert.assertEquals("intValue", jsonSchema.getProperties(1).getName());
		Assert.assertEquals(JsonType.INTEGER, jsonSchema.getProperties(1).getType());
		Assert.assertEquals("42", jsonSchema.getProperties(1).getDefault());

		Assert.assertEquals("enumValue", jsonSchema.getProperties(2).getName());
		Assert.assertEquals(JsonType.STRING, jsonSchema.getProperties(2).getType());
		Assert.assertEquals("second", jsonSchema.getProperties(2).getDefault());
	}
	
	@Test
	public void testIsDescriptorLooping() {
		Assert.assertTrue(AutoSchema.isDescriptorLooping(Bar.getDescriptor()));
	}
	
	@Test
	public void testIsDescriptorLoopingWithIntermediaryLoop() {
		Assert.assertTrue(AutoSchema.isDescriptorLooping(Foo.getDescriptor()));
	}
	
	@Test
	public void testIsDescriptorNonLooping() {
		Assert.assertFalse(AutoSchema.isDescriptorLooping(Movie.getDescriptor()));
	}
}
