package com.noanalyt

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

private const val PLUGIN_ID = "com.noanalyt.compiler.plugin"
private const val ENABLED = "enabled"
private const val NO_ANALYT_VERSION = "noAnalytVersion"
private const val BUILD_CONFIG = "buildConfig"
private const val CONFIG_FILE = "configFileAbsolutPath"
private const val PROJECT_PATH = "projectPath"

internal val KEY_ENABLED = CompilerConfigurationKey<Boolean>(ENABLED)
internal val KEY_NO_ANALYT_VERSION = CompilerConfigurationKey<String>(NO_ANALYT_VERSION)
internal val KEY_CONFIG_FILE = CompilerConfigurationKey<File>(CONFIG_FILE)
internal val KEY_BUILD_CONFIG = CompilerConfigurationKey<BuildConfig>(BUILD_CONFIG)
internal val KEY_PROJECT_PATH = CompilerConfigurationKey<String>(PROJECT_PATH)

@Suppress("EnumEntryName")
enum class BuildConfig {
    pilot, production,
}

@OptIn(ExperimentalCompilerApi::class)
class NoAnalytCommandLine : CommandLineProcessor {
    override val pluginId: String get() = PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            ENABLED, "<true|false>", "", required = false,
            allowMultipleOccurrences = false,
        ),
        CliOption(
            NO_ANALYT_VERSION, "<version>", "", required = false,
            allowMultipleOccurrences = false,
        ),
        CliOption(
            BUILD_CONFIG, "<pilot|production>", "", required = false,
            allowMultipleOccurrences = false,
        ),
        CliOption(
            CONFIG_FILE, "<path>", "", required = false,
            allowMultipleOccurrences = false,
        ),
        CliOption(
            PROJECT_PATH, "<gradle-path>", "", required = false,
            allowMultipleOccurrences = false,
        ),
    )

    @Throws(CliOptionProcessingException::class)
    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        ENABLED -> configuration.put(KEY_ENABLED, value == "true")
        NO_ANALYT_VERSION -> configuration.put(KEY_NO_ANALYT_VERSION, value)
        CONFIG_FILE -> configuration.put(KEY_CONFIG_FILE, File(value))
        BUILD_CONFIG -> configuration.put(KEY_BUILD_CONFIG, BuildConfig.valueOf(value))
        PROJECT_PATH -> configuration.put(KEY_PROJECT_PATH, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}