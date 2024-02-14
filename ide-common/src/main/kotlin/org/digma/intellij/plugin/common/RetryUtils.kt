package org.digma.intellij.plugin.common

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Computable
import java.util.function.Supplier


/*

less verbose utilities to use Retries

example usage:

runWIthRetryWithResult({
    spanDiscovery.discoverSpans(project, psiFile)
},maxRetries = 5)

runWIthRetryWithResult({
    spanDiscovery.discoverSpans(project, psiFile)
}, retryOnException = IndexNotReadyException::class.java, backOffMillis = 20, maxRetries = 5)

 */



fun runWIthRetry(
    runnable: Runnable,
    retryOnException: Class<out Throwable> = Throwable::class.java,
    backOffMillis: Int = 50,
    maxRetries: Int = 5,
) {
    Retries.simpleRetry(runnable, retryOnException, backOffMillis, maxRetries)
}

fun <T> runWIthRetryWithResult(
    tSupplier: Supplier<T>,
    retryOnException: Class<out Throwable> = Throwable::class.java,
    backOffMillis: Int = 50,
    maxRetries: Int = 5,
): T {
    return Retries.retryWithResult(tSupplier, retryOnException, backOffMillis, maxRetries)
}


fun runWIthRetryIgnorePCE(
    runnable: Runnable,
    retryOnException: Class<out Throwable> = Throwable::class.java,
    delayMillis: Int = 50,
    maxRetries: Int = 5,
) {

    repeat(maxRetries) { count ->

        try {
            runnable.run()
            return
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            if (!retryOnException.isAssignableFrom(e::class.java) ||
                count >= maxRetries - 1
            ) {
                throw e
            }
            Thread.sleep(delayMillis.toLong())
        }
    }
}

fun <T> runWIthRetryWithResultIgnorePCE(
    computable: Computable<T>,
    retryOnException: Class<out Throwable> = Throwable::class.java,
    delayMillis: Int = 50,
    maxRetries: Int = 5,
): T {

    var error: Throwable? = null
    repeat(maxRetries) { count ->

        try {
            return computable.compute()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            error = e
            if (!retryOnException.isAssignableFrom(e::class.java) ||
                count >= maxRetries - 1
            ) {
                throw e
            }

            Thread.sleep(delayMillis.toLong())
        }
    }

    //this should never happen but the compiler can't see that and complains on return statement
    error?.let {
        throw it
    } ?: throw IllegalStateException("Something went wrong in retry")
}













