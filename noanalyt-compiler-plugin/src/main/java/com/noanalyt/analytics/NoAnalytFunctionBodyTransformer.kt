package com.noanalyt.analytics

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.isAnonymous
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

interface ModuleLoweringPass {
    fun lower(module: IrModuleFragment)
}

class NoAnalytFunctionBodyTransformer(
    private val context: IrPluginContext,
    private val messageCollector: MessageCollector,
    private val noAnalytVersion: String,
    private val projectPath: String,
    private val whiteSet: WhiteSet,
) : IrElementTransformerVoid(), ModuleLoweringPass,
    FileLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    private var lastAnonymousIndex: Int = 0
    private fun nextAnonymousIndex(): Int = lastAnonymousIndex++

    private var lastConstructorIndex: Int = 0
    private fun nextConstructorIndex(): Int = lastConstructorIndex++

    private var lastReceiverIndex: Int = 0
    private fun nextReceiverIndex(): Int = lastReceiverIndex++

    private var lastSpecialIndex: Int = 0
    private fun nextSpecialIndex(): Int = lastSpecialIndex++

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val irTraceLogFunction by guardedLazy {
        val paramsType = listOf(
            context.irBuiltIns.stringType,
        )
        getTopLevelFunctions(log).singleOrNull { fn ->
            fn.owner.valueParameters.size == paramsType.size &&
                    fn.owner.valueParameters
                        .mapIndexed { i, p -> p.type == paramsType[i] }.all { it }
        }?.owner
    }

    private fun irTraceEvent(
        packageFqName: String,
        className: String,
        methodName: String,
        methodType: String
    ): IrExpression? =
        irTraceLogFunction?.let { function ->
            irCall(
                function,
                args = arrayOf(
                    irConst("$noAnalytVersion;$projectPath;$packageFqName;$className;$methodName;$methodType"),
                )
            )
        }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.getPackageFragment().packageFqName == rootFqName) {
            return declaration
        }
        val packageFqName = declaration.getPackageFragment().packageFqName.asString()
        val className = declaration.parentClassOrNull?.name?.asString() ?: ""
        val name = declaration.name
        val nameAsString = name.asString()
        val methodName =
            name.identifierOrNullIfSpecial ?: if (name.isAnonymous) {
                "<anonymous${nextAnonymousIndex()}"
            } else if (name == SpecialNames.INIT) {
                "<init${nextConstructorIndex()}"
            } else if (name == SpecialNames.RECEIVER) {
                "<receiver${nextReceiverIndex()}"
            } else {
                nameAsString.substring(0, nameAsString.length - 1) + nextSpecialIndex()
            }
        val methodType =
            "(${declaration.valueParameters.joinToString(",") { it.type.classFqName?.asString() ?: "" }}" +
                    ")->${declaration.returnType.classFqName?.asString()}"

        if (whiteSet.isWhite(
                projectPath,
                packageFqName,
                className,
                methodName,
                methodType
            ).not()
        ) {
            return declaration
        }

        val irNoAnalytLogEvent = irTraceEvent(packageFqName, className, methodName, methodType)
        messageCollector.report(
            CompilerMessageSeverity.INFO,
            "irNoAnalytLogEvent is $irNoAnalytLogEvent "
        )

        val body = declaration.body ?: return declaration
        val (nonReturningBody, returnVar) = body.asBodyAndResultVar(declaration)
        val transformed = nonReturningBody.apply { transformChildrenVoid() }
        declaration.body =
            context.irFactory.createBlockBody(body.startOffset, body.endOffset).apply {
                this.statements.addAll(
                    listOfNotNull(
                        irNoAnalytLogEvent,
                        transformed,
                        returnVar?.let { irReturnVar(declaration.symbol, it) }
                    )
                )
            }
        return declaration
    }

    private fun getTopLevelFunctions(callableId: CallableId): Sequence<IrSimpleFunctionSymbol> {
        return context.referenceFunctions(callableId).asSequence()
    }

    private fun irReturnVar(
        target: IrReturnTargetSymbol,
        value: IrVariable,
    ): IrExpression {
        return IrReturnImpl(
            value.initializer?.startOffset ?: UNDEFINED_OFFSET,
            value.initializer?.endOffset ?: UNDEFINED_OFFSET,
            value.type,
            target,
            irGet(value)
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBody.asBodyAndResultVar(
        expectedTarget: IrFunction? = null,
    ): Pair<IrContainerExpression, IrVariable?> {
        val original = IrCompositeImpl(
            startOffset,
            endOffset,
            context.irBuiltIns.unitType,
            null,
            statements
        )
        var block: IrStatementContainer? = original
        var expr: IrStatement? = block?.statements?.lastOrNull()
        while (expr != null && block != null) {
            if (
                expr is IrReturn &&
                (expectedTarget == null || expectedTarget == expr.returnTargetSymbol.owner)
            ) {
                block.statements.pop()
                val valueType = expr.value.type
                val returnType = (expr.returnTargetSymbol as? IrFunctionSymbol)?.owner?.returnType
                    ?: valueType
                return if (returnType.isUnit() || returnType.isNothing() || valueType.isNothing()) {
                    block.statements.add(expr.value)
                    original to null
                } else {
                    val temp = irTemporary(
                        expr.value,
                        "returnTemp${nextTemporaryIndex()}",
                        expectedTarget?.parent,
                    )
                    block.statements.add(temp)
                    original to temp
                }
            }
            if (expr !is IrBlock)
                return original to null
            block = expr
            expr = block.statements.lastOrNull()
        }
        return original to null
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun irCall(
        function: IrFunction,
        origin: IrStatementOrigin? = null,
        dispatchReceiver: IrExpression? = null,
        extensionReceiver: IrExpression? = null,
        vararg args: IrExpression,
    ): IrCall {
        val type = function.returnType
        val symbol = function.symbol
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            origin
        ).also {
            if (dispatchReceiver != null) it.dispatchReceiver = dispatchReceiver
            if (extensionReceiver != null) it.extensionReceiver = extensionReceiver
            args.forEachIndexed { index, arg ->
                it.putValueArgument(index, arg)
            }
        }
    }

    private fun irConst(value: String): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.stringType,
        IrConstKind.String,
        value
    )

    private fun irTemporary(
        value: IrExpression,
        name: String,
        irParent: IrDeclarationParent?,
        irType: IrType = value.type,
        isVar: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    ): IrVariableImpl {
        return IrVariableImpl(
            value.startOffset,
            value.endOffset,
            origin,
            IrVariableSymbolImpl(),
            Name.identifier(name),
            irType,
            isVar,
            isConst = false,
            isLateinit = false
        ).apply {
            initializer = value
            irParent?.let {
                parent = it
            }
        }
    }

    private fun irGet(type: IrType, symbol: IrValueSymbol): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol
        )
    }

    private fun irGet(variable: IrValueDeclaration): IrExpression {
        return irGet(variable.type, variable.symbol)
    }
}
