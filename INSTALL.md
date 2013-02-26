How to build
============

Shapeshifter uses Maven as a build system. More information on how to get and use Maven may be found at [maven.apache.org/](http://maven.apache.org/).

Protocol Buffers
----------------

Apart from the standard dependencies declared in the `pom.xml` file, this library requires the Protocol Buffer compiler, a.k.a `protoc`, to be present on the host. `protoc` is an executable program distributed as part of the open-source Protocol Buffers project. You may install the compiler by downloading its source from the official [repository](http://code.google.com/p/protobuf/) and following the instructions for building it.

Maven Protoc Plugin
-------------------

Shapeshifter's Maven artifact relies on [Sergei Ivanov's Maven plugin](https://github.com/sergei-ivanov/maven-protoc-plugin) for invoking the protocol buffer compiler. This plugin uses Maven's [toolchain](http://maven.apache.org/guides/mini/guide-using-toolchains.html) mechanism to find the path to the compiler. See [the plugin's manual](http://sergei-ivanov.github.com/maven-protoc-plugin/examples/protobuf-toolchain.html) to find a sample configuration.

Building and packaging the library
---------------------

Once you have installed `protoc` and declared the proper toolchain, you may build this library by running:

	$ mvn compile

You may package this library by invoking:

	$ mvn package


