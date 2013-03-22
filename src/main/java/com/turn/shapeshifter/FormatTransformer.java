/**
 * Copyright 2013 Julien Silland
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

/**
 * Extends {@link Transformer} to allow for the specification of a custom
 * {@code format} string for the transformed property in the resulting JSON
 * Schema.
 *
 * @author jsilland
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.23">JSON Schema</a>
 */
public interface FormatTransformer extends Transformer {
	
	/**
	 * Returns the format of the transformed external value.
	 */
	public String getExternalFormat();
}
