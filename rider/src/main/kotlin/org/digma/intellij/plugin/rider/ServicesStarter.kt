package org.digma.intellij.plugin.rider

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.rider.protocol.DocumentCodeObjectsListener
import org.digma.intellij.plugin.rider.protocol.ShowToolWindowHost
import java.util.*
import javax.swing.SwingUtilities

class ServicesStarter : StartupActivity, DumbAware {

    //with rider some services need to start when the project starts because they need to start listening
    // to events from resharper.

    override fun runActivity(project: Project) {
        if (SwingUtilities.isEventDispatchThread()) {
            loadStartupServices(project)
        } else {
            SwingUtilities.invokeAndWait {
                loadStartupServices(project)
            }
        }
    }


    companion object {
        @JvmStatic
        fun loadStartupServices(project: Project) {
            Objects.requireNonNull(project.getService(ShowToolWindowHost::class.java))
            Objects.requireNonNull(project.getService(DocumentCodeObjectsListener::class.java))
        }
    }

}