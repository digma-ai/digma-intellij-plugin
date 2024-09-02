package org.digma.intellij.plugin.posthog

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.activation.UserActivationService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.startup.DigmaProjectActivity
import org.digma.intellij.plugin.ui.ToolWindowShower

class FirstInitStartupActivity : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        //can't rely on intellij RunOnceUtil.runOnceForApp because it will run it again on ide upgrades
        if (PersistenceService.getInstance().isFirstTimePluginLoaded()) {

            //apply the new activation login only to users that installed the plugin first time that includes the new activation logic.
            //users that already has plugin first loaded will not be applied the new login.
            UserActivationService.getInstance().applyNewActivationLogic()
            PersistenceService.getInstance().setFirstTimePluginLoadedDone()
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoaded()
            project.getService(DumbService::class.java).runWhenSmart {
                EDT.ensureEDT { ToolWindowShower.getInstance(project).showToolWindow() }
            }
        }
    }
}
