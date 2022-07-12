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
        Log.log(LOGGER::info, "DocumentCodeObjectsListener waiting for solution startup..")
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            Log.log(LOGGER::info, "Starting DocumentCodeObjectsListener for documentAnalyzed")

            val model = project.solution.codeObjectsModel

            //process documents that are currently in the protocol, they wher added by the
            //backend before the solution was fully started
            Log.log(LOGGER::info, "Processing existing documents in the protocol")
            model.documents.forEach{
                Log.log(LOGGER::info, "Notifying documentAnalyzed for {}",it.key)
                documentAnalyzed(model, it.key, project)
            }

            //the documents that are currently in the protocol may be incomplete, could not resolve
            //references. call refreshIncompleteDocuments so the backend will refresh all incomplete
            //documents, hopefully at this stage reference resolving should work.
            //if a documents was refreshed it will be notified again to the frontend
            model.refreshIncompleteDocuments.fire(Unit)

            Log.log(LOGGER::info, "Starting to listen for documentAnalyzed events")
            model.documentAnalyzed.advise(project.lifetime) { documentKey ->
                Log.log(LOGGER::debug, "Got documentAnalyzed event for {}",documentKey)
                documentAnalyzed(model, documentKey, project)
            }
        }
    }

    private fun documentAnalyzed(model: CodeObjectsModel,
                                 documentKey: String,
                                 project: Project) {
        val docUri = model.documents[documentKey]?.fileUri
        if (docUri != null) {
            Log.log(LOGGER::debug, "Found document in the protocol for {}", documentKey)
            documentAnalyzed(docUri, project)
        } else {
            Log.log(LOGGER::debug, "Could not find document in the protocol for {}", documentKey)
        }
    }

    private fun documentAnalyzed(docUri: String, project: Project) {
        val psiFile = PsiUtils.uriToPsiFile(docUri, project)
        //if psiFile is null we probably have a bug somewhere
        if(psiFile == null){
            Log.log(LOGGER::error, "Could not find psiFile for document uri {}",docUri)
            throw RuntimeException("Could not find psiFile for document $docUri")
        }
        Log.log(LOGGER::debug, "Notifying DocumentCodeObjectsChanged for {}",psiFile.virtualFile)
        notifyDocumentCodeObjectsChanged(psiFile)
    }


    private fun notifyDocumentCodeObjectsChanged(psiFile: PsiFile?) {
        if (project.isDisposed){
            Log.log(LOGGER::error, "notifyDocumentCodeObjectsChanged for file {} called after project is disposed {}",psiFile?.virtualFile,project)
            return
        }
        val publisher: DocumentCodeObjectsChanged =
            project.messageBus.syncPublisher(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC)
        publisher.documentCodeObjectsChanged(psiFile)
    }

}