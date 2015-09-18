package com.ullink.gradle.plugins.jibx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

import java.lang.reflect.Constructor

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
    }

    def verbose(boolean verbose) {
        this.verbose = verbose
    }

    def verify(boolean verify) {
        this.verify = verify
    }

    def trackBranches(boolean trackBranches) {
        this.trackBranches = trackBranches
    }

    def errorOverride(boolean errorOverride) {
        this.errorOverride = errorOverride
    }

    def skipValidate(boolean skipValidate) {
        this.skipValidate = skipValidate
    }

    def classPathFiles(List<File> classPathFiles) {
        this.classPathFiles.addAll(classPathFiles)
    }

    def classPathFile(File classPathFile) {
        this.classPathFiles.add(classPathFile)
    }

    def bindingFiles(List<File> bindingFiles) {
        this.bindingFiles.addAll(bindingFiles)
    }

    def bindingFile(File bindingFile) {
        this.bindingFiles.add(bindingFile)
    }

    @TaskAction
    def compile() {
        def classLoader = URLClassLoader.newInstance(classPathFiles.collect { it -> it.toURI().toURL() } as URL[])
        def compilerClass
        def exceptionClass
        try {
            compilerClass = Class.forName('org.jibx.binding.Compile', true, classLoader)
            exceptionClass = Class.forName('org.jibx.runtime.JiBXException', true, classLoader)
        }
        catch (ClassNotFoundException e) {
            logger.warn("Could not compile JIBX bindings because no JIBX compiler was found in your classpath. Make sure you reference one in the task classpath, for example in the jibxCompile configuration.")
            throw new TaskExecutionException(this, e)
        }

        try {
            def compiler;
            try {
                //from version 1.2.5 of JibX, constructor has two verbose switches (and so one more parameter)
                Constructor<?> constructor = compilerClass.getDeclaredConstructor(Boolean.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class)
                compiler = compilerClass.newInstance(verbose, verbose, load, verify, trackBranches, errorOverride)
            }
            catch (NoSuchMethodException) {
                compiler = compilerClass.newInstance(verbose, load, verify, trackBranches, errorOverride)
            }
            compiler.skipValidate = skipValidate
            compiler.compile(classPathFiles as String[], bindingFiles as String[])
        }
        catch (Exception e) {
            if (exceptionClass.isInstance(e) && e.getRootCause() != null) {
                logger.warn("An exception occurred during the JIBX compilation: " + e.message)
                throw e.getRootCause()
            }
            throw new TaskExecutionException(this, e)
        }
    }

}
