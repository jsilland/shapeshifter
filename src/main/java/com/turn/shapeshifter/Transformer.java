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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A two-way transform between an external node and an internal value.
 *
 * @author jsilland
 */
public interface Transformer {

	/**
	 * Returns the type of JSON node this object handles.
	 */
	public JsonType getJsonType();

	/**
	 * Transforms a given object into a suitable external representation.
	 *
	 * @param value the value to transform
	 */
	public JsonNode serialize(Object value);

	/**
	 * Reverse-transformation of a JSON node into an internal representation.
	 *
	 * @param node the node to convert
	 */
	public Object parse(JsonNode node);
}