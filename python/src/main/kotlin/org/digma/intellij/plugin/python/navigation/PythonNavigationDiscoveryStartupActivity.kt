package org.digma.intellij.plugin.python.navigation

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

internal class PythonNavigationDiscoveryStartupActivity : DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        PythonNavigationDiscoveryManager.getInstance(project).start()
    }
}
