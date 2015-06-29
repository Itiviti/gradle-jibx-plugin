package com.ullink.gradle.plugins.jibx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle interface to the JIBX compiler.
 */
class JIBXCompile extends DefaultTask {

    private boolean load
    private boolean verbose
    private boolean verify
    private boolean trackBranches
    private boolean errorOverride
    private boolean skipValidate
    private List<File> classPathFiles = []
    private List<File> bindingFiles = []

    def load(boolean load) {
        this.load = load
        return this
    }

    def verbose(boolean verbose) {
        this.verbose = verbose
        return this
    }

    def verify(boolean verify) {
        this.verify = verify
        return this
    }

    def trackBranches(boolean trackBranches) {
        this.trackBranches = trackBranches
        return this
    }

    def errorOverride(boolean errorOverride) {
        this.errorOverride = errorOverride
        return this
    }

    def skipValidate(boolean skipValidate) {
        this.skipValidate = skipValidate
        return this
    }

    def classPathFiles(List<File> classPathFiles) {
        this.classPathFiles.addAll(classPathFiles)
        return this
    }

    def classPathFile(File classPathFile) {
        this.classPathFiles.add(classPathFile)
        return this
    }

    def bindingFiles(List<File> bindingFiles) {
        this.bindingFiles.addAll(bindingFiles)
        return this
    }

    def bindingFile(File bindingFile) {
        this.bindingFiles.add(bindingFile)
        return this
    }

    @TaskAction
    def compile() {
        def classLoader = URLClassLoader.newInstance(classPathFiles.collect { it -> it.toURI().toURL() } as URL[])
        def compilerClass = Class.forName('org.jibx.binding.Compile', true, classLoader)
        def compiler = compilerClass.newInstance(verbose, load, verify, trackBranches, errorOverride)
        compiler.skipValidate = skipValidate
        compiler.compile(classPathFiles as String[], bindingFiles as String[])
    }

}
