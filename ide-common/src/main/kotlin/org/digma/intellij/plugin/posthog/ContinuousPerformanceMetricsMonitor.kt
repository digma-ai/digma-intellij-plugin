package org.digma.intellij.plugin.posthog

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class ContinuousPerformanceMetricsMonitor: Disposable {

    companion object{
        val logger = Logger.getInstance(this::class.java)
    }


    private val timerStarted: AtomicBoolean = AtomicBoolean(false)

    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    init {
        Log.log(logger::info,"Starting notification center")
        startTimer()
    }

    private fun startTimer() {

        if (timerStarted.get()){
            return
        }

        timerStarted.set(true)
        @Suppress("UnstableApiUsage")
        DisposingScope(this).launch {
            while (true) {
                delay(12*60*60*1000) //every 12 Hr  12*60*60*1000
                findActiveProject()?.let { project ->
                    val result = AnalyticsService.getInstance(project).performanceMetrics
                    if (result.metrics.isNotEmpty()) {
                        for (metric in result.metrics) {
                            if(metric.metric == "TraceRateTimeSeries" || metric.metric == "SpanRateTimeSeries"){
                                metric.value = (metric.value as ArrayList<LinkedHashMap<String,Double>>).filter { it["value"] != 0.0 }.toList()
                            }
                        }
                        ActivityMonitor.getInstance(project).registerContinuousPerformanceMetrics(result)
                    }
                }
            }
        }
    }
}


class ContinuousPerformanceMetricsMonitorStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        service<ContinuousPerformanceMetricsMonitor>()
    }
}