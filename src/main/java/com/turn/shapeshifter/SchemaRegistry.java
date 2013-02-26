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

import com.turn.shapeshifter.ShapeshifterProtos.JsonSchema;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;

/**
 * A container of schemas.
 *
 * @author jsilland
 *
 * @see ReadableSchemaRegistry
 * @see WritableSchemaRegistry
 */
public class SchemaRegistry implements ReadableSchemaRegistry, WritableSchemaRegistry {

	private final ConcurrentMap<String, NamedSchema> schemas;
	private final LoadingCache<Descriptors.Descriptor, AutoSchema> autoSchemas;

	private static final CacheLoader<Descriptors.Descriptor, AutoSchema> AUTO_SCHEMA_LOADER =
			new CacheLoader<Descriptors.Descriptor, AutoSchema>() {

				@Override
				public AutoSchema load(Descriptor key) throws Exception {
					return AutoSchema.of(key);
				}
			};

	/**
	 * Creates a new, empty registry.
	 */
	public SchemaRegistry() {
		this(CacheBuilder.newBuilder());
	}

	/**
	 * Constructs a new instance of this class with the given caching policy.
	 *
	 * @param cacheBuilder a configured {@link CacheBuilder} for storing
	 * instances of automatically generated schema
	 */
	public SchemaRegistry(CacheBuilder<Object, Object> cacheBuilder) {
		Preconditions.checkNotNull(cacheBuilder);
		this.schemas = new MapMaker().makeMap();
		this.autoSchemas = cacheBuilder.build(AUTO_SCHEMA_LOADER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void register(NamedSchema schema) {
		Preconditions.checkNotNull(schema);
		schemas.put(schema.getName(), schema);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NamedSchema get(String name) {
		Preconditions.checkNotNull(name);
		return schemas.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AutoSchema get(final Descriptors.Descriptor descriptor)
			throws SchemaObtentionException {
		try {
			return autoSchemas.get(descriptor);
		} catch (ExecutionException ee) {
			throw new SchemaObtentionException(ee);
		} catch (UncheckedExecutionException uee) {
			throw new SchemaObtentionException(uee);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<JsonSchema> getJsonSchemas() throws JsonSchemaException {
		ImmutableList.Builder<JsonSchema> jsonSchemas = ImmutableList.builder();
		for (NamedSchema schema : schemas.values()) {
			jsonSchemas.add(schema.getJsonSchema(this));
		}
		return jsonSchemas.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(String name) {
		return schemas.containsKey(name);
	}

	/**
	 * Returns the set of schemas registered in this instance.
	 */
	public Iterator<NamedSchema> getSchemas() {
		return schemas.values().iterator();
	}
}