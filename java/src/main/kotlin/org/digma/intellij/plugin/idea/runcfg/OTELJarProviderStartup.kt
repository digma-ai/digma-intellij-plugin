package org.digma.intellij.plugin.idea.runcfg

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class OTELJarProviderStartup: StartupActivity {
    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().getService(OTELJarProvider::class.java).ensureDownloaded(project,true)
    }
}