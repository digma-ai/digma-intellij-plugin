package org.digma.intellij.plugin.rider

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.rider.protocol.LanguageServiceHost
import org.digma.intellij.plugin.rider.protocol.ShowToolWindowHost
import java.util.*

class ServicesStarter : StartupActivity, DumbAware {

    //some rider services need to start when the project starts because they need to start listening
    // to events from resharper.
    // some services need a protocol object and may need to run on startup under EDT to initialize
    // early

    override fun runActivity(project: Project) {
        loadStartupServices(project)
    }


    companion object {
        @JvmStatic
        fun loadStartupServices(project: Project) {
            val logger = Logger.getInstance(ServicesStarter::class.java)

            SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(project.lifetime) {
                Log.log(logger::debug, "Initializing startup services")
                EDT.ensureEDT {
                    Objects.requireNonNull(project.getService(ShowToolWindowHost::class.java))

                    //initialize LanguageServiceHost early on startup, so it can initialize its model early on EDT.
                    // if called later it will require EDT which is not always what we want.
                    Objects.requireNonNull(project.getService(LanguageServiceHost::class.java))
                }
            }
        }
    }

}