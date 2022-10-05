package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiFileNotFountException
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.rider.psi.csharp.CSharpDocumentAnalyzer

/**
 * Listens for documentAnalyzed events from resharper, actually when a document is opened.
 */
class DocumentCodeObjectsListener(project: Project) : LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(DocumentCodeObjectsListener::class.java)

    private val cSharpDocumentAnalyzer = project.getService(CSharpDocumentAnalyzer::class.java)


    init {
        Log.log(logger::info, "DocumentCodeObjectsListener registering for solution startup..")

        project.solution.isLoaded.advise(project.lifetime) {

            Log.log(logger::info, "Starting DocumentCodeObjectsListener")

            val model = project.solution.codeObjectsModel

            //process documents that are currently in the protocol, they may be added by the
            //backend before the solution was fully started. it may happen if there are
            //re-opened documents at startup.
            Log.log(logger::info, "Processing existing documents in the protocol")
            model.documents.forEach {
                Log.log(logger::info, "Notifying documentAnalyzed for {}", it.key)
                documentAnalyzed(model, it.key, project)
            }


            //the documents that are currently in the protocol may be incomplete, could not resolve
            //references. call refreshIncompleteDocuments so the backend will refresh all incomplete
            //documents, hopefully at this stage reference resolving should work.
            //if a document was refreshed it will be notified again to the frontend.
            model.refreshIncompleteDocuments.fire(Unit)

            Log.log(logger::info,project, "Starting to listen for documentAnalyzed events")
            //in rider documentAnalyzed is actually document opened + already analysed
            model.documentAnalyzed.advise(project.lifetime) { documentKey ->
                Log.log(logger::debug,project, "Got documentAnalyzed event for {}", documentKey)
                documentAnalyzed(model, documentKey, project)
            }
        }
    }

    private fun documentAnalyzed(model: CodeObjectsModel,
                                 documentKey: String,
                                 project: Project) {
        val docUri = model.documents[documentKey]?.fileUri
        if (docUri != null) {
            Log.log(logger::debug, "Found document in the protocol for {}", documentKey)
            documentAnalyzed(docUri, project)
        } else {
            Log.log(logger::debug, "Could not find document in the protocol for {}", documentKey)
        }
    }


    private fun documentAnalyzed(docUri: String, project: Project) {
        try {
            val psiFile = PsiUtils.uriToPsiFile(docUri, project)
            Log.log(logger::debug, "Notifying DocumentCodeObjectsChanged for {}", psiFile.virtualFile)
            cSharpDocumentAnalyzer.analyzeDocument(psiFile)
        } catch (e: PsiFileNotFountException) {
            Log.error(logger, project, e, "Could not find psiFile for document uri {}", docUri)
        }
    }

}