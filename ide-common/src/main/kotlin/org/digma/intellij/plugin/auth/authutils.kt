package org.digma.intellij.plugin.auth

import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.posthog.ActivityMonitor


fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
    findActiveProject()?.let { project ->
        ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
    }
}