package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.discovery.AbstractNavigationDiscoveryManager
import org.digma.intellij.plugin.startup.DigmaProjectActivity

internal class JvmNavigationDiscoveryStartupActivity : DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        if (AbstractNavigationDiscoveryManager.isDiscoveryEnabled()) {
            JvmNavigationDiscoveryManager.getInstance(project).startup()
        }
    }
}
