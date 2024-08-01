package org.digma.intellij.plugin.auth

import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.posthog.ActivityMonitor

fun reportAuthPosthogEvent(evenName: String, caller: String, details: Map<String, Any> = mapOf()) {
    findActiveProject()?.let { project ->
        val detailsToSend = details.toMutableMap()
        detailsToSend["caller"] = caller
        detailsToSend["auth"] = "true" //a property for filtering auth events ,filter on auth isSet
        ActivityMonitor.getInstance(project).registerAuthEvent(evenName, detailsToSend)
    }
}
