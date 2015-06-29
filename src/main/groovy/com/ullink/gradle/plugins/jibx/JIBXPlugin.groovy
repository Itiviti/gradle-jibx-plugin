package com.ullink.gradle.plugins.jibx

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar

class JIBXPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.configurations.create('jibxCompile')
        project.extensions.create('jibx', JIBXPluginExtension)

        project.afterEvaluate {
            project.task('generateUnboundJar', dependsOn: 'classes', type: Jar) {
                description 'Generates a JAR file without any JIBX binding.'
                classifier = 'nojibxbinding'
                from project.sourceSets.main.output.files
            }
            project.generateUnboundJar.onlyIf { project.jibx.archiveUnboundJar }

            project.task('jibxDependencies', type: Copy) {
                description 'Copies and extract all JIBX dependencies into a temporary directory.'
                from project.configurations.jibxCompile.collect{ project.zipTree(it) }
                from project.sourceSets.main.output.classesDir
                into temporaryDir
            }

            project.task('jibxBindings', type: Copy) {
                description 'Copies all JIBX bindings into a temporary directory.'
                from project.sourceSets.main.output.files
                into temporaryDir
                include project.jibx.rootPath + '**/bindings/*.xml'
                eachFile {
                    details -> details.path = details.name
                }
            }

            project.task('compileJibx', dependsOn: ['jibxDependencies', 'jibxBindings', 'classes', 'generateUnboundJar'], type: JIBXCompile) {
                description 'Runs the JIBX compiler on the generated classes in a temporary directory.'
                load project.jibx.testLoading
                verbose project.jibx.verbose
                verify project.jibx.verify
                trackBranches project.jibx.trackBranches
                errorOverride project.jibx.overrideErrors
                skipValidate project.jibx.skipBindValidation
                classPathFiles project.configurations.compile.collect()
                classPathFiles project.configurations.jibxCompile.collect()
                classPathFile project.jibxDependencies.temporaryDir
                classPathFile project.jibxBindings.temporaryDir
                classPathFiles project.jibx.classPathFiles
                bindingFiles project.jibx.bindingFiles
            }

            project.task('collectJibx', dependsOn: 'compileJibx', type:Copy) {
                description 'Copies the compiled JIBX files back into the generated classes directory.'
                from project.jibxDependencies.temporaryDir
                into project.sourceSets.main.output.classesDir
                include project.jibx.rootPath + '/**/*.*'
            }

            project.jar.dependsOn project.collectJibx
        }
    }

}
