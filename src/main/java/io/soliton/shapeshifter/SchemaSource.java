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

import com.google.protobuf.Descriptors.Descriptor;

/**
 * A source for instances of {@link Schema} that are built from a Protocol
 * Buffer {@link Descriptor}.
 *
 * @author jsilland
 */
public interface SchemaSource {

  /**
   * Returns an automatically generated schema for the given message
   * descriptor.
   *
   * @param descriptor the descriptor of a Protocol Buffer message
   * @throws SchemaObtentionException when the schema cannot be obtained
   */
  public Schema get(Descriptor descriptor) throws SchemaObtentionException;
}