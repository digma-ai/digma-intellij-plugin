package org.digma.intellij.plugin.digmathon

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class DigmathonStartup : StartupActivity.DumbAware {

    override fun runActivity(project: Project) {
        DigmathonService.getInstance()
    }
}