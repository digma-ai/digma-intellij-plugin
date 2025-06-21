package org.digma.intellij.plugin.python.navigation

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.discovery.AbstractNavigationDiscoveryManager
import org.digma.intellij.plugin.startup.DigmaProjectActivity

internal class PythonNavigationDiscoveryStartupActivity : DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        if (AbstractNavigationDiscoveryManager.isDiscoveryEnabled()) {
            PythonNavigationDiscoveryManager.getInstance(project).startup()
        }
    }
}
