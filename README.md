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

## The `JIBXCompile` Task

This is a simple wrapper of the JIBX Compile command line task that we call to
compile the bindings. The same arguments as command line are accepted.

## The `jibx` Plugin

Applying the plugin hooks the JIBX compilation tasks on the `jar` task. It will
first compile the bindings in a temporary directory then copy the result in the
generated classes directory.

To configure the plugin, you can either use the `jibx` project extension or
hook yourself on ont of the following sub tasks for a more complex workflow:

- `jibxDependencies` will extract all your jibx dependencies (by default only
the JIBX library itself and your sources) in order to pass it to the compiler
as a classpath. If you need to add more dependencies, add them to this Copy
task.
- `jibxBindings` will copy all your JIBX mappings into a temporary directory
(by default only the XML files found in the bindings directory of your
sources). This will enable you to reference multiple files in your bindings.
If you need to reference mappings from third party libraries, reference them
using `registerBindingsDirectory` and `registerBindingsArchive`.
