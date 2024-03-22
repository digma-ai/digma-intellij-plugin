package org.digma.intellij.plugin.posthog

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse
import org.digma.intellij.plugin.persistence.PersistenceService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class PerformanceMetricsPosthogEventStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        service<ContinuousPerformanceMetricsReporter>().start(project)
    }
}


@Service(Service.Level.APP)
class ContinuousPerformanceMetricsReporter : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val started = AtomicBoolean(false)

    override fun dispose() {
        //nothing to do, used as parent disposable
    }


    fun start(project: Project) {

        if (started.getAndSet(true)) {
            Log.log(logger::info, "ContinuousPerformanceMetricsReporter.start called for project {} but already started", project.name)
            return
        }


        Log.log(logger::info, "ContinuousPerformanceMetricsReporter starting")

        @Suppress("UnstableApiUsage")
        disposingScope().launch {

            if (!PersistenceService.getInstance().isFirstTimePerformanceMetrics()) {
                waitForFirstTime(this)
                //after first time wait 6 hours for next report
                launchContinuousReport(6.hours)
            } else {
                //this will happen on startup, make sure we have a report in 10 minutes and then every 6 hours
                launchContinuousReport(10.minutes)
            }
        }

    }


    private suspend fun waitForFirstTime(coroutineScope: CoroutineScope) {

        while (!PersistenceService.getInstance().isFirstTimePerformanceMetrics() && coroutineScope.isActive) {
            try {

                delay(10.minutes.inWholeMilliseconds)

                getAnalyticsService()?.let { analyticsService ->
                    val result: PerformanceMetricsResponse = Retries.retryWithResult({
                        analyticsService.performanceMetrics
                    }, AnalyticsServiceException::class.java, 30000, 20)

                    if (result.metrics.isNotEmpty()) {
                        filterMetrics(result)
                        getActivityMonitor()?.let { activityMonitor ->
                            Log.log(logger::info, "registering first time performance metrics")
                            activityMonitor.registerPerformanceMetrics(result, true)
                            if (!PersistenceService.getInstance().isFirstTimePerformanceMetrics()) {
                                PersistenceService.getInstance().setFirstTimePerformanceMetrics()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.warnWithException(logger, e, "failed in first time registerPerformanceMetrics")
                ErrorReporter.getInstance()
                    .reportError(findActiveProject(), "PerformanceMetricsPosthogEventStartupActivity.firstTimePerformanceMetrics", e)
            }
        }
    }


    private fun launchContinuousReport(nextReport: Duration) {

        @Suppress("UnstableApiUsage")
        disposingScope().launch {

            delay(nextReport.inWholeMilliseconds)

            while (isActive) {
                try {
                    getAnalyticsService()?.let { analyticsService ->
                        val result: PerformanceMetricsResponse = Retries.retryWithResult({
                            analyticsService.performanceMetrics
                        }, AnalyticsServiceException::class.java, 30000, 20)

                        if (result.metrics.isNotEmpty()) {
                            filterMetrics(result)
                            Log.log(logger::info, "registering continuous performance metrics")
                            getActivityMonitor()?.registerPerformanceMetrics(result, false)
                        }
                    }

                    delay(6.hours.inWholeMilliseconds)

                } catch (e: Exception) {
                    Log.warnWithException(logger, e, "failed in continuous registerPerformanceMetrics")
                    ErrorReporter.getInstance()
                        .reportError("PerformanceMetricsPosthogEventStartupActivity.continuousPerformanceMetrics", e)
                    delay(1.hours.inWholeMilliseconds)
                }
            }
        }
    }


    private fun filterMetrics(result: PerformanceMetricsResponse) {
        if (result.metrics.isNotEmpty()) {
            for (metric in result.metrics) {
                if (metric.metric == "TraceRateTimeSeries" || metric.metric == "SpanRateTimeSeries") {
                    if (metric.value is List<*>) {
                        metric.value = (metric.value as List<*>).filter { element: Any? ->
                            element is Map<*, *> && element["value"] != 0.0
                        }
                    }
                }
            }
        }
    }


    private fun getActivityMonitor(): ActivityMonitor? {
        return findActiveProject()?.service<ActivityMonitor>()
    }

    private fun getAnalyticsService(): AnalyticsService? {
        return findActiveProject()?.service<AnalyticsService>()
    }

}