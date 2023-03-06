package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.framework.impl.RpcTimeouts
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.editors.getProjectModelId
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.PsiUtils
import kotlin.random.Random

class LanguageServiceHost(project: Project) : LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(LanguageServiceHost::class.java)


    private var model: LanguageServiceModel = project.solution.languageServiceModel


    init {
        SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(componentLifetime) {
            Log.log(logger::debug, "Initializing model only when solution loaded")
        }
    }

    //always use getInstance instead of injecting directly to other services.
    // this ensures lazy init only when this host is needed.
    // when injecting directly in constructor of other services it needs to load the solution model
    // and that required EDT which is not always a good idea.
    companion object {
        @JvmStatic
        fun getInstance(project: Project): LanguageServiceHost {
            return project.getService(LanguageServiceHost::class.java)
        }
    }


    @Suppress("unused")
    fun getDocumentInfo(psiFile: PsiFile): DocumentInfo? {
        Log.log(logger::debug, "Got request for getDocumentInfo for PsiFile {}", psiFile.virtualFile)
        return getDocumentInfo(psiFile, null)
    }

    fun getDocumentInfo(psiFile: PsiFile, newEditor: FileEditor?): DocumentInfo? {

        Log.log(
            logger::debug,
            "Got request for getDocumentInfo for PsiFile {}, selectedEditor",
            psiFile.virtualFile,
            newEditor
        )

        val psiUri = PsiUtils.psiFileToUri(psiFile)
        var projectModelId: Int? = null
        if (newEditor != null && newEditor is TextEditor) {
            projectModelId = newEditor.editor.getProjectModelId()
        }

        return try {
            val riderDocumentInfo =
                model.getDocumentInfo.sync(PsiFileID(projectModelId, psiUri),RpcTimeouts.longRunning)
            riderDocumentInfo?.let { toModel(riderDocumentInfo) }
        } catch (e: Exception) {
            Log.debugWithException(logger, e, "Got exception from getDocumentInfo {}", e.message)
            null
        }
    }


    fun detectMethodUnderCaret(psiFile: PsiFile, selectedEditor: Editor?, caretOffset: Int): MethodUnderCaret {

        Log.log(
            logger::debug,
            "Got request to detectMethodUnderCaret for PsiFile {}, selectedEditor",
            psiFile.virtualFile
        )


        //projectModelId is the preferred way to find a IPsiSourceFile in rider backend. the backend will try to find
        // by projectModelId and will fall back to find by uri.
        var projectModelId: Int? = null
        if (selectedEditor != null) {
            projectModelId = selectedEditor.getProjectModelId()
        }

        val psiUri = PsiUtils.psiFileToUri(psiFile)
        return try {
            val riderMethodUnderCaret =
                model.detectMethodUnderCaret.sync(MethodUnderCaretRequest(PsiFileID(projectModelId, psiUri), caretOffset),RpcTimeouts.longRunning)
            riderMethodUnderCaret?.toMethodUnderCaret() ?: MethodUnderCaret("", "", "", "")
        } catch (e: Exception) {
            Log.debugWithException(
                logger,
                e,
                "Got exception from detectMethodUnderCaret for {}, {}. returning empty",
                psiFile,
                e.message
            )
            MethodUnderCaret("", "", "", "")
        }
    }


    fun findWorkspaceUrisForCodeObjectIds(codeObjectIds: MutableList<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val workspaceUriPairs = model.getWorkspaceUris.sync(codeObjectIds, RpcTimeouts.longRunning)
        workspaceUriPairs.forEach {
            result[it.codeObjectId] = it.workspaceUri
        }
        return result
    }

    fun findWorkspaceUrisForSpanIds(spanIds: MutableList<String>): Map<String, Pair<String, Int>> {
        val result = HashMap<String, Pair<String, Int>>()
        val workspaceUris = model.getSpansWorkspaceUris.sync(spanIds, RpcTimeouts.longRunning)
        workspaceUris.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri, it.offset)
        }
        return result
    }


    fun isCSharpMethod(methodCodeObjectId: String): Boolean {
        return model.isCsharpMethod.sync(methodCodeObjectId, RpcTimeouts.longRunning)
    }


    fun navigateToMethod(codeObjectId: String) {
        model.protocol.scheduler.invokeOrQueue {
            //the message needs to be unique. if a message is the same as the previous one the event is not fired
            val message = "{${Random.nextInt()}}$codeObjectId"
            model.navigateToMethod.fire(message)
        }
    }


    private fun RiderMethodUnderCaret.toMethodUnderCaret() = MethodUnderCaret(
        id = fqn,
        name = name,
        className = className,
        fileUri = normalizeFileUri(fileUri, project)
    )


    private fun toModel(document: RiderDocumentInfo?): DocumentInfo? {
        return document?.toDocumentInfo()
    }


    private fun RiderDocumentInfo.toDocumentInfo() = DocumentInfo(
        fileUri = normalizeFileUri(fileUri, project),
        methods = toMethodInfoMap(methods)
    )

    private fun toMethodInfoMap(methods: List<RiderMethodInfo>): MutableMap<String, MethodInfo> {
        val modelMethods: HashMap<String, MethodInfo> = HashMap()
        methods.forEach {
            modelMethods[it.id] = it.toMethodInfo()
        }
        return modelMethods
    }


    private fun RiderMethodInfo.toMethodInfo() = MethodInfo(
        id = id,
        name = name,
        containingClass = containingClass,
        containingNamespace = containingNamespace,
        containingFileUri = normalizeFileUri(containingFileUri, project),
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
        containingMethodId = containingMethod,
        containingFileUri = normalizeFileUri(containingFileUri, project)
    )


}