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

import com.turn.shapeshifter.ShapeshifterProtos.JsonSchema;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the schema in charge of formatting instances of {@link JsonSchema}
 * <p/>
 * (Sooooo meta)
 *
 * @author jsilland
 */
public class JsonSchemasTest {

  @Test
  public void testSchema() throws Exception {
    SchemaRegistry registry = new SchemaRegistry();
    registry.register(JsonSchemas.SCHEMA);
    JsonSchema metaSchema = JsonSchemas.SCHEMA.getJsonSchema(registry);
    JsonNode metaSchemaNode = JsonSchemas.SCHEMA.getSerializer().serialize(
        metaSchema, registry);

    Assert.assertTrue(metaSchemaNode.isObject());
    Assert.assertTrue(metaSchemaNode.has("id"));
    Assert.assertEquals("JsonSchema", metaSchemaNode.get("id").asText());
    Assert.assertTrue(metaSchemaNode.has("type"));
    Assert.assertEquals("object", metaSchemaNode.get("type").asText());
    Assert.assertTrue(metaSchemaNode.has("properties"));

    JsonNode properties = metaSchemaNode.get("properties");

    Assert.assertTrue(properties.has("id"));
    Assert.assertTrue(properties.get("id").isObject());
    Assert.assertEquals("string", properties.get("id").get("type").asText());

    Assert.assertTrue(properties.has("type"));
    Assert.assertTrue(properties.get("type").isObject());
    Assert.assertEquals("string", properties.get("type").get("type").asText());

    Assert.assertTrue(properties.has("description"));
    Assert.assertTrue(properties.get("description").isObject());
    Assert.assertEquals("string", properties.get("description").get("type").asText());

    Assert.assertTrue(properties.has("additionalProperties"));
    Assert.assertTrue(properties.get("additionalProperties").isObject());
    Assert.assertEquals("JsonSchema",
        properties.get("additionalProperties").get("$ref").asText());

    Assert.assertTrue(properties.has("items"));
    Assert.assertTrue(properties.get("items").isObject());
    Assert.assertEquals("JsonSchema",
        properties.get("items").get("$ref").asText());

    Assert.assertTrue(properties.has("$ref"));
    Assert.assertTrue(properties.get("$ref").isObject());
    Assert.assertEquals("string", properties.get("$ref").get("type").asText());

    Assert.assertTrue(properties.has("properties"));
    Assert.assertTrue(properties.get("properties").isObject());
    Assert.assertEquals("object", properties.get("properties").get("type").asText());
    Assert.assertEquals("JsonSchema",
        properties.get("properties").get("additionalProperties").get("$ref").asText());

    Assert.assertTrue(properties.has("enum"));
    Assert.assertTrue(properties.get("enum").isObject());
    Assert.assertEquals("array", properties.get("enum").get("type").asText());
    Assert.assertEquals("string", properties.get("enum").get("items").get("type").asText());

    Assert.assertTrue(properties.has("required"));
    Assert.assertTrue(properties.get("required").isObject());
    Assert.assertEquals("boolean", properties.get("required").get("type").asText());
    Assert.assertEquals("false", properties.get("required").get("default").asText());

    Assert.assertTrue(properties.has("default"));
    Assert.assertTrue(properties.get("default").isObject());
    Assert.assertEquals("string", properties.get("default").get("type").asText());

    Assert.assertTrue(properties.has("format"));
    Assert.assertTrue(properties.get("format").isObject());
    Assert.assertEquals("string", properties.get("format").get("type").asText());
  }
}
