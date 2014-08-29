package com.ullink.gradle.plugins.jibx

import org.gradle.api.Plugin
import org.gradle.api.Project

class JIBXPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.configurations.create('jibxBindingReference')
        project.configurations.create('jibxRuntime').extendsFrom(project.configurations.jibxBindingReference)

        project.extensions.create("JIBXBinding", JIBXPluginExtension)


        project.JIBXBinding.bindingDir = new File(project.buildDir,JIBXPluginExtension.DEFAULT_BINDING_DIR)
        def tempClassFolder = new File(project.JIBXBinding.bindingDir,project.JIBXBinding.tempBuildFolderName)

        project.task('JIBXResources') << {
            project.JIBXBinding.bindingDir.mkdirs()
            tempClassFolder.mkdirs()
            def jibxDir = new File(tempClassFolder,project.JIBXBinding.rootAPIPath+'/jibx')
            jibxDir.mkdirs()

        }

        project.task('prepareJIBX').dependsOn('compileJava','JIBXResources') << {

            //copying binding configuration
            project.copy {
                from project.sourceSets.main.allSource
                println("copying binding configuration into $project.JIBXBinding.bindingDir.absolutePath")
                eachFile {
                    details -> details.path = details.name
                }
                into project.JIBXBinding.bindingDir
                include '**/bindings/*.xml'
            }

            //copying binding dependencies
            project.copy {
                from project.configurations.jibxBindingReference.files.collect{project.zipTree(it)}
                into tempClassFolder
                include '**/*.*'
            }

            //copying classes to bind
            project.copy {
                from project.sourceSets.main.output.files
                into tempClassFolder
                include '**/*.*'
            }


        }
        def runJIBX = project.task('runJIBX').dependsOn('prepareJIBX') << {
            project.javaexec {
                main = 'org.jibx.binding.Compile'
                args = project.JIBXBinding.bindingFiles
                debug = false
                classpath = project.configurations.jibxRuntime.asFileTree + project.files(tempClassFolder)
            }
        }


        project.task('postJIBX').dependsOn('runJIBX') << {
            //copy instrumented classes back to output dir
            project.copy {
                from tempClassFolder
                into project.sourceSets.main.output.classesDir;
                include project.JIBXBinding.rootAPIPath+'/**/*.*'
            }
        }

        // hooking JIBX on "classes" task from java plugin
        project.getTasks().getByName('classes').dependsOn('postJIBX')
    }
}
