package com.noanalyt.analytics

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class NoAnalytIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val noAnalytVersion: String,
    private val projectPath: String,
    private val whiteSet: WhiteSet,
) :
    IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        NoAnalytFunctionBodyTransformer(
            pluginContext,
            messageCollector,
            noAnalytVersion,
            projectPath,
            whiteSet,
        ).lower(moduleFragment)
    }
}
