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

import java.util.EnumSet;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;

/**
 * Thrown when a JSON value node cannot be correctly mapped to a Java object.
 *
 * @author jsilland
 */
@SuppressWarnings("serial")
public class UnmappableValueException extends IllegalArgumentException {

	private static final Joiner COMMA_JOINER = Joiner.on(",");

	/**
	 * @param jsonNode the value node that triggered the error
	 * @param acceptedTokenTypes the set of node types that are considered valid
	 */
	public UnmappableValueException(JsonNode jsonNode, EnumSet<JsonToken> acceptedTokenTypes) {
		super(COMMA_JOINER.appendTo(
				new StringBuilder("JSON node with value: '")
				.append(jsonNode.toString())
				.append("' cannot be parsed as one of "),
				acceptedTokenTypes.iterator()).toString());
	}
}