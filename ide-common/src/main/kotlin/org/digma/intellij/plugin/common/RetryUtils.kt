package org.digma.intellij.plugin.common

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
