package org.digma.intellij.plugin.digmathon

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class DigmathonStartup : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        DigmathonService.getInstance()
    }
}