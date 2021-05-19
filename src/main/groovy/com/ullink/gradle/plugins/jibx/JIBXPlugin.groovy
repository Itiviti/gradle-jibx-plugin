package com.ullink.gradle.plugins.jibx

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar

class JIBXPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.configurations.create('jibxCompile')
        project.extensions.create('jibx', JIBXPluginExtension)

        def output = project.sourceSets.main.output
        def mainClassesDirs
        if (output.hasProperty('classesDirs')) {
            mainClassesDirs = output.classesDirs.getSingleFile()
        } else {
            mainClassesDirs = output.classesDir
        }

        project.task('generateUnboundJar', dependsOn: 'classes', type: Jar) {
            description 'Generates a JAR file without any JIBX binding.'
            classifier = 'nojibxbinding'
            from output.files
            onlyIf { project.jibx.archiveUnboundJar }
        }

        project.task('generateBindingsJar', dependsOn: 'processResources', type: Jar) {
            description 'Generates a JAR file with only the XML bindings.'
            classifier = 'bindings'
            from output.resourcesDir
            eachFile {
                f -> f.path = new File(f.name)
            }
            onlyIf { project.jibx.archiveBindingsJar }
        }
        project.task('jibxDependencies', type: Copy) {
            description 'Copies and extract all JIBX dependencies into a temporary directory.'
            from { project.configurations.jibxCompile.collect { project.zipTree(it) } }
            from mainClassesDirs
            into temporaryDir
        }

        project.task('jibxBindings', dependsOn: 'processResources', type: JIBXBindings) {
            description 'Copies all JIBX bindings into a temporary directory.'
            registerBindingsDirectory new File(output.resourcesDir, project.jibx.rootPath), '.'
        }

        project.task('compileJibx', dependsOn: ['jibxDependencies', 'jibxBindings', 'classes', 'generateUnboundJar', 'generateBindingsJar'], type: JIBXCompile) {
            description 'Runs the JIBX compiler on the generated classes in a temporary directory.'
            load project.jibx.testLoading
            verbose project.jibx.verbose
            verify project.jibx.verify
            trackBranches project.jibx.trackBranches
            errorOverride project.jibx.overrideErrors
            skipValidate project.jibx.skipBindValidation
            classPathFile project.jibxDependencies.temporaryDir
            classPathFile project.jibxBindings.temporaryDir
            classPathFiles project.jibx.classPathFiles
            bindingFiles project.jibx.bindingFiles
        }

        project.task('collectJibx', dependsOn: 'compileJibx', type: Copy) {
            description 'Copies the compiled JIBX files back into the generated classes directory.'
            from project.jibxDependencies.temporaryDir
            into mainClassesDirs
            project.afterEvaluate {
                include project.jibx.rootPath + '/**/*.*'
            }
        }

        project.classes.finalizedBy project.collectJibx
    }

}
