package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.project.Project
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.idea.navigation.model.Origin
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.SessionMetadata
import org.digma.intellij.plugin.posthog.getPluginLoadedKey
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

abstract class AbstractNavigationDiscovery(protected val project: Project) {

    protected fun registerStartEvent(eventName: String, origin: Origin, retry: Int) {

        //no need to send the event on file changed because it is too many
        if (origin == Origin.FileChanged) {
            return
        }


        val pluginLoadedTime = SessionMetadata.getInstance().getCreated(getPluginLoadedKey(project))
        val duration = pluginLoadedTime?.let {
            Clock.System.now().minus(it)
        } ?: Duration.ZERO

        val startupLagMillis = duration.inWholeMilliseconds

        val startupLagToShow =
            if (startupLagMillis > 2000) "${TimeUnit.MILLISECONDS.toSeconds(startupLagMillis)} seconds" else "$startupLagMillis millis"

        val details = if (origin == Origin.Startup) {
            mapOf(
                "startupLag" to startupLagToShow,
                "origin" to origin,
                "retry" to retry
            )
        } else {
            mapOf(
                "origin" to origin,
                "retry" to retry
            )
        }

        ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
            eventName,
            details
        )
    }

    protected fun registerFinishedEvent(
        eventName: String,
        origin: Origin,
        success: Boolean,
        exhaustedRetries: Boolean,
        retry: Int,
        timeTookMillis: Long,
        hadErrors: Boolean,
        hadPCE: Boolean,
    ) {

        //no need to send the event on file changed because it is too many
        if (origin == Origin.FileChanged) {
            return
        }

        val timeToShow = if (timeTookMillis > 2000) "${TimeUnit.MILLISECONDS.toSeconds(timeTookMillis)} seconds" else "$timeTookMillis millis"

        ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
            eventName,
            mapOf(
                "timeTook" to timeToShow,
                "origin" to origin,
                "success" to success,
                "exhaustedRetries" to exhaustedRetries,
                "retry" to retry,
                "hadErrors" to hadErrors,
                "hadPCE" to hadPCE
            )
        )
    }


}