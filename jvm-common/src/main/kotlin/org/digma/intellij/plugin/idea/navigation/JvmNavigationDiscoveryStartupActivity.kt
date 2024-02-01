package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.datetime.Clock
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter.Companion.getInstance
import org.digma.intellij.plugin.idea.psi.navigation.JavaEndpointNavigationProvider
import org.digma.intellij.plugin.idea.psi.navigation.JavaSpanNavigationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.SessionMetadata
import org.digma.intellij.plugin.posthog.getPluginLoadedKey
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class JvmNavigationDiscoveryStartupActivity : StartupActivity {

    private val logger = Logger.getInstance(this::class.java)

    override fun runActivity(project: Project) {

        Backgroundable.executeOnPooledThread {

            val pluginLoadedTime = SessionMetadata.getInstance().getCreated(getPluginLoadedKey(project))
            val duration = pluginLoadedTime?.let {
                Clock.System.now().minus(it)
            } ?: Duration.ZERO

            val startupLagSeconds = duration.inWholeSeconds
            ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
                "started",
                mapOf("startupLagSeconds" to startupLagSeconds)
            )

            val stopWatch = StopWatch.createStarted()
            try {
                Log.log(logger::info, "starting navigation mapping")
                val javaSpanNavigationProvider = JavaSpanNavigationProvider.getInstance(project)
                javaSpanNavigationProvider.buildSpanNavigation()

                val javaEndpointNavigationProvider = JavaEndpointNavigationProvider.getInstance(project)
                javaEndpointNavigationProvider.buildEndpointNavigation()
                Log.log(logger::info, "navigation mapping completed successfully")
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in navigation mapping {}", e)
                getInstance().reportError(project, "JvmNavigationDiscoveryStartupActivity.runActivity", e)

            } finally {
                val time = stopWatch.getTime(TimeUnit.SECONDS)
                ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
                    "finished",
                    mapOf(
                        "timeTookSeconds" to time,
                        "startupLagSeconds" to startupLagSeconds
                    )
                )
            }
        }
    }
}
