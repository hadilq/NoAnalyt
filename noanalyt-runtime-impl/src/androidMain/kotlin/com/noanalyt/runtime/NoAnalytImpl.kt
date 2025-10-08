package com.noanalyt.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

internal class NoAnalytImpl : NoAnalyt {
    private val scope: CoroutineScope =
        CoroutineScope(Dispatchers.IO.limitedParallelism(1, "noanalyt"))
    private val keeper: Array<NoAnalytData?> = Array(KEEPER_SIZE) { null }
    private var isCycleTraced: Boolean = false
    private var index: Int = 0

    override fun log(data: NoAnalytData) {
        scope.launch {
            if (testsRunning.value) {
                println("${data.millis}, ${data.nanos}, ${data.key}")
            } else {
                dispatch(data)
            }
        }


    }

    private fun dispatch(data: NoAnalytData) {
        keeper[index] = data
        if (index == KEEPER_SIZE - 1) {
            isCycleTraced = true
            index = 0
        }
        // TODO check threshold to make a page and send it
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

internal class AnalyticsData(
    val millis: Long,
    val nanos: Int,
    val module: String,
    val packageFqName: String,
    val parentClass: String,
    val funName: String,
)

internal const val KEEPER_SIZE = 1000
