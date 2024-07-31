package org.digma.intellij.plugin.common

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.UntraceableException
import kotlinx.datetime.Clock
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

class FrequencyDetector(cacheExpirationTime: java.time.Duration) {

    private val myCache = MyCache(cacheExpirationTime.toKotlinDuration())


    companion object {

        val logger = Logger.getInstance(FrequencyDetector::class.java)

        private val BACKTRACE_FIELD: Field? = try {
            val field = Throwable::class.java.getDeclaredField("backtrace")
            field.isAccessible = true
            field
        } catch (e: Throwable) {
            //never use ErrorReporter here , it will cause an endless recursion
            logger.warn(e)
            null
        }

        private fun getBacktrace(throwable: Throwable): Array<*>? {
            // the JVM blocks access to Throwable.backtrace via reflection sometimes
            val backtrace = try {
                BACKTRACE_FIELD?.get(throwable)
            } catch (e: Throwable) {
                //never use ErrorReporter here , it will cause an endless recursion
                logger.warn(e)
                return null
            }
            return if (backtrace is Array<*>) backtrace else null
        }
    }

    fun isTooFrequentMessage(message: String): Boolean {
        return getMessageFrequency(message).isTooFrequent()
    }

    fun getMessageFrequency(message: String): Frequency {
        return myCache.getOrCreate(message)
    }

    fun isTooFrequentStackTrace(message: String, stackTrace: String?): Boolean {
        return getStackTraceFrequency(message, stackTrace).isTooFrequent()
    }

    fun getStackTraceFrequency(message: String, stackTrace: String?): Frequency {
        return stackTrace?.let {
            val hash = it.hashCode()
            myCache.getOrCreate(message, hash.toString())
        } ?: getErrorFrequency(message, "")
    }


    fun isTooFrequentError(message: String, action: String): Boolean {
        return getErrorFrequency(message, action).isTooFrequent()
    }

    fun getErrorFrequency(message: String, action: String): Frequency {
        return myCache.getOrCreate(message, action)
    }

    fun isTooFrequentException(message: String, t: Throwable): Boolean {
        return getExceptionFrequency(message, t).isTooFrequent()
    }

    fun getExceptionFrequency(message: String, t: Throwable): Frequency {
        val hash = computeAccurateTraceHashCode(t)
        return myCache.getOrCreate(hash, t, message)
    }

    private fun computeAccurateTraceHashCode(throwable: Throwable): Int {
        val backtrace = getBacktrace(throwable)
        if (backtrace == null) {
            val trace = if (throwable is UntraceableException) null else throwable.stackTrace
            return trace.contentHashCode()
        }
        return backtrace.contentDeepHashCode()
    }

}


data class Frequency(val frequencyInLastPeriod: Int, val frequencySinceStart: Int, val duration: Duration) {
    fun isTooFrequent(): Boolean {
        return frequencyInLastPeriod > 1
    }

    fun formatDurationToMinutes(): Long {
        return duration.inWholeMinutes
    }
}


private class MyCache(cacheExpirationTime: Duration) {

    private val startTime: kotlinx.datetime.Instant = Clock.System.now()

    private val cache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(cacheExpirationTime.inWholeMinutes, TimeUnit.MINUTES)
        .build<String, AtomicInteger>()

    private val frequencies = Caffeine.newBuilder()
        .maximumSize(1000)
        .build<String, AtomicInteger>()


    fun getOrCreate(hash: Int, t: Throwable, message: String): Frequency {
        return getFrequency("$hash:$t:$message")
    }

    fun getOrCreate(message: String, action: String): Frequency {
        return getFrequency("$message:$action")
    }

    fun getOrCreate(message: String): Frequency {
        return getFrequency(message)
    }

    private fun getFrequency(key: String): Frequency {
        val frequencyInLastPeriod = cache.get(key) { AtomicInteger() }.incrementAndGet()
        val frequencySinceStart = frequencies.get(key) { AtomicInteger() }.incrementAndGet()
        return Frequency(frequencyInLastPeriod, frequencySinceStart, Clock.System.now().minus(startTime))
    }

}