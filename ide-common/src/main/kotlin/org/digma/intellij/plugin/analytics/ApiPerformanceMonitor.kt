package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.FrequencyDetector
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration


@Service(Service.Level.PROJECT)
class ApiPerformanceMonitor(private val project:Project) {

    private val frequencyDetector = FrequencyDetector(10.minutes.toJavaDuration())

    private val durations = ConcurrentHashMap(mutableMapOf<String,Long>())

    fun addPerformance(apiName: String, duration: Long) {
        if (duration < 2000) {
            return
        }

        durations[apiName] = max(duration, durations[apiName] ?: 0L)

        durations[apiName]?.let {
            report(apiName, it)
        }
    }

    private fun report(apiName: String, duration: Long) {
        if (frequencyDetector.isTooFrequentMessage(apiName)){
            return
        }

        //reset the max duration before reporting, new one will be collected
        durations[apiName] = 0L

        val details = mutableMapOf<String,Any>(
            "api name" to apiName,
            "duration" to duration
        )

        ActivityMonitor.getInstance(project).reportApiPerformanceIssue(details)

    }


}