package org.digma.intellij.plugin.posthog

import com.intellij.collaboration.async.DisposingScope
import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService

private const val MY_ID = "org.digma.runonce.FirstTimePerformanceMetricsMonitor"

class FirstTimePerformanceMetricsMonitor: StartupActivity.DumbAware {

    override fun runActivity(project: Project) {

        RunOnceUtil.runOnceForApp(MY_ID){

            val disposable = Disposer.newDisposable()
            Disposer.register(AnalyticsService.getInstance(project),disposable)

            @Suppress("UnstableApiUsage")
            DisposingScope(disposable).launch {

                var maxFailures = 20

                while (!PersistenceService.getInstance().state.firstTimePerformanceMetrics && maxFailures > 0){
                    try {

                        delay(30000)

                        val result = AnalyticsService.getInstance(project).performanceMetrics

                        if (result.metrics.isNotEmpty()) {
                            ActivityMonitor.getInstance(project).registerPerformanceMetrics(result)
                            PersistenceService.getInstance().state.firstTimePerformanceMetrics = true
                        }
                    }catch (e: AnalyticsServiceException){
                        Log.warnWithException(Logger.getInstance(FirstTimePerformanceMetricsMonitor::class.java),e,"failed in registerPerformanceMetrics")
                        maxFailures--
                    }
                }
            }
        }
    }
}