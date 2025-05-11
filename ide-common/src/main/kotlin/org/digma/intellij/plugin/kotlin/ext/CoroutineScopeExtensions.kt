package org.digma.intellij.plugin.kotlin.ext

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * This method implements the same idiom.
 * Runs the block and reports errors ignoring CancellationException.
 */
fun CoroutineScope.launchWithErrorReporting(
    name: String,
    logger: Logger,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context + CoroutineName(name), start) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e // ⚠️ Always rethrow to propagate cancellation properly
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "Error in coroutine {}", name)
        ErrorReporter.getInstance().reportError("launchWithErrorReporting.$name", e)
    }
}


/**
 * Runs the block while the coroutine is active where there may be a startup delay. Always a constant delay between iterations.
 * This method is not suitable if the delay may change between iterations, or if the while condition is more complex than isActive,
 * or if there is something to do with exceptions other than reporting.
 */
fun CoroutineScope.launchWhileActiveWithErrorReporting(
    startupDelay: kotlin.time.Duration?,
    myDelay: kotlin.time.Duration,
    name: String,
    logger: Logger,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context + CoroutineName(name), start) {
    startupDelay?.let {
        delay(it)
    }
    while (isActive) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e // ⚠️ Always rethrow to propagate cancellation properly
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Error in coroutine {},{}", name, e)
            ErrorReporter.getInstance().reportError("launchWhileActiveWithErrorReporting.$name", e)
        }
        delay(myDelay)
    }
}


