package org.digma.intellij.plugin.posthog

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.serviceContainer.AlreadyDisposedException
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_LOW
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.scheduling.disposingOneShotDelayedTask
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.startup.DigmaProjectActivity
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class PerformanceMetricsPosthogEventStartupActivity : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        service<ContinuousPerformanceMetricsReporter>().start()
    }
}


@Service(Service.Level.APP)
class ContinuousPerformanceMetricsReporter : DisposableAdaptor {

    private val logger = Logger.getInstance(this::class.java)

    private val started = AtomicBoolean(false)

    fun start() {

        if (started.getAndSet(true)) {
            Log.log(logger::info, "ContinuousPerformanceMetricsReporter.start called for project {} but already started")
            return
        }

        Log.log(logger::info, "ContinuousPerformanceMetricsReporter starting")

        if (!PersistenceService.getInstance().isFirstTimePerformanceMetrics()) {

            val disposable = Disposer.newDisposable()
            disposable.disposingPeriodicTask(
                "ContinuousPerformanceMetricsReporter.waitForFirstTime",
                2.minutes.inWholeMilliseconds,
                10.minutes.inWholeMilliseconds
            ) {
                if (!PersistenceService.getInstance().isFirstTimePerformanceMetrics()) {
                    checkForFirstTime()
                } else {
                    //will dispose this task
                    Disposer.dispose(disposable)
                    launchContinuousReport(6.hours)
                }
            }
        } else {
            //this will happen on startup, make sure we have a report in 10 minutes and then every 6 hours
            launchContinuousReport(10.minutes)
        }
    }


    private fun checkForFirstTime() {

        if (!PersistenceService.getInstance().isFirstTimePerformanceMetrics()) {
            try {

                findActiveProject()?.takeIf { isProjectValid(it) }?.let { project ->

                    val result: PerformanceMetricsResponse = AnalyticsService.getInstance(project).performanceMetrics

                    if (result.metrics.isNotEmpty()) {
                        filterMetrics(result)
                        getActivityMonitor(project).let { activityMonitor ->
                            Log.log(logger::info, "registering first time performance metrics")
                            activityMonitor.registerPerformanceMetrics(result, true)
                            if (!PersistenceService.getInstance().isFirstTimePerformanceMetrics()) {
                                PersistenceService.getInstance().setFirstTimePerformanceMetrics()
                            }
                        }
                    }
                }

            } catch (e: AlreadyDisposedException) {
                //ignore this exception.
                // it may happen when closing projects because this class uses any active project when it needs one on every iteration,
                // and sometimes the project will close before the current iteration ends.
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "failed in first time registerPerformanceMetrics {}", e)
                ErrorReporter.getInstance()
                    .reportError(
                        "ContinuousPerformanceMetricsReporter.firstTimePerformanceMetrics",
                        e,
                        mapOf(SEVERITY_PROP_NAME to SEVERITY_LOW)
                    )
            }
        }
    }


    private fun launchContinuousReport(nextReport: Duration) {

        val name = "ContinuousPerformanceMetricsReporter.continuousPerformanceMetrics"
        disposingPeriodicTask(name, nextReport.inWholeMilliseconds, 6.hours.inWholeMilliseconds) {
            try {
                report()
            } catch (e: AlreadyDisposedException) {
                //ignore this exception.
                // it may happen when closing projects because this class uses any active project when it needs one on every iteration,
                // and sometimes the project will close before the current iteration ends.
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "failed in continuous registerPerformanceMetrics")
                ErrorReporter.getInstance()
                    .reportError(
                        "ContinuousPerformanceMetricsReporter.continuousPerformanceMetrics",
                        e,
                        mapOf(SEVERITY_PROP_NAME to SEVERITY_LOW)
                    )
                //if failed, run it again in 1 hour instead of waiting the 6 hours delay
                disposingOneShotDelayedTask(name, 1.hours.inWholeMilliseconds) {
                    //not mandatory to catch exceptions here, exceptions will be caught and reported by the scheduler
                    report()
                }
            }
        }
    }


    private fun report() {
        findActiveProject()?.takeIf { isProjectValid(it) }?.let { project ->

            val result = AnalyticsService.getInstance(project).performanceMetrics

            if (result.metrics.isNotEmpty()) {
                filterMetrics(result)
                Log.log(logger::info, "registering continuous performance metrics")
                getActivityMonitor(project).registerPerformanceMetrics(result, false)
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


    private fun getActivityMonitor(project: Project): ActivityMonitor {
        return ActivityMonitor.getInstance(project)
    }

}