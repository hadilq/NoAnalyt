package com.noanalyt

import com.noanalyt.analytics.NoAnalytIrGenerationExtension
import com.noanalyt.analytics.PilotWhiteSet
import com.noanalyt.analytics.ProductionWhiteSet
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class NoAnalytPluginRegistrar() : CompilerPluginRegistrar() {

    constructor(
        testNoAnalytVersion: String,
        testProjectPath: String,
        testBuildConfig: BuildConfig,
        testConfigFile: File,
    ) : this() {
        this.testProjectPath = testProjectPath
        this.testNoAnalytVersion = testNoAnalytVersion
        this.testBuildConfig = testBuildConfig
        this.testConfigFile = testConfigFile
    }

    private var testProjectPath: String? = null
    private var testNoAnalytVersion: String? = null
    private var testBuildConfig: BuildConfig? = null
    private var testConfigFile: File? = null

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) return

        val realMessageCollector = configuration.get(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE
        )

        val messageCollector = configuration.get(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            realMessageCollector
        )

        val noAnalytVersion = configuration[KEY_NO_ANALYT_VERSION] ?: testNoAnalytVersion ?: run {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "no analyt version is null"
            )
            return
        }

        val projectPath = configuration[KEY_PROJECT_PATH] ?: testProjectPath ?: run {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "project path is null"
            )
            return
        }

        val buildConfig = configuration[KEY_BUILD_CONFIG] ?: testBuildConfig ?: run {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "build config is null"
            )
            return
        }

        val configFile = configuration[KEY_CONFIG_FILE] ?: testConfigFile ?: run {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "config file is null"
            )
            return
        }

        val whiteSet = when (buildConfig) {
            BuildConfig.pilot -> PilotWhiteSet()
            BuildConfig.production -> ProductionWhiteSet().prepare(configFile)
        }

        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "noanalyt compiler plugin is starting!"
        )

        IrGenerationExtension.registerExtension(
            NoAnalytIrGenerationExtension(messageCollector, noAnalytVersion, projectPath, whiteSet)
        )
    }
}
