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

import com.turn.shapeshifter.testing.TestProtos.Actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Message;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AutoParser}.
 * 
 * @author jsilland
 */
public class AutoParserTest {
	
	@Test
	public void testParse() throws Exception {
		Actor.Builder actorBuilder = Actor.newBuilder().setName("James Dean");
		actorBuilder.addMoviesBuilder().setTitle("Rebel Withour A Cause").setYear(1955).build();
		
		Actor actor = actorBuilder.build();
		
		JsonNode json = new AutoSerializer(Actor.getDescriptor())
				.serialize(actor, ReadableSchemaRegistry.EMPTY);

		Message parsedMessage = new AutoParser(Actor.getDescriptor())
				.parse(json, ReadableSchemaRegistry.EMPTY);
		Actor parsedActor = Actor.newBuilder().mergeFrom(parsedMessage).build();
		
		Assert.assertEquals(actor, parsedActor);
	}

	@Test
	public void testParseWithEmptyObject() throws Exception {
		Actor actor = Actor.getDefaultInstance();
		
		JsonNode json = new AutoSerializer(Actor.getDescriptor())
				.serialize(actor, ReadableSchemaRegistry.EMPTY);

		Message parsedMessage = new AutoParser(Actor.getDescriptor())
				.parse(json, ReadableSchemaRegistry.EMPTY);
		Actor parsedActor = Actor.newBuilder().mergeFrom(parsedMessage).build();
		
		Assert.assertEquals(Actor.getDefaultInstance(), parsedActor);
	}

	@Test
	public void testParseWithRepeatedPrimitive() throws Exception {
		Actor actor = Actor.newBuilder()
				.setName("James Dean")
				.addQuotes("Foo Bar")
				.addQuotes("Qux").build();
		
		JsonNode json = new AutoSerializer(Actor.getDescriptor())
				.serialize(actor, ReadableSchemaRegistry.EMPTY);

		Message parsedMessage = new AutoParser(Actor.getDescriptor())
				.parse(json, ReadableSchemaRegistry.EMPTY);
		Actor parsedActor = Actor.newBuilder().mergeFrom(parsedMessage).build();
		
		Assert.assertEquals(actor, parsedActor);
	}

	@Test
	public void testParseWithEmptyRepeated() throws Exception {
		Actor actor = Actor.newBuilder()
				.setName("James Dean").build();
		
		ObjectNode json = (ObjectNode) new AutoSerializer(Actor.getDescriptor())
				.serialize(actor, ReadableSchemaRegistry.EMPTY);
		json.put("quotes", new ArrayNode(JsonNodeFactory.instance));

		Message parsedMessage = new AutoParser(Actor.getDescriptor())
				.parse(json, ReadableSchemaRegistry.EMPTY);
		Actor parsedActor = Actor.newBuilder().mergeFrom(parsedMessage).build();
		
		Assert.assertEquals("James Dean", parsedActor.getName());
		Assert.assertEquals(0, parsedActor.getQuotesCount());
	}
}
