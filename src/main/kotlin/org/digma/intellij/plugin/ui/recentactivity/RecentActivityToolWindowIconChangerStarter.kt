package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class RecentActivityToolWindowIconChangerStarter: DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        RecentActivityToolWindowIconChanger.getInstance(project)
    }
}