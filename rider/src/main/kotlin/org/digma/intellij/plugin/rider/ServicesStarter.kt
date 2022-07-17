package org.digma.intellij.plugin.rider

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.document.DocumentAnalyzer
import org.digma.intellij.plugin.rider.editor.RiderDocumentInfoConsumer
import org.digma.intellij.plugin.rider.env.RiderEnvironmentChangedListener
import org.digma.intellij.plugin.rider.protocol.DocumentCodeObjectsListener
import org.digma.intellij.plugin.rider.protocol.ShowToolWindowHost
import java.util.*

//todo : delete
class ServicesStarter : StartupActivity {

    //with rider some services need to start when the project starts.
    //StartupActivity is a good option. but if the tool window is re-opened on startup
    //then createToolWindowContent will be executed before StartupActivity and RiderEditorEventsHandler.start
    //will execute before all these services are started.
    //if the tool window is not re-opened then StartupActivity will execute and start the necessary services for rider.
    //calling loadStartupServices from runActivity and from RiderEditorEventsHandler.start makes
    //sure that in each situation we have all these services running.
    //these rider services are usually listeners to protocol events or project message bus.
    //calling project.getService multiple times with the same class is ok, it's always the same service


    override fun runActivity(project: Project) {
        loadStartupServices(project)
    }


    companion object {
        @JvmStatic
        fun loadStartupServices(project: Project) {
            Objects.requireNonNull(project.getService(ShowToolWindowHost::class.java))
            Objects.requireNonNull(project.getService(DocumentAnalyzer::class.java))
            Objects.requireNonNull(project.getService(RiderDocumentInfoConsumer::class.java))
            Objects.requireNonNull(project.getService(DocumentCodeObjectsListener::class.java))
            Objects.requireNonNull(project.getService(RiderEnvironmentChangedListener::class.java))
        }

    }

}