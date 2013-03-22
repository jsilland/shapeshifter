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
package com.turn.shapeshifter.transformers;

import com.fasterxml.jackson.databind.node.TextNode;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DateTimeTransformer}
 * 
 * @author jsilland
 */
public class DateTimeTransformerTest {

	@Test
	public void testSerializeAndParse() {
		DateTimeTransformer transformer = new DateTimeTransformer();
		long tooLongAgo = (Long) transformer.parse(new TextNode("1981-10-17T21:00:00.000-02:00"));
		Assert.assertEquals("1981-10-17T23:00:00.000Z",
				transformer.serialize(new Long(tooLongAgo)).asText());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidValueRubbish() {
		// Completely rubbish
		DateTimeTransformer transformer = new DateTimeTransformer();
		transformer.parse(new TextNode("qux"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidValueMissingTimezone() {
		// Missing timezone
		DateTimeTransformer transformer = new DateTimeTransformer();
		transformer.parse(new TextNode("1981-10-17T21:00:00.000"));
	}
}
