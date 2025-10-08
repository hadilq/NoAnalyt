package com.noanalyt.runtime

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.datetime.Clock

fun configure(areTestsRunning: Boolean) {
    testsRunning.update { areTestsRunning }
}

@Suppress("unused")
fun log(key: String) {
    val now = Clock.System.now()
    NoAnalyt.getInstance().log(
        NoAnalytData(
            noAnalytVersion,
            now.toEpochMilliseconds(),
            now.nanosecondsOfSecond,
            key,
        )
    )
}

val testsRunning = atomic(false)

interface NoAnalyt {
    fun log(data: NoAnalytData)
    fun dispatchFrom(instance: NoAnalyt)
    fun receive(): Array<NoAnalytData?>

    companion object {
        private var _instance: NoAnalyt = NoAnalytTmp()

        fun setInstance(instance: NoAnalyt) {
            instance.dispatchFrom(_instance)
            _instance = instance
        }

        fun getInstance(): NoAnalyt {
            return _instance
        }
    }
}

internal class NoAnalytTmp : NoAnalyt {
    private val keeper: Array<NoAnalytData?> = Array(KEEPER_SIZE) { null }
    private var isCycleTraced: Boolean = false
    private val index: AtomicInt = atomic(0)

    override fun log(data: NoAnalytData) {
        if (testsRunning.value) {
            println("runtime.version;${data.millis};${data.nanos};${data.key}")
        } else {
            dispatch(data)
        }
    }

    private fun dispatch(data: NoAnalytData) {
        index.getAndUpdate { index ->
            keeper[index] = data
            if (index == KEEPER_SIZE - 1) {
                isCycleTraced = true
                0
            } else index + 1
        }
    }

    override fun dispatchFrom(instance: NoAnalyt) {
        instance.receive().asSequence().filterNotNull().forEach { data ->
            dispatch(data)
        }
    }

    override fun receive(): Array<NoAnalytData?> {
        return keeper
    }
}

class NoAnalytData(
    val noAnalytVersion: String,
    val millis: Long,
    val nanos: Int,
    val key: String,
)

internal const val KEEPER_SIZE = 1000
