package com.noanalyt

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class NoAnalytGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("noanalyt", NoAnalytPluginExtension::class.java)
        project.dependencies.add("implementation", "com.noanalyt:noanalyt-runtime-api:${noAnalytVersionOf(project)}")
        project.plugins.apply(NoAnalytGradleSubPlugin::class.java)
    }
}

open class NoAnalytPluginExtension {
    var enabled: Boolean = true
}
