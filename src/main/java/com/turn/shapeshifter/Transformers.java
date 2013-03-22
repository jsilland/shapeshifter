/**
 * Copyright 2013 Turn, Inc.
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
import com.turn.shapeshifter.ShapeshifterProtos.JsonType;

/**
 * Static utilities pertaining to {@link Transformer} instances.
 *
 * @author jsilland
 */
public final class Transformers {

	private Transformers() {
		
	}
	
	private static class NullFormatTransformer implements FormatTransformer {

		private final Transformer transformer;
		
		private NullFormatTransformer(Transformer transformer) {
			this.transformer = transformer;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public JsonType getJsonType() {
			return transformer.getJsonType();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public JsonNode serialize(Object value) {
			return transformer.serialize(value);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object parse(JsonNode node) {
			return transformer.parse(node);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getExternalFormat() {
			return null;
		}
		
	}
	
	/**
	 * Returns a new formatting transformer that delegates to a simple
	 * transformer but returns {@code null} for the format value.
	 * 
	 * @param transformer the transformer to wrap
	 */
	public static FormatTransformer format(Transformer transformer) {
		return new NullFormatTransformer(transformer);
	}
}
