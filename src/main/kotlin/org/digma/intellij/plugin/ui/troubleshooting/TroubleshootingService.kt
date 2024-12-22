package org.digma.intellij.plugin.ui.troubleshooting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.DisposableAdaptor

@Service(Service.Level.PROJECT)
class TroubleshootingService : DisposableAdaptor {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TroubleshootingService {
            return project.service<TroubleshootingService>()
        }
    }
}