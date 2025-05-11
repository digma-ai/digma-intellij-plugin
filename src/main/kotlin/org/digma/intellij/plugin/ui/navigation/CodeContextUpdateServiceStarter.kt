package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class CodeContextUpdateServiceStarter: DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        CodeContextUpdateService.getInstance(project)
    }
}