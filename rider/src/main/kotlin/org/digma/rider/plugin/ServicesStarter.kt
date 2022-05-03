package org.digma.rider.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentInfoChanged

class ServicesStarter : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            //some plugins need to be started manually because no one calls them.
            //for example RiderDocumentInfoConsumer needs to start listening on a topic
            project.getService(DocumentInfoChanged::class.java)
        }
    }
}