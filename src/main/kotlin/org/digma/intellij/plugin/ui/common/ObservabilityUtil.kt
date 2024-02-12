package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.observability.ObservabilityChanged
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor


fun updateObservabilityValue(project: Project, isObservabilityEnabled: Boolean) {

    PersistenceService.getInstance().setObservabilityEnabled(isObservabilityEnabled)

    project.messageBus.syncPublisher(ObservabilityChanged.OBSERVABILITY_CHANGED_TOPIC)
        .observabilityChanged(isObservabilityEnabled)

    if (isObservabilityEnabled) {
        ActivityMonitor.getInstance(project).registerObservabilityOn()
    } else {
        ActivityMonitor.getInstance(project).registerObservabilityOff()
    }
}