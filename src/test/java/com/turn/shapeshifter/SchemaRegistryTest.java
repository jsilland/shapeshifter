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

import com.turn.shapeshifter.testing.TestProtos.Union;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link SchemaRegistry}.
 * 
 * @author jsilland
 */
public class SchemaRegistryTest {

	@Test
	public void testRegistration() {
		NamedSchema schema = NamedSchema.of(Union.getDescriptor(), "Union");
		SchemaRegistry registry = new SchemaRegistry();
		
		Assert.assertNull(registry.get(schema.getId()));
		Assert.assertFalse(registry.contains(schema.getId()));
		registry.register(schema);
		Assert.assertNotNull(registry.get(schema.getName()));
		Assert.assertTrue(registry.contains(schema.getName()));
	}
}
