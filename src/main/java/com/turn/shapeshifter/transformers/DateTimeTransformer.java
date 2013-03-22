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

import com.turn.shapeshifter.FormatTransformer;
import com.turn.shapeshifter.ShapeshifterProtos.JsonType;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Preconditions;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Marshalls a Unix epoch in milliseconds into a ISO-8601-formatted string.
 * 
 * @author jsilland
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a>
 */
public class DateTimeTransformer implements FormatTransformer {

	private static final DateTimeFormatter ISO_8601 = ISODateTimeFormat.dateTime()
			.withZone(DateTimeZone.UTC);
	private static final String DATE_TIME_FORMAT = "date-time";
	
	/** 
	 * @param object the number of milliseconds since the Unix epoch
	 */
	@Override
	public JsonNode serialize(Object object) {
		if (object == null) {
			return NullNode.instance;
		}
		Preconditions.checkArgument(object instanceof Long);
		Long millis = (Long) object;
		return new TextNode(ISO_8601.print(millis));
	}

	/** 
	 * @param node a date formatted using ISO 8601
	 */
	@Override
	public Object parse(JsonNode node) {
		Preconditions.checkArgument(JsonToken.VALUE_STRING.equals(node.asToken()));
		String nodeValue = node.asText();
		return new Long(ISO_8601.parseMillis(nodeValue));
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public JsonType getJsonType() {
		return JsonType.STRING;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getExternalFormat() {
		return DATE_TIME_FORMAT;
	}
}
