package org.digma.intellij.plugin.kotlin.ext

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.ApiErrorHandler
import org.digma.intellij.plugin.analytics.isNoAccountInCentralized
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Runs the block and reports errors ignoring CancellationException.
 */
//todo: maybe use CoroutineExceptionHandler to catch exceptions. if used maybe there is no need to try/catch in the code and ignore CancellationException
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
        Log.warnWithException(logger, e, "Error in coroutine {}, {}", name, e)
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
): Job = this.launchWhileActiveWithErrorReporting(startupDelay, myDelay, false, name, logger, context, start, block)


fun CoroutineScope.launchWhileActiveWithErrorReporting(
    startupDelay: kotlin.time.Duration?,
    myDelay: kotlin.time.Duration,
    pausable: Boolean,
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
        if (paused(pausable)) {
            delay(myDelay)
            continue
        }

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


fun <T> CoroutineScope.asyncWithErrorReporting(
    name: String,
    logger: Logger,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = async(context + CoroutineName(name), start) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e // ⚠️ Always rethrow to propagate cancellation properly
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "Error in coroutine {}", e)
        ErrorReporter.getInstance().reportError("asyncWithErrorReporting.$name", e)
        throw e
    }
}


private fun paused(passable: Boolean): Boolean {
    return try {
        passable &&
                (ApiErrorHandler.getInstance().isNoConnectionMode() || isNoAccountInCentralized())
    } catch (_: Throwable) {
        false
    }
}