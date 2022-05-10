package org.digma.rider.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentAnalyzer
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.editor.RiderDocumentInfoConsumer
import org.digma.intellij.plugin.env.RiderEnvironmentChangedListener
import org.digma.intellij.plugin.psi.csharp.CSharpDocumentAnalyzer

class ServicesStarter : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            //some listeners need to be started manually because no one calls them.
            //for example RiderDocumentInfoConsumer needs to start listening on a topic.
            //it's also possible to register them as listeners, but then they can't get the Project in the constructor.
            //anyway they do the subscription manually in their constructor.
            project.getService(RiderDocumentInfoConsumer::class.java)
            project.getService(DocumentAnalyzer::class.java)
            project.getService(RiderEnvironmentChangedListener::class.java)
        }
    }
}