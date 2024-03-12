package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

internal class JvmNavigationDiscoveryStartupActivity : StartupActivity {

    private val logger = Logger.getInstance(this::class.java)

    override fun runActivity(project: Project) {

        Backgroundable.executeOnPooledThread {

            try {
                Log.log(logger::info, "starting navigation mapping")
                val jvmSpanNavigationProvider = JvmSpanNavigationProvider.getInstance(project)
                jvmSpanNavigationProvider.buildNavigationDiscovery()

                val javaEndpointNavigationProvider = JvmEndpointNavigationProvider.getInstance(project)
                javaEndpointNavigationProvider.buildNavigationDiscovery()
                Log.log(logger::info, "navigation mapping completed successfully")
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "error in navigation mapping {}", e)
                ErrorReporter.getInstance().reportError(project, "JvmNavigationDiscoveryStartupActivity.runActivity", e)
            }
        }
    }
}
