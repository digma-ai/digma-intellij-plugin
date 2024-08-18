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


//if we want to debug issues in auth manager wrap some code that will run only if debug is on.
//set the property in runIde task
fun withAuthManagerDebug(block: () -> Unit) {
    if (System.getProperty("org.digma.plugin.auth.debug") != null) {
        block.invoke()
    }
}