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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;

/**
 * Encapsulates the task of serializing a {@link Message} to JSON.
 *
 * @author jsilland
 */
public interface Serializer {

	/**
	 * Returns a JSON representation of {@code message}.
	 *
	 * <p>The registry argument is used as a source of schemas when the
	 * serializer needs to generate content for message types it cannot
	 * handle on its own.
	 *
	 * @param message the message to format.
	 * @param registry a registry of previously existing schemas used
	 * for serializing sub-objects.
	 * @see SchemaRegistry#register(NamedSchema)
	 * @see SchemaRegistry#get(com.google.protobuf.Descriptors.Descriptor)
	 */
	public JsonNode serialize(Message message, ReadableSchemaRegistry registry)
			throws SerializationException;
}