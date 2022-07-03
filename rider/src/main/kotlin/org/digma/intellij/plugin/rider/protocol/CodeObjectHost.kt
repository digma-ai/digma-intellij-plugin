package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.util.reactive.IMutableViewableMap
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

class CodeObjectHost(val project: Project) {

    private val LOGGER = Logger.getInstance(CodeObjectHost::class.java)

    private var model: CodeObjectsModel = project.solution.codeObjectsModel


    //using the file uri as the document key in rider protocol proved to be unstable because of
    // differences in conversion between linux and windows.
    // so the document key is just its full path without the URI schema 'file:///'
    // the uri is a field in Document and is used to find a psi file in rider fronend


    fun getDocument(psiFile: PsiFile): DocumentInfo? {
        return ReadAction.compute<DocumentInfo,Exception> {
            val path: String = PsiUtils.psiFileToDocumentProtocolKey(psiFile)
            Log.log(LOGGER::debug, "Got request for getDocument for {}",path)
            val document: Document? = this.model.documents[path]
            Log.log(LOGGER::debug, "Got document for {}: {}",path,document?.printToString())
            return@compute toModel(document)
        }
    }



    fun installCodeLens(@NotNull psiFile: PsiFile, @NotNull codeLenses: MutableList<CodeLens>) {

        if (codeLenses.isEmpty()) return

        Log.log(LOGGER::debug, "Installing code lens for {}: {}",psiFile.virtualFile,codeLenses)

        model.protocol.scheduler.invokeOrQueue {
            codeLenses.forEach(Consumer { codeLens ->
                model.codeLens[codeLens.codeObjectId] = codeLens.toRiderCodeLensInfo()
            })

            val documentKey = PsiUtils.psiFileToDocumentProtocolKey(psiFile)
            Log.log(LOGGER::debug, "Calling reanalyze for {}",documentKey)
            model.reanalyze.fire(documentKey)

        }
    }


    /**
     * called when environment changed.
     * clears related objects and fired DocumentCodeObjectsChanged for documents that are still in the protocol
     */
    fun environmentChanged() {
        Log.log(LOGGER::debug, "Got environmentChanged event , refreshing all documents")

         model.protocol.scheduler.invokeOrQueue {

            model.codeLens.clear()
            model.reanalyzeAll.fire(Unit)

            model.documents.values.forEach {
                val docUri = it.fileUri
                val psiFile = PsiUtils.uriToPsiFile(docUri, project)
                notifyDocumentCodeObjectsChanged(psiFile)
            }
        }
    }


    private fun notifyDocumentCodeObjectsChanged(psiFile: PsiFile?) {
        val publisher: DocumentCodeObjectsChanged =
            project.messageBus.syncPublisher(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC)
        publisher.documentCodeObjectsChanged(psiFile)
    }



    fun findWorkspaceUrisForCodeObjectIds(codeObjectIds: MutableList<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val workspaceUriPairs = model.getWorkspaceUris.callSynchronously(codeObjectIds, model.protocol)
        workspaceUriPairs?.forEach {
            result.put(it.codeObjectId, it.workspaceUri)
        }
        return result
    }

    fun findWorkspaceUrisForSpanIds(spanIds: MutableList<String>): Map<String, Pair<String,Int>> {
        val result = HashMap<String,Pair<String,Int>>()
        val workspaceUris = model.getSpansWorkspaceUris.callSynchronously(spanIds,model.protocol)
        workspaceUris?.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri,it.offset)
        }
        return result
    }


    private fun toModel(document: Document?): DocumentInfo? {
        return document?.toDocumentInfo()
    }


    private fun Document.toDocumentInfo() = DocumentInfo(
        path = fileUri,
        methods = toMethodInfoMap(methods)

    )

    private fun toMethodInfoMap(methods: IMutableViewableMap<String, RiderMethodInfo>): MutableMap<String, MethodInfo> {
        val modelMethods: HashMap<String, MethodInfo> = HashMap()
        methods.forEach { (k, v) ->
            modelMethods[k] = v.toMethodInfo()
        }
        return modelMethods
    }


    private fun RiderMethodInfo.toMethodInfo() = MethodInfo(
        id = id,
        name = name,
        containingClass = containingClass,
        containingNamespace = containingNamespace,
        containingFileUri = containingFileUri,
        spans = toSpansList(spans)
    )

    private fun toSpansList(spans: List<RiderSpanInfo>): List<SpanInfo> {
        val modelSpans = ArrayList<SpanInfo>()
        spans.forEach {
            modelSpans.add(it.toSpanInfo())
        }
        return modelSpans
    }


    private fun RiderSpanInfo.toSpanInfo() = SpanInfo(
        id = id,
        name = name,
        containingMethod = containingMethod,
        containingFileUri = containingFileUri
    )


    private fun CodeLens.toRiderCodeLensInfo() = RiderCodeLensInfo(
        codeObjectId = codeObjectId,
        lensText = lensText,
        lensTooltip = lensTooltipText,
        moreText = lensMoreText,
        anchor = anchor
    )



}