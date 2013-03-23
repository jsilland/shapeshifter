Shapeshifter
============

Shapeshifter is a library that bridges the gap between Protocol Buffers and JSON, for the purpose of building robust APIs whose payloads are well-defined and documented.

Protocol Buffers provide an extremely compact, backward and forward-compatible serialization mechanism that is well-suited for machine to machine communication. On the other hand, JSON is the native data format for all parts of the modern web toolchain.

Shapeshifter lets you re-use messages defined as protobufs in your web service by exposing them as JSON objects. We hope you'll find Shapeshifter as useful as we have when developing our latest-generation web services. Either way, feel free to drop a line on the [mailing list](https://groups.google.com/forum/?fromgroups#!forum/shapeshifter-discuss) or file a bug on the [tracker](https://github.com/turn/shapeshifter/issues).

Maven coordinates
-----------------

You may add the following to your `pom.xml`

	<dependency>
		<groupId>com.turn</groupId>
		<artifactId>shapeshifter</artifactId>
		<version>1.1.0</version>
	</dependency>

Usage
-----

Assuming there exists a Protocol Buffer message defined as such:

	message Person {
		optional string name = 1;
		optional int32 age = 2;
		optional string city = 3;
	}

You may declare a schema 

	NamedSchema personSchema = NamedSchema.of(Person.getDescriptor(), "Person");

Serialization
-------------

Once a schema is defined, you may want to create an object for generating JSON:

	Serializer personSerializer = personSchema.getSerializer();
	Person biebs = Person.newBuilder().setName("Justin Bieber").build();
	JsonNode node = personSerializer.serialize(biebs, SchemaRegistry.EMPTY);

The resulting node's notation will be:

	{
		"name": "Justin Bieber",
	}

Parsing
-------

Conversely, schemas are able to validate and parse JSON content into Protocol Buffers messages:

	Parser personParser = personSchema.getParser();
	Message message = personParser.parse(node, SchemaRegistry.EMPTY)
	Person parsedBiebs = Person.newBuilder().mergeFrom(message).build();

Configurability
---------------

Schemas are configurable — you may want to transform values and names, avoid surfacing certain confidential fields and even create map-like JSON constructs. Read the full [Javadoc](http://turn.github.com/shapeshifter/apidocs/) to find out the available options.

JSON-Schema
-----------

Moreover, Shapeshifter supports the JSON-Schema specification and is able to derive the set of schemas from your Protocol Buffer messages. The library contains a message definition that represents a JSON-Schema and, as a proof of concept, Shapeshifter uses itself to define a schema compliant with the specification:

	JsonSchema personJsonSchema = personSchema.getJsonSchema();
	JsonNode node = JsonSchemas.SCHEMA.getSerializer().serialize(personJsonSchema);

Will yield:

	{
		"id": "Person",
		"type": "object",
		"properties": {
			"name": {
				"type": "string"
			},
			"age": {
				"type": "number"
			},
			"city": {
				"type": "string"
			}
		}
	}

License
-------

Shapeshifter is distributed under the terms of the Apache Software License version 2.0. See LICENSE file for more details.


Authors and contributors
------------------------

* Julien Silland <<jsilland@turn.com>> (Software Engineer at Turn, Inc)  
  Original author, main developer and maintainer
