package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.startup.DigmaProjectActivity

internal class JvmNavigationDiscoveryStartupActivity : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {

        Log.log(logger::info, project, "starting span navigation mapping")
        val jvmSpanNavigationProvider = JvmSpanNavigationProvider.getInstance(project)
        jvmSpanNavigationProvider.buildNavigationDiscovery()

        Log.log(logger::info, project, "starting endpoint navigation mapping")
        val javaEndpointNavigationProvider = JvmEndpointNavigationProvider.getInstance(project)
        javaEndpointNavigationProvider.buildNavigationDiscovery()

        //start the service
        project.service<NavigationDiscoveryChangeService>()
    }
}
