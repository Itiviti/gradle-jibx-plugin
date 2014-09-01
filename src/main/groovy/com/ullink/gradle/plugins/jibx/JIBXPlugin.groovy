package com.ullink.gradle.plugins.jibx

import org.gradle.api.Plugin
import org.gradle.api.Project

class JIBXPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.configurations.create('jibxBindingReference')
        project.configurations.create('jibxRuntime').extendsFrom(project.configurations.jibxBindingReference)

        project.extensions.create("JIBXBinding", JIBXPluginExtension)

        //force cleanup since incremental build on this plugin is not yet working well
        project.sourceSets.main.output.classesDir.deleteDir();

        project.task('JIBXResources') << {
            project.JIBXBinding.bindingDir = new File(project.buildDir,JIBXPluginExtension.DEFAULT_BINDING_DIR)
            project.JIBXBinding.tempClassFolder = new File(project.JIBXBinding.bindingDir,JIBXPluginExtension.TEMP_TARGET_DIR)
            project.JIBXBinding.bindingDir.mkdirs()
            if (project.JIBXBinding.tempClassFolder.exists()) {
                project.JIBXBinding.tempClassFolder.deleteDir()
            }
            project.JIBXBinding.tempClassFolder.mkdirs()
            def jibxDir = new File(project.JIBXBinding.tempClassFolder,project.JIBXBinding.rootAPIPath+'/jibx')
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
                into project.JIBXBinding.tempClassFolder
                include '**/*.*'
            }

            //copying classes to bind
            project.copy {
                from project.sourceSets.main.output.files
                into project.JIBXBinding.tempClassFolder
                include '**/*.*'
            }


        }
        project.task('runJIBX').dependsOn('prepareJIBX') << {
            project.javaexec {
                //compute arguments
                ArrayList<String> argList = new ArrayList<>();
                if (project.JIBXBinding.verbose) {
                    argList +="-v"
                }
                if (project.JIBXBinding.verify) {
                    argList +="-b"
                }
                if (project.JIBXBinding.overrideErrors) {
                    argList +="-o"
                }
                if (project.JIBXBinding.skipBindValidation) {
                    argList +="-s"
                }
                if (project.JIBXBinding.trackBranches) {
                    argList +="-t"
                }
                if (project.JIBXBinding.testLoading) {
                    argList +="-l"
                }
                argList.addAll(project.JIBXBinding.bindingFiles)
                main = 'org.jibx.binding.Compile'
                args = argList
                debug = false
                classpath = project.configurations.jibxRuntime.asFileTree + project.files(project.JIBXBinding.tempClassFolder)
                jvmArgs = project.JIBXBinding.jibxTaskJvmArgs
            }
        }


        project.task('postJIBX').dependsOn('runJIBX') << {
            //copy instrumented classes back to output dir
            project.copy {
                from project.JIBXBinding.tempClassFolder
                into project.sourceSets.main.output.classesDir
                include project.JIBXBinding.rootAPIPath+'/**/*.*'
            }
        }

        // hooking JIBX on "classes" task from java plugin
        project.getTasks().getByName('classes').dependsOn('postJIBX')
    }
}
