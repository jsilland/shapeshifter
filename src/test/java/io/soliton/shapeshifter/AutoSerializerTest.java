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
package io.soliton.shapeshifter;

import com.turn.shapeshifter.testing.TestProtos.Actor;
import com.turn.shapeshifter.testing.TestProtos.DefaultValue;
import com.turn.shapeshifter.testing.TestProtos.Genre;
import com.turn.shapeshifter.testing.TestProtos.Movie;
import com.turn.shapeshifter.testing.TestProtos.Union;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AutoSerializer}.
 *
 * @author jsilland
 */
public class AutoSerializerTest {

  @Test
  public void testSerialize() throws Exception {
    Movie movie = Movie.newBuilder().setTitle("Rebel Without A Cause")
        .setYear(1955).build();
    JsonNode result = new AutoSerializer(Movie.getDescriptor())
        .serialize(movie, ReadableSchemaRegistry.EMPTY);

    Assert.assertTrue(result.isObject());
    Assert.assertEquals(JsonToken.VALUE_STRING, result.get("title").asToken());
    Assert.assertEquals("Rebel Without A Cause", result.get("title").asText());
    Assert.assertEquals(JsonToken.VALUE_NUMBER_INT, result.get("year").asToken());
    Assert.assertEquals(1955, result.get("year").asInt());
  }

  @Test
  public void testSerializeWithSubObject() throws Exception {

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
    Movie movie = Movie.newBuilder().setTitle("Rebel Without A Cause")
        .setYear(1955).build();
    Actor actor = Actor.newBuilder().setName("James Dean").addMovies(movie).build();

    JsonNode result = new AutoSerializer(Actor.getDescriptor())
        .serialize(actor, ReadableSchemaRegistry.EMPTY);

    Assert.assertTrue(result.isObject());
    Assert.assertEquals(JsonToken.VALUE_STRING, result.get("name").asToken());
    Assert.assertEquals("James Dean", result.get("name").asText());

    JsonNode array = result.get("movies");
    Assert.assertTrue(array.isArray());
    Assert.assertEquals(1, array.size());

    JsonNode movieNode = array.get(0);
    Assert.assertEquals(JsonToken.VALUE_STRING, movieNode.get("title").asToken());
    Assert.assertEquals("Rebel Without A Cause", movieNode.get("title").asText());
    Assert.assertEquals(JsonToken.VALUE_NUMBER_INT, movieNode.get("year").asToken());
    Assert.assertEquals(1955, movieNode.get("year").asInt());
  }

  @Test
  public void testSerializeWithRepeatedPrimitive() throws Exception {
    Actor actor = Actor.newBuilder().setName("James Dean").addQuotes("Foo").build();

    JsonNode result = new AutoSerializer(Actor.getDescriptor())
        .serialize(actor, ReadableSchemaRegistry.EMPTY);

    Assert.assertTrue(result.isObject());
    Assert.assertEquals(JsonToken.VALUE_STRING, result.get("name").asToken());
    Assert.assertEquals("James Dean", result.get("name").asText());

    JsonNode array = result.get("quotes");
    Assert.assertTrue(array.isArray());
    Assert.assertEquals(1, array.size());

    JsonNode quoteNode = array.get(0);
    Assert.assertEquals(JsonToken.VALUE_STRING, quoteNode.asToken());
    Assert.assertEquals("Foo", quoteNode.asText());
  }

  @Test
  public void testSerializeWithEmptyRepeated() throws Exception {
    Actor actor = Actor.newBuilder().setName("James Dean").build();

    JsonNode result = new AutoSerializer(Actor.getDescriptor())
        .serialize(actor, ReadableSchemaRegistry.EMPTY);

    Assert.assertTrue(result.isObject());
    Assert.assertEquals(JsonToken.VALUE_STRING, result.get("name").asToken());
    Assert.assertEquals("James Dean", result.get("name").asText());

    Assert.assertNull(result.get("movies"));
  }

  @Test
  public void testCaseFormatConversion() throws Exception {
    Movie movie = Movie.newBuilder().setProductionYear(1981).build();
    JsonNode node = new AutoSerializer(Movie.getDescriptor())
        .serialize(movie, ReadableSchemaRegistry.EMPTY);
    Assert.assertEquals(1981, node.get("productionYear").asInt());
  }

  @Test
  public void testSerializeMessageWithDefaultValue() throws Exception {
    // This test ensures that we do not serialize default values when they haven't
    // been explicitly set. This mirrors the behavior of protocol buffers
    // themselves.
    Serializer serializer = new AutoSerializer(DefaultValue.getDescriptor());
    DefaultValue defaultValue = DefaultValue.newBuilder().build();
    JsonNode result = serializer.serialize(defaultValue, ReadableSchemaRegistry.EMPTY);
    Assert.assertTrue(result.isNull());
  }

  @Test
  public void testSerializeEnum() throws Exception {
    Movie movie = Movie.newBuilder().setGenre(Genre.DRAMA).build();
    JsonNode node = new AutoSerializer(Movie.getDescriptor())
        .serialize(movie, ReadableSchemaRegistry.EMPTY);
    Assert.assertEquals("drama", node.get("genre").asText());
  }
}
