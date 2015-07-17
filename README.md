# Gradle JIBX Plugin

This plugin allows to compile JIBX bindings which may reference other bindings
in third party libraries.

## Main Features

- Gradle wrapper of the JIBX compiler including command arguments.
- Version of JIBX libraries configurable by the build script.
- Support of bindings located in third party libraries.
- Compatible with Java 6/7/8.

## Quick Start

```
apply plugin: 'jibx'

jibx {
    // Configure the plugin here.
    // See com.ullink.gradle.plugins.jibx.JIBXPluginExtension for details.

    // The path of the binding files in your project. Other files referenced in
    // definitions will be ignored. Useful if you have bindings in third-party
    // libraries.
    rootPath = 'com/example/my_bindings'

    // The root files of your definitions. This is the input to the JIBX
    // compiler.
    bindingFiles = [ 'my_object.xml', 'another_object.xml' ]

    // The classpath where JIBX will find dependencies to your third party
    // libraries.
    classPathFiles = [ 'build/tmp/jibxDependencies/lib1' ]

    // Ask to generate a JAR without any JIBX binding in it. You will need this
    // if other libraries depend on you.
    archiveUnboundJar = true

    // Ask to generate a JAR with only the JIBX binding definitions in it. You
    // will need this if other libraries depend on you.
    archiveBindingsJar = true
}

dependencies {
    // Add the JIBX dependencies explicitly in this configuration.
    jibxCompile(
            [group: 'org.jibx', name: 'jibx-bind', version: '1.2.6'],
            [group: 'org.jibx', name: 'jibx-run', version: '1.2.6'],
            [group: 'xpp3', name: 'xpp3', version: '1.1.3.4-RC8'],
            // Use the latest release of BCEL for Java 6/7.
            [group: 'org.apache.bcel', name: 'bcel', version: '5.2'],
            // Use the next version of BCEL for Java 8.
            // [group: 'org.apache.bcel', name: 'bcel', version: '6.0-SNAPSHOT'],
    )
}
```

## Use Case

This plugin can be used when bindings are split in multiple libraries. Imagine
the following use case:

```
library1
    com/lib1/Object1.class
    object1.xml
library2
    com/lib2/Object2.class that extends Object1
    object2.xml that contains <include path="./library1/object1.xml" />
```

- The first library is compiled, plus unbound and a bindings JAR files.
- The second library uses the result of the first to compile:
    - The unbound jar is extracted in the dependencies directory. It will be
    bound a second time but the result will be ignored. We only need it to
    compile the second library.
    - The bindings jar is extracted in the bindings directory by calling
    `registerBindingsDirectory`, so the second definition can depend on the
    first one.

Because it can bind a dependency multiple times, this plugin uses a
temporary directory to compile and then copies only what is needed in the
classes directory. Even if library1 was bound once again when we compiled
library2, setting the rootPath to `com/lib2/` ensured us that only the
second one was included in the final jar.

## Gradle API

### The `JIBXCompile` Task

This is a simple wrapper of the JIBX Compile command line task that we call to
compile the bindings. The same arguments as command line are accepted.

### The `jibx` Plugin

Applying the plugin hooks the JIBX compilation tasks on the `jar` task. It will
first compile the bindings in a temporary directory then copy the result in the
generated classes directory.

To configure the plugin, you can either use the `jibx` project extension or
hook yourself on one of the following sub tasks for a more complex workflow:

- `jibxDependencies` will extract all your jibx dependencies (by default only
the JIBX library itself and your sources) in order to pass it to the compiler
as a classpath. If you need to add more dependencies, add them to this Copy
task.
- `jibxBindings` will copy all your JIBX mappings into a temporary directory
(by default only the XML files found in the bindings directory of your
sources). This will enable you to reference multiple files in your bindings.
If you need to reference mappings from third party libraries, reference them
using `registerBindingsDirectory` and `registerBindingsArchive`.
