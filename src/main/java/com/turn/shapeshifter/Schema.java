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

import com.google.protobuf.Descriptors;

/**
 * A self-descriptive, two-way transform between a JSON object and a Protocol
 * Buffer message.
 *
 * @author jsilland
 * @see <a href="http://json-schema.org">JSON Schema</a>
 * @see < href="https://developers.google.com/protocol-buffers">Protocol Buffers</a>
 */
public interface Schema {

	/**
	 * Returns a parser able to read JSON content, as configured by this
	 * instance.
	 */
	public Parser getParser();

	/**
	 * Returns a serializer able to generate JSON content, as configured
	 * by this instance.
	 */
	public Serializer getSerializer();

	/**
	 * Returns the JSON Schema which describes the JSON payloads this schema is
	 * able to parse and serialize.
	 *
	 * @param registry may contain custom schemas for other Protocol Buffer
	 * types refered to by this instance
	 * @throws JsonSchemaException in case the schema cannot be generated
	 */
	public ShapeshifterProtos.JsonSchema getJsonSchema(ReadableSchemaRegistry registry)
			throws JsonSchemaException;

	/**
	 * Returns the Protocol Buffer descriptor on which this schema is based.
	 */
	public Descriptors.Descriptor getDescriptor();
}