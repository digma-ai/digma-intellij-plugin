package org.digma.intellij.plugin.posthog

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ActivityMonitorStarter : StartupActivity.Background {

    //try to start ActivityMonitor as early as possible. although if the digma tool window is opened on start it will call
    // ActivityMonitor on UI thread
    override fun runActivity(project: Project) {
        ActivityMonitor.getInstance(project)
    }

}