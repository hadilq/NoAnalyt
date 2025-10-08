package com.noanalyt

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File

private const val PLUGIN_ID = "com.noanalyt.compiler.plugin"
private const val ENABLED = "enabled"
private const val NO_ANALYT_VERSION = "noAnalytVersion"
private const val BUILD_CONFIG = "buildConfig"
private const val CONFIG_FILE = "configFileAbsolutPath"
private const val PROJECT_PATH = "projectPath"

class NoAnalytGradleSubPlugin : KotlinCompilerPluginSupportPlugin {

    private lateinit var noAnalytVersion: String

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.hasPlugin(NoAnalytGradlePlugin::class.java)
    }

    override fun apply(target: Project) {
        noAnalytVersion = noAnalytVersionOf(target)
        super.apply(target)
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "com.noanalyt",
            artifactId = "noanalyt-compiler-plugin",
            version = noAnalytVersion,
        )


    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val noAnalytExtension = project.extensions.findByType(NoAnalytPluginExtension::class.java)
            ?: project.extensions.create("noanalyt", NoAnalytPluginExtension::class.java)

        // TODO add actual build configs
        val buildConfig = "pilot"
        val configDir = File(project.layout.buildDirectory.asFile.get(), "noanalyt-config")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        // TODO download the config file
        val configFile = File(configDir, "config")
        val projectPath = project.path

        val pluginOptions = listOf(
            SubpluginOption(ENABLED, value = noAnalytExtension.enabled.toString()),
            SubpluginOption(NO_ANALYT_VERSION, value = noAnalytVersion),
            SubpluginOption(BUILD_CONFIG, value = buildConfig),
            SubpluginOption(CONFIG_FILE, value = configFile.absolutePath ?: ""),
            SubpluginOption(PROJECT_PATH, value = projectPath),
        )

        return project.provider { pluginOptions }
    }
}

internal fun noAnalytVersionOf(target: Project): String = target.rootProject
    .extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")
    .findVersion("noanalyt")
    .get()
    .displayName
