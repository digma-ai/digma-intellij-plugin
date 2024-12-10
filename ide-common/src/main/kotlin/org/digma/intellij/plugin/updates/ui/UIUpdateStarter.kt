package org.digma.intellij.plugin.updates.ui

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class UIUpdateStarter : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        UIVersioningService.getInstance()
    }
}