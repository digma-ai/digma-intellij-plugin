package org.digma.intellij.plugin.posthog

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.startup.DigmaProjectActivity
import org.digma.intellij.plugin.ui.ToolWindowShower

class FirstInitStartupActivity : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        //can't rely on intellij RunOnceUtil.runOnceForApp because it will run it again on ide upgrades
        if (PersistenceService.getInstance().isFirstTimePluginLoaded()) {
            PersistenceService.getInstance().setFirstTimePluginLoadedDone()
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoaded()
            project.getService(DumbService::class.java).runWhenSmart {
                EDT.ensureEDT { ToolWindowShower.getInstance(project).showToolWindow() }
            }
        }
    }
}
