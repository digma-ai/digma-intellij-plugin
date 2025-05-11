package org.digma.intellij.plugin.rider.protocol

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.editors.getProjectModelId
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import com.jetbrains.rider.projectView.solution
import org.apache.commons.collections4.map.LRUMap
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.discovery.SpanInfo
import kotlin.random.Random

@Suppress("LightServiceMigrationCode")
class LanguageServiceHost(project: Project) : LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(this::class.java)

    private val csharpMethodCache = LRUMap<String, Boolean>()

    private var model: LanguageServiceModel = project.solution.languageServiceModel

    companion object {
        @JvmStatic
        fun getInstance(project: Project): LanguageServiceHost {
            return project.getService(LanguageServiceHost::class.java)
        }
    }

    init {
        SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(componentLifetime) {
            //clean the cache so it will rebuild after the solution loaded.
            csharpMethodCache.clear()
        }
    }


    //this method should never be called on EDT
    suspend fun getDocumentInfo(virtualFile: VirtualFile, editor: Editor?, language: Language): DocumentInfo? {

        EDT.assertNonDispatchThread()

        Log.trace(logger, "Got request for getDocumentInfo for file {}", virtualFile)

        val projectModelId: Int? = editor?.getProjectModelId()
        val psiId = PsiFileID(projectModelId, virtualFile.url)

        Log.trace(logger, "Sending request to getDocumentInfo with {}", psiId)

        val riderDocumentInfo: RiderDocumentInfo? = model.getDocumentInfo.startSuspending(componentLifetime, psiId)

        if (riderDocumentInfo == null) {
            Log.trace(logger, "Could not load RiderDocumentInfo for {}", virtualFile)
        } else {
            Log.trace(logger, "RiderDocumentInfo for {} loaded '{}'", virtualFile, riderDocumentInfo)
        }

        return riderDocumentInfo?.let { toModel(riderDocumentInfo, language) }
    }


    //always try to send the editor to this method or execute it on EDT, if the editor is null, this method will try
    // to find the selected editor only if executed on EDT.
    suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret {

        Log.trace(logger, "Got request to detectMethodUnderCaret for file {}", virtualFile)


        //projectModelId is the preferred way to find the IPsiSourceFile in rider backend. the backend will try to find
        // by projectModelId and will fall back to find by uri.
        val projectModelId: Int = editor.getProjectModelId()
        val psiUri = virtualFile.url
        val psiId = PsiFileID(projectModelId, psiUri)

        Log.trace(logger, "Sending request to detectMethodUnderCaret with {}", psiId)

        val riderMethodUnderCaret: RiderMethodUnderCaret? =
            model.detectMethodUnderCaret.startSuspending(componentLifetime, MethodUnderCaretRequest(psiId, caretOffset))

        if (riderMethodUnderCaret == null) {
            Log.trace(logger, "Could not load RiderMethodUnderCaret for {}", virtualFile)
        } else {
            Log.trace(logger, "Found RiderMethodUnderCaret for {} , '{}'", virtualFile, riderMethodUnderCaret)
        }

        return riderMethodUnderCaret?.toMethodUnderCaret(caretOffset) ?: MethodUnderCaret.empty(virtualFile.url)
    }


    suspend fun getMethodIdBySpanId(spanId: String): String? {
        Log.trace(logger, "Got request to getMethodIdBySpanId {}", spanId)
        val result = model.getMethodIdBySpanId.startSuspending(spanId)
        if (result == null) {
            Log.trace(logger, "Could not load MethodIdBySpanId for {}", spanId)
        } else {
            Log.trace(logger, "Found MethodIdBySpanId for {} , '{}'", spanId, result)
        }
        return result
    }


    suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(codeObjectIds: List<String>): Map<String, String> {
        Log.trace(logger, "Got request to findWorkspaceUrisForCodeObjectIds {}", codeObjectIds)
        val result = HashMap<String, String>()
        val workspaceUriPairs = model.getWorkspaceUrisForErrorStackTrace.startSuspending(codeObjectIds)
        workspaceUriPairs.forEach {
            result[it.codeObjectId] = it.workspaceUri
        }
        Log.trace(logger, "Found WorkspaceUrisForErrorStackTrace {}", result)
        return result
    }


    suspend fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): MutableMap<String, Pair<String, Int>> {
        Log.trace(logger, "Got request to findWorkspaceUrisForMethodCodeObjectIds {}", methodCodeObjectIds)
        val result = HashMap<String, Pair<String, Int>>()
        val workspaceUriTuples = model.getWorkspaceUrisForMethodCodeObjectIds.startSuspending(methodCodeObjectIds)
        workspaceUriTuples.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri, it.offset)
        }
        Log.trace(logger, "Found WorkspaceUrisForMethodCodeObjectIds {}", result)
        return result
    }

    suspend fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        Log.trace(logger, "Got request to findWorkspaceUrisForSpanIds {}", spanIds)
        val result = HashMap<String, Pair<String, Int>>()
        val workspaceUriTuples = model.getSpansWorkspaceUris.startSuspending(spanIds)
        workspaceUriTuples.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri, it.offset)
        }
        Log.trace(logger, "Found WorkspaceUrisForSpanIds {}", result)
        return result
    }


    suspend fun isCSharpMethod(methodCodeObjectId: String): Boolean {

        Log.trace(logger, "Got request for isCSharpMethod {}", methodCodeObjectId)

        //calls to this method with the same argument may happen many times.
        // but languageServiceHost.isCSharpMethod is a call to resharper which is not the best performance,
        // so keep all methods ids in a simple cache for later use.
        // use the cache only after backend is fully loaded,there may be a result that was stored in
        // the cache before the backend was loaded, and it may be wrong because there was no reference
        // resolving in resharper.
        // if there is a value in the cache that was stored before the solution fully loaded it will be cleared, this class
        // listens to solution load event and clears the csharpMethodCache.
        return if (SolutionLifecycleHost.getInstance(project).isBackendLoaded.value &&
            csharpMethodCache.containsKey(methodCodeObjectId)
        ) {
            Log.trace(logger, "Returning isCSharpMethod for {} from local cache {}", methodCodeObjectId, csharpMethodCache[methodCodeObjectId])
            csharpMethodCache[methodCodeObjectId] == true
        } else {
            val isCHarpMethod = isCSharpMethodImpl(methodCodeObjectId)
            csharpMethodCache[methodCodeObjectId] = isCHarpMethod
            isCHarpMethod
        }
    }


    private suspend fun isCSharpMethodImpl(methodCodeObjectId: String): Boolean {
        return model.isCsharpMethod.startSuspending(methodCodeObjectId)
    }


    fun navigateToMethod(methodId: String) {
        model.protocol?.scheduler?.invokeOrQueue {
            //the message needs to be unique. if a message is the same as the previous one the event is not fired
            val message = "{${Random.nextInt()}}$methodId"
            model.navigateToMethod.fire(message)
        }
    }


    private fun RiderMethodUnderCaret.toMethodUnderCaret(caretOffset: Int) = MethodUnderCaret(
        id = fqn,
        name = name,
        className = className,
        namespace = "",
        fileUri = normalizeFileUri(fileUri, project),
        caretOffset = caretOffset
    )


    private fun toModel(document: RiderDocumentInfo?, language: Language): DocumentInfo? {
        return document?.toDocumentInfo(language)
    }


    private fun RiderDocumentInfo.toDocumentInfo(language: Language) = DocumentInfo(
        fileUri = normalizeFileUri(fileUri, project),
        methods = toMethodInfoMap(methods),
        languageId = language.id
    )

    private fun toMethodInfoMap(methods: List<RiderMethodInfo>): MutableMap<String, MethodInfo> {
        val modelMethods: HashMap<String, MethodInfo> = HashMap()
        methods.forEach {
            modelMethods[it.id] = it.toMethodInfo()
        }
        return modelMethods
    }


    private fun RiderMethodInfo.toMethodInfo(): MethodInfo {
        val methodInfo = MethodInfo(
            id = id,
            name = name,
            containingClass = containingClass,
            containingNamespace = containingNamespace,
            containingFileUri = normalizeFileUri(containingFileUri, project),
        )
        methodInfo.addSpans(toSpansList(spans))
        return methodInfo
    }

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
