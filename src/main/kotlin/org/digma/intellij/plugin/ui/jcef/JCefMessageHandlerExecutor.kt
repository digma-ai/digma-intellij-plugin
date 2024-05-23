package org.digma.intellij.plugin.ui.jcef

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class JCefMessageHandlerExecutor(
    private val nThreads: Int,
    private val name: String
) : ExecutorCoroutineDispatcher() {
    private val threadNo = AtomicInteger()

    override val executor: Executor =
        Executors.newScheduledThreadPool(nThreads) { target ->
            PoolThread(
                this@JCefMessageHandlerExecutor,
                target,
                if (nThreads == 1) name else name + "-" + threadNo.incrementAndGet()
            )
        }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.execute(block)
    }

    override fun close() {
        (executor as ExecutorService).shutdown()
    }

    override fun toString(): String = "ThreadPoolDispatcher[$nThreads, $name]"
}


class PoolThread(
    @JvmField val dispatcher: JCefMessageHandlerExecutor, // for debugging & tests
    target: Runnable, name: String
) : Thread(target, name) {
    init {
        isDaemon = true
    }
}

fun newSingleThreadExecutor(name: String): JCefMessageHandlerExecutor {
    return JCefMessageHandlerExecutor(1, name)
}

fun newExecutor(nThreads: Int, name: String): JCefMessageHandlerExecutor {
    return JCefMessageHandlerExecutor(nThreads, name)
}


