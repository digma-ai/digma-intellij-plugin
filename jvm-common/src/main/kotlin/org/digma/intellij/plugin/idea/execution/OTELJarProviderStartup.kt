package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class OTELJarProviderStartup : StartupActivity {
    override fun runActivity(project: Project) {
        //just call the service so its initializes if it's the first IDE startup.
        //opening more projects will do nothing because it's an application service.
        service<OTELJarProvider>()
    }
}