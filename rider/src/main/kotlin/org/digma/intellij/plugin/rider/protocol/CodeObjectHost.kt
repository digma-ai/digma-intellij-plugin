package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.util.reactive.IMutableViewableMap
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.rider.psi.RiderPsiUtils
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

class CodeObjectHost(project: Project): LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(CodeObjectHost::class.java)

    private var model: CodeObjectsModel = project.solution.codeObjectsModel


    //Note: using the file uri as the document key in rider protocol proved to be unstable because of
    // differences in conversion between linux and windows.
    //when running on windows the file:// schema has different number of slashes between resharper and the jvm,
    // and so it's not possible to use it as a map key in both sides.
    // so the document key is just its full path without the URI schema 'file:///', in resharper we use just the path
    // as the map key and in java we take the path from the PsiFile.
    // the Document has a fileUri field which is an uri with schema and is used to find a psi file in rider frontend.
    // PsuUtil.uriToPsiFile work ok with both the uri from resharper and the uri in the jvm side.


    fun getDocument(psiFile: PsiFile): DocumentInfo? {
        val path: String = RiderPsiUtils.psiFileToDocumentProtocolKey(psiFile)
        Log.log(logger::debug,"Got request for getDocument for PsiFile {}, converted to path: {}",psiFile.virtualFile,path)
        val document: Document? = this.model.documents[path]
        if (logger.isDebugEnabled) {
            Log.log(logger::debug, "Got document for {}: {}", path, document?.printToString())
        }
        return toModel(document)
    }


    fun removeDocument(psiFile: PsiFile) {
        model.protocol.scheduler.invokeOrQueue {
            Log.log(logger::debug, "Removing document for PsiFile {}", psiFile.virtualFile)
            val path: String = RiderPsiUtils.psiFileToDocumentProtocolKey(psiFile)
            this.model.documents.remove(path)
        }
    }


    fun installCodeLens(@NotNull psiFile: PsiFile, @NotNull codeLenses: MutableList<CodeLens>) {

        //install code lens for a document. this code will also take care of clearing old
        //code lens of this document, necessary in environment change event.

        Log.log(logger::debug, "Installing code lens for {}: {}", psiFile.virtualFile, codeLenses)

        model.protocol.scheduler.invokeOrQueue {
            val documentKey = RiderPsiUtils.psiFileToDocumentProtocolKey(psiFile)

            //first remove all code lens entries belonging to this document.
            //the map is not keyed by document, so we have to search
            val toRemove = mutableSetOf<String>()
            model.codeLens.forEach { entry ->
                entry.value.lens.forEach { codeLensInfo ->
                    if (codeLensInfo.documentProtocolKey.equals(documentKey)) {
                        toRemove.add(entry.key)
                    }
                }
            }
            toRemove.forEach {
                model.codeLens.remove(it)
            }


            //add code lens to the rider protocol
            codeLenses.forEach(Consumer { codeLens ->
                model.codeLens.computeIfAbsent(codeLens.codeObjectId) { LensPerObjectId() }
                model.codeLens[codeLens.codeObjectId]?.lens?.add(codeLens.toRiderCodeLensInfo(documentKey))
            })

            Log.log(logger::debug, "Calling reanalyze for {}", documentKey)
            model.reanalyze.fire(documentKey)
        }
    }


    fun findWorkspaceUrisForCodeObjectIds(codeObjectIds: MutableList<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val workspaceUriPairs = model.getWorkspaceUris.callSynchronously(codeObjectIds, model.protocol)
        workspaceUriPairs?.forEach {
            result[it.codeObjectId] = it.workspaceUri
        }
        return result
    }

    fun findWorkspaceUrisForSpanIds(spanIds: MutableList<String>): Map<String, Pair<String, Int>> {
        val result = HashMap<String, Pair<String, Int>>()
        val workspaceUris = model.getSpansWorkspaceUris.callSynchronously(spanIds, model.protocol)
        workspaceUris?.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri, it.offset)
        }
        return result
    }


    fun isCSharpMethod(methodCodeObjectId: String): Boolean {
        val result = model.isCsharpMethod.callSynchronously(methodCodeObjectId, model.protocol)
        result?.let {
            return result
        }
        return false
    }


    private fun toModel(document: Document?): DocumentInfo? {
        return document?.toDocumentInfo()
    }


    private fun Document.toDocumentInfo() = DocumentInfo(
        fileUri = normalizeFileUri(fileUri,project),
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
        containingFileUri = normalizeFileUri(containingFileUri,project),
        offsetAtFileUri = offsetAtFileUri,
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
        containingFileUri = normalizeFileUri(containingFileUri,project)
    )


    private fun CodeLens.toRiderCodeLensInfo(docKey: String) = RiderCodeLensInfo(
        codeObjectId = codeObjectId,
        type = toRiderCodeLensType(type),
        lensText = lensText,
        lensTooltip = lensTooltipText,
        moreText = lensMoreText,
        anchor = anchor,
        documentProtocolKey = docKey
    )

    private fun toRiderCodeLensType(type: CodeLens.CodeLensType): CodeLensType {
        return when (type) {
            CodeLens.CodeLensType.ErrorHotspot -> CodeLensType.ErrorHotspot
            CodeLens.CodeLensType.LowUsage -> CodeLensType.LowUsage
            CodeLens.CodeLensType.HighUsage -> CodeLensType.HighUsage
        }
    }


}