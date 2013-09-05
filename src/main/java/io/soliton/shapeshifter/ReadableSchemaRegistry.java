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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.Descriptor;

/**
 * Defines the read operations on a registry of schema instances.
 *
 * @author jsilland
 */
public interface ReadableSchemaRegistry extends SchemaSource {

  /**
   * An immutable, empty instance of readable schema registry.
   */
  public static final ReadableSchemaRegistry EMPTY = new ReadableSchemaRegistry() {

    @Override
    public List<JsonSchema> getJsonSchemas() {
      return ImmutableList.of();
    }

    @Override
    public NamedSchema get(String name) {
      return null;
    }

    @Override
    public Schema get(Descriptor descriptor) {
      return AutoSchema.of(descriptor);
    }

    @Override
    public boolean contains(String name) {
      return false;
    }
  };

  /**
   * Returns the schema identified by the given name, or {@code null} if
   * no such schema is registered in this instance.
   *
   * @param name the name of the schema to look up
   */
  public Schema get(String name);

  /**
   * Generates the set of JSON Schemas that are known to this instance.
   *
   * @return The list of JSON Schemas.
   * @throws JsonSchemaException
   */
  public List<JsonSchema> getJsonSchemas() throws JsonSchemaException;

  /**
   * Returns {@code true} if this registry contains a schema for the given
   * name.
   *
   * @param name the identifier to look up
   */
  public boolean contains(String name);

}