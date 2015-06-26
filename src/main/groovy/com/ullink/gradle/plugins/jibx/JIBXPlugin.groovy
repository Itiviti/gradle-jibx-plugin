package com.ullink.gradle.plugins.jibx

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar

class JIBXPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.configurations.create('jibxBindingReference')
        project.configurations.create('jibxRuntime').extendsFrom(project.configurations.jibxBindingReference)

        project.extensions.create("JIBXBinding", JIBXPluginExtension)

        //force cleanup since incremental build on this plugin is not yet working well
        project.sourceSets.main.output.classesDir.deleteDir();

        def generateUnboundJar = project.task(type: Jar, dependsOn: 'JIBXResources', 'generateUnboundJar') {
            classifier = 'nojibxbinding'
            from project.sourceSets.main.output.files
        }

        project.configure(project) {
            afterEvaluate {

                project.JIBXBinding.bindingDir = new File(project.buildDir,JIBXPluginExtension.DEFAULT_BINDING_DIR)
                project.JIBXBinding.tempClassFolder = new File(project.JIBXBinding.bindingDir,JIBXPluginExtension.TEMP_TARGET_DIR)
                project.test.classpath = project.files(project.JIBXBinding.tempClassFolder) + project.test.classpath

                project.task('JIBXResources') << {
                    project.JIBXBinding.bindingDir.mkdirs()
                    if (project.JIBXBinding.tempClassFolder.exists()) {
                        project.JIBXBinding.tempClassFolder.deleteDir()
                    }
                    project.JIBXBinding.tempClassFolder.mkdirs()
                    def jibxDir = new File(project.JIBXBinding.tempClassFolder,project.JIBXBinding.rootAPIPath+'/jibx')
                    jibxDir.mkdirs()
                }

                project.task('prepareJIBX').dependsOn('JIBXResources') << {
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

                    project.JIBXBinding.JIBXExternalJars.each {
                        path,jar ->
                            def pathBinding = new File(project.JIBXBinding.bindingDir,path)
                            pathBinding.mkdirs();
                            project.copy {
                                from project.configurations.jibxRuntime.files.findAll{it.name.matches(".*$jar.*")}.collect{project.zipTree(it)}.files
                                into pathBinding;
                                include '**/*.xml'
                            }
                    }

                }

                project.task(dependsOn: ['prepareJIBX','generateUnboundJar'], 'runJIBX') << {
                    def ext = project.JIBXBinding
                    def classpath = project.files(project.JIBXBinding.tempClassFolder) + project.configurations.jibxRuntime.asFileTree
                    def classLoader = URLClassLoader.newInstance(classpath.collect { dep -> dep.toURI().toURL() } as URL[])
                    def compilerClass = Class.forName('org.jibx.binding.Compile', true, classLoader)
                    def compiler = compilerClass.newInstance(ext.verbose, ext.testLoading, ext.verify, ext.trackBranches, ext.overrideErrors)
                    compiler.setSkipValidate(ext.skipBindValidation)
                    compiler.compile(classpath.collect { dep -> dep.toString() } as String[], project.JIBXBinding.bindingFiles as String[])
                }


                project.task(type: Copy, dependsOn: 'runJIBX', 'postJIBX') {
                    from project.JIBXBinding.tempClassFolder
                    into project.sourceSets.main.output.classesDir
                    include project.JIBXBinding.rootAPIPath+'/**/*.*'
                }

                generateUnboundJar.onlyIf { project.JIBXBinding.archiveUnboundJar }

                // hooking JIBX on tasks from java plugin
                project.getTasks().getByName('jar').dependsOn('postJIBX')
                project.getTasks().getByName('test').dependsOn('postJIBX')

            }
        }



    }
}
