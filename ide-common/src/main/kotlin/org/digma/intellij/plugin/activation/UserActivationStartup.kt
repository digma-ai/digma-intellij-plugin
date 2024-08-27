package org.digma.intellij.plugin.activation

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class UserActivationStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        //just start the service
        UserActivationService.getInstance()
    }
}