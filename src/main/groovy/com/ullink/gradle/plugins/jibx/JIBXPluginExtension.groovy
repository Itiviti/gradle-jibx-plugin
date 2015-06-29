package com.ullink.gradle.plugins.jibx

class JIBXPluginExtension {

    String rootPath = ''
    List<File> bindingFiles = []
    List<File> classPathFiles = []

    boolean verbose = false
    boolean verify = false
    boolean overrideErrors = false
    boolean skipBindValidation = false
    boolean trackBranches = false
    boolean testLoading = false
    boolean archiveUnboundJar = false

}
