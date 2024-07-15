package org.digma.intellij.plugin.common

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.diagnostic.UntraceableException
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

class FrequencyDetector(cacheExpirationTime: java.time.Duration) {

    private val myCache = MyCache(cacheExpirationTime.toKotlinDuration())


    fun isTooFrequentMessage(message: String): Boolean {
        val counter = myCache.getOrCreate(message)
        val occurrences = counter.incrementAndGet()
        return occurrences > 1
    }

    fun isTooFrequentStackTrace(message: String, stackTrace: String?): Boolean {
        //this method is meant to test stack trace which may be a long string.
        //its better not to use a long string as key, so we use the stacktrace hash.
        //if stacktrace is null just check isTooFrequentError

        return stackTrace?.let {
            val hash = it.hashCode()
            val counter = myCache.getOrCreate(message, hash.toString())
            val occurrences = counter.incrementAndGet()
            occurrences > 1
        } ?: isTooFrequentError(message, "")
    }

    fun isTooFrequentError(message: String, action: String): Boolean {
        val counter = myCache.getOrCreate(message, action)
        val occurrences = counter.incrementAndGet()
        return occurrences > 1
    }

    fun isTooFrequentException(message: String, t: Throwable): Boolean {
        val hash = computeAccurateTraceHashCode(t)
        val counter = myCache.getOrCreate(hash, t, message)
        val occurrences = counter.incrementAndGet()
        return occurrences > 1
    }

    private fun computeAccurateTraceHashCode(throwable: Throwable): Int {
        val backtrace = getBacktrace(throwable)
        if (backtrace == null) {
            val trace = if (throwable is UntraceableException) null else throwable.stackTrace
            return trace.contentHashCode()
        }
        return backtrace.contentDeepHashCode()
    }


    private fun getBacktrace(throwable: Throwable): Array<Any>? {

        val backtrace = try {

            val backtraceField: Field? = com.intellij.util.ReflectionUtil.getDeclaredField(Throwable::class.java, "backtrace")
            if (backtraceField != null) {
                backtraceField.isAccessible = true
                backtraceField.get(throwable)
            } else {
                null
            }

        } catch (e: Throwable) {
            null
        }

        if (backtrace != null && backtrace is Array<*> && backtrace.isArrayOf<Any>()) {
            @Suppress("UNCHECKED_CAST")
            return backtrace as Array<Any>
        }

        return null

    }

}


private class MyCache(cacheExpirationTime: Duration) {
    private val cache = Caffeine.newBuilder()
        .maximumSize(1000)
        //expireAfterAccess means we don't send the error as long as it keeps occurring until it is quite for this time,
        //if it reappears send it again
//        .expireAfterAccess(10, TimeUnit.MINUTES)
        //expireAfterWrite mean the error will be sent in fixed intervals.
//        .expireAfterWrite(24, TimeUnit.HOURS)
        .expireAfterWrite(cacheExpirationTime.inWholeMinutes, TimeUnit.MINUTES)
        .build<String, AtomicInteger>()

    fun getOrCreate(hash: Int, t: Throwable, message: String): AtomicInteger {
        return cache.get("$hash:$t:$message") { AtomicInteger() }
    }

    fun getOrCreate(message: String, action: String): AtomicInteger {
        return cache.get("$message:$action") { AtomicInteger() }
    }

    fun getOrCreate(message: String): AtomicInteger {
        return cache.get(message) { AtomicInteger() }
    }
}