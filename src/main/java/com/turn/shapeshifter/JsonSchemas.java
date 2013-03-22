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

/**
 * Utility class for JSON schemas manipulation.
 *
 * @author jsilland
 */
public final class JsonSchemas {

	private JsonSchemas() {

	}

	/**
	 * A schema properly configured to serialize instances of JSON schemas.
	 */
	public static final NamedSchema SCHEMA = NamedSchema
			.of(JsonSchema.getDescriptor(), "JsonSchema")
			.mapRepeatedField("properties", "name")
			.skip("name")
			.substitute("schema_reference", "$ref")
			.useSchema("additional_properties", "JsonSchema")
			.useSchema("items", "JsonSchema")
			.useSchema("properties", "JsonSchema")
			.describe("id", "The unique identifier for this schema")
			.describe("type", "The value of the type for this schema")
			.describe("description", "A description of the considered object or field")
			.describe("enum", "Values this field may take, if it is an enum")
			.describe("schema_reference",
					"A reference to another schema, by way of its unique identifier")
			.describe("items", "A schema of the objects contained in a JSON array")
			.describe("additional_properties",
					"If this the schema for an object, this property defines a schema for the " +
					"dynamic keys of that object")
			.describe("properties", "The properties of this object")
			.describe("required", "Whether the property is required")
			.describe("default", "The default value of the property")
			.describe("format", "Additional information about the format of the property");

}