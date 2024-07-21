package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.FrequencyDetector
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.Collections
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration


@Service(Service.Level.PROJECT)
class ApiPerformanceMonitor(private val project: Project) {

    private val frequencyDetector = FrequencyDetector(10.minutes.toJavaDuration())

    private val durations = Collections.synchronizedMap(mutableMapOf<String, Pair<Long, Long>>())

    //this method is not thread safe. max duration for apiName may not be the real max if two threads for
    // the same apiName run concurrently, and they have different durations.
    //it's not a bug just not completely accurate reporting.
    //the only way to make it accurate is to lock the code that computes max and puts it in the durations map.
    //but locking means that threads will wait for each other.
    //it's possible to execute this code in a coroutine, and then It's ok to lock, but the error is minor and doesn't
    // worth the complexity of a coroutine.
    fun addPerformance(apiName: String, duration: Long, httpNetoTime: Long, exception: Throwable?) {
        if (duration < 2000) {
            return
        }

        val current = Pair(duration, httpNetoTime)
        val latest = durations[apiName] ?: Pair(0L, 0L)
        //put the max in the map
        val toReport = if (current.first > latest.first) {
            durations[apiName] = current
            current
        } else {
            durations[apiName] = latest
            latest
        }

        report(apiName, toReport.first, toReport.second, exception)
    }

    private fun report(apiName: String, duration: Long, httpNetoTime: Long, exception: Throwable?) {
        if (frequencyDetector.isTooFrequentMessage(apiName)) {
            return
        }

        //reset the max duration before reporting, new one will be collected
        durations[apiName] = Pair(0L, 0L)

        val details = mutableMapOf<String, Any>(
            "api name" to apiName,
            "duration" to duration,
            "http neto" to httpNetoTime,
            "exception" to (exception?.message ?: exception?.toString() ?: "")
        )

        ActivityMonitor.getInstance(project).reportApiPerformanceIssue(details)

    }


}