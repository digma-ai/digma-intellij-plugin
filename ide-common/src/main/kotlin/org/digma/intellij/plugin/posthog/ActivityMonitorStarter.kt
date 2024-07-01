package org.digma.intellij.plugin.posthog

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class ActivityMonitorStarter : DigmaProjectActivity() {

    //try to start ActivityMonitor as early as possible. although if the digma tool window is opened on start it will call
    // ActivityMonitor on UI thread
    override fun executeProjectStartup(project: Project) {
        ActivityMonitor.getInstance(project)
    }

}