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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;

/**
 * This interface encapsulates the task of parsing a JSON node into a
 * protocol buffer {@link Message}.
 *
 * @author jsilland
 */
public interface Parser {

  /**
   * Parses a JSON node into its corresponding message.
   * <p/>
   * Note: schemas only maintain the data relative to the protocol buffer's
   * structure but know nothing of the generated Java type that resulted from
   * the representation's compilation. This method is therefore unable to
   * instantiate messages of the correct subclass. The returned object is
   * however guaranteed to be fully compatible with said generated class and
   * can be safely merged at construction time, e.g.:
   * <p/>
   * <pre> {@code
   * Schema schema = Schema.of(MyProto.getDescriptorForType())
   * Message message = schema.parse(jsonNode);
   * MyProto instance = MyProto.newBuilder().mergeFrom(message).build();}</pre>
   *
   * @param node the root node to parse
   */
  public Message parse(JsonNode node, ReadableSchemaRegistry registry) throws ParsingException;
}