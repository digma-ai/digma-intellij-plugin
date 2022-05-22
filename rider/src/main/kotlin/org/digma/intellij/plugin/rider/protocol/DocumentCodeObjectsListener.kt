package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.psi.PsiFile
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils

class DocumentCodeObjectsListener : ProjectManagerListener {

    private val LOGGER = Logger.getInstance(DocumentCodeObjectsListener::class.java)

    lateinit var project: Project

    override fun projectOpened(project: Project) {
        this.project = project
        //todo: check what can be used on solution
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            val model = project.solution.codeObjectsModel
            model.documentAnalyzed.advise(project.lifetime) { documentKey ->
                Log.log(LOGGER::info, "Got documentAnalyzed event for {}",documentKey)
                var docUri = model.documents[documentKey]?.fileUri
                if (docUri != null) {
                    Log.log(LOGGER::info, "Found document for {}",documentKey)
                    documentAnalyzed(docUri, project)
                }
            }
        }
    }

    private fun documentAnalyzed(docUri: String, project: Project) {
        val psiFile = PsiUtils.uriToPsiFile(docUri, project)
        notifyDocumentCodeObjectsChanged(psiFile)
    }


    private fun notifyDocumentCodeObjectsChanged(psiFile: PsiFile?) {
        val publisher: DocumentCodeObjectsChanged =
            project.messageBus.syncPublisher(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC)
        publisher.documentCodeObjectsChanged(psiFile)
    }

}