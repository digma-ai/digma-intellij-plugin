package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor

class ObservabilityUtil {

    companion object {

        @JvmStatic
        fun updateObservabilityValue(project: Project, selected: Boolean) {
            PersistenceService.getInstance().state.isAutoOtel = selected
            if (selected) {
                ActivityMonitor.getInstance(project).registerObservabilityOn()
            } else {
                ActivityMonitor.getInstance(project).registerObservabilityOff()
            }
        }
    }
}