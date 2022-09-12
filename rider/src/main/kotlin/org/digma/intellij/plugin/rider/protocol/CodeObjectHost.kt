package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
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
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

class CodeObjectHost(project: Project): LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(CodeObjectHost::class.java)

    private var model: CodeObjectsModel = project.solution.codeObjectsModel


    //using the file uri as the document key in rider protocol proved to be unstable because of
    // differences in conversion between linux and windows.
    // so the document key is just its full path without the URI schema 'file:///'
    // the uri is a field in Document and is used to find a psi file in rider fronend


    fun getDocument(psiFile: PsiFile): DocumentInfo? {
        return ReadAction.compute<DocumentInfo,Exception> {
            val path: String = PsiUtils.psiFileToDocumentProtocolKey(psiFile)
            Log.log(logger::debug, "Got request for getDocument for PsiFile {}, converted to path: {}",psiFile.virtualFile,path)
            val document: Document? = this.model.documents[path]
            Log.log(logger::debug, "Got document for {}: {}",path,document?.printToString())
            return@compute toModel(document)
        }
    }



    fun installCodeLens(@NotNull psiFile: PsiFile, @NotNull codeLenses: MutableList<CodeLens>) {

        if (codeLenses.isEmpty()) return

        Log.log(logger::debug, "Installing code lens for {}: {}",psiFile.virtualFile,codeLenses)

        model.protocol.scheduler.invokeOrQueue {
            WriteAction.run<Exception> {
                val documentKey = PsiUtils.psiFileToDocumentProtocolKey(psiFile)

                codeLenses.forEach(Consumer { codeLens ->
                    model.codeLens.computeIfAbsent(codeLens.codeObjectId){LensPerObjectId()}
                    model.codeLens[codeLens.codeObjectId]?.lens?.add(codeLens.toRiderCodeLensInfo(documentKey))
                })

                Log.log(logger::debug, "Calling reanalyze for {}",documentKey)
                model.reanalyze.fire(documentKey)
            }
        }
    }



    fun environmentChanged() {
        Log.log(logger::debug, "Got environmentChanged event")

         model.protocol.scheduler.invokeOrQueue {
             WriteAction.run<Exception> {

                 //collect document keys, clear code lens and call reanalyze for each document
                 val docKeys = HashSet<String>()

                 model.codeLens.forEach { entry ->
                     entry.value.lens.forEach {codeLensInfo ->
                         docKeys.add(codeLensInfo.documentProtocolKey)
                     }
                 }

                 model.codeLens.clear()

                 docKeys.forEach {
                     Log.log(logger::debug, "Got environmentChanged , calling reanalyze for {}",it)
                     model.reanalyze.fire(it)
                 }
             }
        }
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
        containingFileUri = containingFileUri
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