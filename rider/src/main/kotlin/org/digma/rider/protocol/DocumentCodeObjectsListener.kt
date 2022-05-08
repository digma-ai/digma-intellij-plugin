package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.psi.PsiFile
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged

class DocumentCodeObjectsListener : ProjectManagerListener {

    lateinit var project: Project

    override fun projectOpened(project: Project) {
        this.project = project
        //todo: check what can be used on solution
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            val model = project.solution.codeObjectsModel
            model.documentAnalyzed.advise(project.lifetime) { filePath ->
                documentAnalyzed(filePath, project)
            }
        }
    }

    private fun documentAnalyzed(filePath: String, project: Project) {
        val psiFile = pathToPsiFile(filePath, project)
        notifyDocumentCodeObjectsChanged(psiFile)
    }


    private fun notifyDocumentCodeObjectsChanged(psiFile: PsiFile?) {
        val publisher: DocumentCodeObjectsChanged =
            project.messageBus.syncPublisher(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC)
        publisher.documentCodeObjectsChanged(psiFile)
    }

}