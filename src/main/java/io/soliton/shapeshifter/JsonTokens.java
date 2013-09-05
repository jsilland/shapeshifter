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

import java.util.EnumSet;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Constants and utilities pertaining to JSON tokens.
 *
 * @author jsilland
 */
final class JsonTokens {

  private JsonTokens() {

  }

  /**
   * The set of tokens suitable to represent a boolean.
   */
  static final EnumSet<JsonToken> VALID_BOOLEAN_TOKENS = EnumSet.of(
      JsonToken.VALUE_FALSE, JsonToken.VALUE_TRUE);

  /**
   * The set of tokens suitable to represent an enumeration.
   */
  static final EnumSet<JsonToken> VALID_ENUM_TOKENS = EnumSet.of(JsonToken.VALUE_STRING);

  /**
   * The set of tokens suitable to represent an integer.
   */
  static final EnumSet<JsonToken> VALID_INTEGER_TOKENS = EnumSet
      .of(JsonToken.VALUE_NUMBER_INT);

  /**
   * The set of tokens suitable to represent a string.
   */
  static final EnumSet<JsonToken> VALID_STRING_TOKENS = EnumSet
      .of(JsonToken.VALUE_STRING);

  /**
   * The set of tokens suitable to represent a floating point number.
   */
  static final EnumSet<JsonToken> VALID_FLOAT_TOKENS = EnumSet
      .of(JsonToken.VALUE_NUMBER_FLOAT);

  /**
   * Enforces the type of a JSON node.
   *
   * @param jsonNode           the considered JSON node
   * @param acceptedTokenTypes the set of accepted node types
   */
  static void checkJsonValueConformance(JsonNode jsonNode,
                                        EnumSet<JsonToken> acceptedTokenTypes) {
    JsonToken consideredToken = jsonNode.asToken();
    if (!acceptedTokenTypes.contains(consideredToken)) {
      throw new UnmappableValueException(jsonNode, acceptedTokenTypes);
    }
  }
}