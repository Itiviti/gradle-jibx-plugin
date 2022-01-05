package com.ullink.gradle.plugins.jibx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class JIBXBindings extends DefaultTask {

    @Internal
    Map directories = [:]

    @Internal
    Map archives = [:]

    def registerBindingsDirectory(def directory, def output) {
        directories[directory] = output
    }

    def registerBindingsArchive(def file, def output) {
        archives[file] = output
    }

    @TaskAction
    def buildBindingsLayout() {
        directories.each { directory, output ->
            project.fileTree(directory)
                    .filter { it.name.endsWith(".xml") }
                    .each { copyBindingFile(it, output) }
        }
        archives.each { archive, output ->
            getProject().zipTree(archive).each {
                copyBindingFile(it, output)
            }
        }
    }

    def copyBindingFile(file, output ) {
        getProject().copy {
            from file
            into temporaryDir
            eachFile {
                f -> f.path = new File(output, f.name)
            }
        }
    }

}
