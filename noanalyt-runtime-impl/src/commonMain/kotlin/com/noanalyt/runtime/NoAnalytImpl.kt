package com.noanalyt.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

// TODO set this on onCreate
internal class NoAnalytImpl : NoAnalyt {
    private val scope: CoroutineScope =
        CoroutineScope(Dispatchers.IO.limitedParallelism(1, "noanalyt"))
    private val keeper: Array<NoAnalytData?> = Array(KEEPER_SIZE) { null }
    private var isCycleTraced: Boolean = false
    private val index: Int = 0

    override fun log(data: NoAnalytData) {
        scope.launch {
            if (testsRunning.value) {
                println("${data.millis}, ${data.nanos}, ${data.module}, ${data.packageFqName}, ${data.parentClass}, ${data.funName}")
            }
            keeper[index] = data
            if (index == KEEPER_SIZE) {
                isCycleTraced = true
            }
            // TODO check threshold to make a page and send it
        }


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
