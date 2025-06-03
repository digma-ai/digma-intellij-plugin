package org.digma.intellij.plugin.common

import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException


fun <T> retry(
    times: Int = 3,
    delayMillis: Long = 100,
    block: () -> T
): T {
    var lastException: Throwable? = null
    repeat(times) {
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            if (it < times - 1) Thread.sleep(delayMillis)
        }
    }
    throw lastException ?: IllegalStateException("retry() failed with no exception?")
}


fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 100,
    factor: Double = 2.0,
    block: () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Thread.sleep(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block() // last attempt (let exception propagate)
}


suspend fun <T> suspendableRetry(
    times: Int = 3,
    delayMillis: Long = 100,
    block: suspend () -> T
): T {
    var lastException: Throwable? = null
    repeat(times) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            lastException = e
            if (it < times - 1) delay(delayMillis)
        }
    }
    throw lastException ?: IllegalStateException("retry() failed with no exception?")
}


suspend fun <T> suspendableRetryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 100,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block() // last attempt (let exception propagate)
}





