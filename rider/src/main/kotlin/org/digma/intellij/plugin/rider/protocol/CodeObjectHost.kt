package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.util.reactive.IMutableViewableMap
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

class CodeObjectHost(val project: Project) {

    private var model: CodeObjectsModel = project.solution.codeObjectsModel


    //using the file uri as the document key in rider protocol proved to be unstable because of
    // differences in conversion between linux and windows.
    // so the document key is just its full path without the URI schema 'file:///'
    // the uri is a field in Document and is used to find a psi file in rider fronend


    fun getDocument(psiFile: PsiFile): DocumentInfo? {
        val path: String = PsiUtils.psiFileToDocumentProtocolKey(psiFile)
        val document: Document? = this.model.documents[path]
        return toModel(document)
    }

    private fun toModel(document: Document?): DocumentInfo? {
        return document?.toDocumentInfo()
    }



    fun installCodeLens(@NotNull psiFile: PsiFile, @NotNull codeLenses: MutableList<CodeLens>) {

        if (codeLenses.isEmpty()) return

        codeLenses.forEach(Consumer { codeLens ->
            model.codeLens[codeLens.codeObjectId] = codeLens.toRiderCodeLensInfo()
        })

        var documentKey = PsiUtils.psiFileToDocumentProtocolKey(psiFile)
        model.reanalyze.fire(documentKey)
    }


    /**
     * called when environment changed.
     * clears related objects and fired DocumentCodeObjectsChanged for documents that are still in the protocol
     */
    fun environmentChanged() {
        model.codeLens.clear()
        model.reanalyzeAll.fire(Unit)

        model.documents.values.forEach {
            val docUri = it.fileUri
            val psiFile = PsiUtils.uriToPsiFile(docUri, project)
            notifyDocumentCodeObjectsChanged(psiFile)
        }
    }


    private fun notifyDocumentCodeObjectsChanged(psiFile: PsiFile?) {
        val publisher: DocumentCodeObjectsChanged =
            project.messageBus.syncPublisher(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC)
        publisher.documentCodeObjectsChanged(psiFile)
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