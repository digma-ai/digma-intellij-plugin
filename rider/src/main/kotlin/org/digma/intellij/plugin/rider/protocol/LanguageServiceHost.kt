package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import com.jetbrains.rider.projectView.SolutionStartupService
import com.jetbrains.rider.projectView.solution
import kotlinx.coroutines.runBlocking
import org.apache.commons.collections4.map.LRUMap
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.BuildDocumentInfoProcessContext
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageUtil
import org.digma.intellij.plugin.ui.CaretContextService
import kotlin.random.Random

@Suppress("LightServiceMigrationCode")
class LanguageServiceHost(project: Project) : LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(LanguageServiceHost::class.java)

    private val csharpMethodCache = LRUMap<String, Boolean>()

    private var model: LanguageServiceModel = project.solution.languageServiceModel

    private var solutionLoaded: Boolean = false

    //always use getInstance instead of injecting directly to other services.
    // this ensures lazy init only when this host is needed.
    // when injecting directly in constructor of other services it needs to load the solution model
    // and that required EDT which is not always the case.
    companion object {
        @JvmStatic
        fun getInstance(project: Project): LanguageServiceHost {
            return project.getService(LanguageServiceHost::class.java)
        }
    }

    init {

        SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(componentLifetime) {
            Log.log(logger::debug, "solution loaded, warm startup: {}, initializing {}",
                SolutionStartupService.getInstance(project).isWarmStartup(),SolutionStartupService.getInstance(project).isInitializing())

            solutionLoaded = true
            csharpMethodCache.clear()
            refreshCodeObjectsAndSelectedEditorOnSolutionLoaded(project, SolutionStartupService.getInstance(project).isWarmStartup())
        }
    }

    private fun getProtocol(model: LanguageServiceModel): IProtocol {
        return model.protocol!! //protocol is nullable in 2023.2, remove when 2023.2 is our base
    }

    /*
        the IDE remembers open files on shutdown and reopens them on startup.
        EditorEventsHandler.selectionChanged catches the opening of the files,
        loads code objects and insights in smart mode and updates the UI.
        while smart mode is good for java,python, it does not guarantee that Rider C# solution
        is loaded, so it may be that code objects are not found for those files in smart mode and there will be no
        insights for them.
        only after the solution is loaded it is guaranteed to find code objects.
        This method runs on solution loaded event, clears all documents from DocumentInfoService, that includes
        code objects and insights. reloads code objects for the selected editor if any, and updates
        the method context. the other documents will be forced to load code objects again in
        EditorEventsHandler.selectionChanged when switching to them.
     */
    private fun refreshCodeObjectsAndSelectedEditorOnSolutionLoaded(project: Project, warmStartup: Boolean) {

        Log.log(logger::debug, "refreshCodeObjectsAndSelectedEditorOnSolutionLoaded called")

        //usually just a second or two is needed before all indexing is complete
        if (!warmStartup) {
            Thread.sleep(1000)
        }

        Log.log(logger::debug, "in refreshCodeObjectsAndSelectedEditorOnSolutionLoaded , updating method context")
        val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
        selectedTextEditor?.let {
            val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
            selectedEditor?.let {
                val virtualFile = selectedEditor.file
                virtualFile?.takeIf { isValidVirtualFile(virtualFile) }.let {
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    psiFile?.takeIf { PsiUtils.isValidPsiFile(psiFile) }?.let {

                        if (CSharpLanguageUtil.isCSharpLanguage(psiFile.language)) {

                            val languageService = LanguageServiceLocator.getInstance(project).locate(psiFile.language)

                            if (languageService.isRelevant(psiFile)) {
                                val offset = selectedTextEditor.logicalPositionToOffset(selectedTextEditor.caretModel.logicalPosition)

                                Backgroundable.executeOnPooledThread {

                                    BuildDocumentInfoProcessContext.buildDocumentInfoUnderProcess(project) { progressIndicator: ProgressIndicator ->
                                        val context = BuildDocumentInfoProcessContext(progressIndicator)
                                        val documentInfoService = DocumentInfoService.getInstance(project)
                                        val documentInfo = languageService.buildDocumentInfo(psiFile, selectedEditor, context)
                                        documentInfo.let {
                                            documentInfoService.addCodeObjects(psiFile, documentInfo)
                                        }
                                        val methodUnderCaret =
                                            detectMethodUnderCaret(psiFile, selectedTextEditor, offset)
                                        CaretContextService.getInstance(project).contextChanged(methodUnderCaret)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }


    //will wait for solution to load, or execute immediately is solution is already loaded
    fun runIfSolutionLoaded(r: Runnable) {
        Log.log(logger::debug, "runIfSolutionLoaded called")
        SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(componentLifetime) {
            Log.log(logger::debug, "executing task in solution loaded")
            r.run()
        }
    }


    //avoid using this method and always use overloaded with FileEditor because we need the ProjectModelId
    @Suppress("unused")
    fun getDocumentInfo(psiFile: PsiFile): DocumentInfo? {
        Log.log(logger::debug, "Got request for getDocumentInfo for PsiFile {}", psiFile.virtualFile)
        var editor: FileEditor? = null
        if (EDT.isEdt()) {
            editor = FileEditorManager.getInstance(project).getSelectedEditor(psiFile.virtualFile)
        }
        return getDocumentInfo(psiFile, editor)
    }


    //this method should never be called on EDT
    fun getDocumentInfo(psiFile: PsiFile, fileEditor: FileEditor?): DocumentInfo? {

        Log.log(logger::debug,"Got request for getDocumentInfo for PsiFile {}, selectedEditor {}, solution loaded {}",psiFile.virtualFile,fileEditor,solutionLoaded)

        val projectModelId: Int? = tryGetProjectModelId(psiFile, fileEditor, project)
        val psiUri = PsiUtils.psiFileToUri(psiFile)
        val psiId = PsiFileID(projectModelId, psiUri)

        Log.log(logger::debug, "Sending request to getDocumentInfo with {}", psiId)

        val riderDocumentInfo: RiderDocumentInfo? =
            runBlocking {
                model.getDocumentInfo.startSuspending((psiId))
            }

        if (riderDocumentInfo == null) {
            Log.log(logger::debug, "Could not load RiderDocumentInfo for {}", psiFile.virtualFile)
        } else {
            Log.log(logger::debug, "RiderDocumentInfo for {} loaded '{}'", psiFile.virtualFile, riderDocumentInfo)
        }

        return riderDocumentInfo?.let { toModel(riderDocumentInfo) }
    }


    //always try to send the editor to this method, or execute it on EDT, if the editor is null this method will try
    // to find selected editor only if executed on EDT.
    fun detectMethodUnderCaret(psiFile: PsiFile, selectedEditor: Editor?, caretOffset: Int): MethodUnderCaret {

        Log.log(logger::debug,"Got request to detectMethodUnderCaret for PsiFile {}, selectedEditor {}, solution loaded {}",psiFile.virtualFile,selectedEditor,solutionLoaded)

        //always try to find ProjectModelId.
        //projectModelId is the preferred way to find a IPsiSourceFile in rider backend. the backend will try to find
        // by projectModelId and will fall back to find by uri.
        val projectModelId: Int? = tryGetProjectModelId(psiFile, selectedEditor, project)

        val psiUri = PsiUtils.psiFileToUri(psiFile)
        val psiId = PsiFileID(projectModelId, psiUri)

        Log.log(logger::debug, "Sending request to detectMethodUnderCaret with {}", psiId)

        val riderMethodUnderCaret: RiderMethodUnderCaret? =
            if (ApplicationManager.getApplication().isDispatchThread) {
                model.detectMethodUnderCaret.callSynchronously(MethodUnderCaretRequest(psiId, caretOffset), getProtocol(model))
            } else {
                runBlocking {
                    model.detectMethodUnderCaret.startSuspending(MethodUnderCaretRequest(psiId, caretOffset))
                }
            }


        if (riderMethodUnderCaret == null) {
            Log.log(logger::debug, "Could not load RiderMethodUnderCaret for {}", psiFile.virtualFile)
        } else {
            Log.log(logger::debug, "Found RiderMethodUnderCaret for {} , '{}'", psiFile.virtualFile, riderMethodUnderCaret)
        }

        return riderMethodUnderCaret?.toMethodUnderCaret(caretOffset) ?: MethodUnderCaret("", "", "", "", "", caretOffset)
    }


    fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(codeObjectIds: MutableList<String>): Map<String, String> {

        Log.log(logger::debug, "Got request to findWorkspaceUrisForCodeObjectIds {}", codeObjectIds)

        val result = HashMap<String, String>()

        val workspaceUriPairs =
            if (ApplicationManager.getApplication().isDispatchThread) {
                model.getWorkspaceUrisForErrorStackTrace.callSynchronously(codeObjectIds, getProtocol(model))
            } else {
                runBlocking {
                    model.getWorkspaceUrisForErrorStackTrace.startSuspending(codeObjectIds)
                }
            }

        workspaceUriPairs?.forEach {
            result[it.codeObjectId] = it.workspaceUri
        }
        Log.log(logger::debug, "Found WorkspaceUrisForErrorStackTrace {}", result)
        return result
    }


    fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: MutableList<String>): MutableMap<String, Pair<String, Int>> {
        Log.log(logger::debug, "Got request to findWorkspaceUrisForMethodCodeObjectIds {}", methodCodeObjectIds)

        val result = HashMap<String, Pair<String, Int>>()

        val workspaceUriTuples =
            if (ApplicationManager.getApplication().isDispatchThread) {
                model.getWorkspaceUrisForMethodCodeObjectIds.callSynchronously(methodCodeObjectIds, getProtocol(model))
            } else {
                runBlocking {
                    model.getWorkspaceUrisForMethodCodeObjectIds.startSuspending(methodCodeObjectIds)
                }
            }

        workspaceUriTuples?.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri, it.offset)
        }
        Log.log(logger::debug, "Found WorkspaceUrisForMethodCodeObjectIds {}", result)
        return result
    }

    fun findWorkspaceUrisForSpanIds(spanIds: MutableList<String>): Map<String, Pair<String, Int>> {

        Log.log(logger::debug, "Got request to findWorkspaceUrisForSpanIds {}", spanIds)

        val result = HashMap<String, Pair<String, Int>>()

        val workspaceUriTuples =
            if (ApplicationManager.getApplication().isDispatchThread) {
                model.getSpansWorkspaceUris.callSynchronously(spanIds, getProtocol(model))
            } else {
                runBlocking {
                    model.getSpansWorkspaceUris.startSuspending(spanIds)
                }
            }

        workspaceUriTuples?.forEach {
            result[it.codeObjectId] = Pair(it.workspaceUri, it.offset)
        }
        Log.log(logger::debug, "Found WorkspaceUrisForSpanIds {}", result)
        return result
    }


    fun isCSharpMethod(methodCodeObjectId: String): Boolean {

        Log.log(logger::debug, "Got request for isCSharpMethod {}, solution loaded {}", methodCodeObjectId, solutionLoaded)

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
            Log.log(logger::debug, "Returning isCSharpMethod for {} from local cache {}", methodCodeObjectId, csharpMethodCache[methodCodeObjectId])
            csharpMethodCache[methodCodeObjectId] == true
        } else {
            val isCHarpMethod = isCSharpMethodImpl(methodCodeObjectId)
            csharpMethodCache[methodCodeObjectId] = isCHarpMethod
            isCHarpMethod
        }
    }


    private fun isCSharpMethodImpl(methodCodeObjectId: String): Boolean {

        val result =
            if (ApplicationManager.getApplication().isDispatchThread) {
                model.isCsharpMethod.callSynchronously(methodCodeObjectId, getProtocol(model))
            } else {
                runBlocking {
                    model.isCsharpMethod.startSuspending(methodCodeObjectId)
                }
            }

        return result == true
    }


    fun navigateToMethod(methodId: String) {
        getProtocol(model).scheduler.invokeOrQueue {
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


    private fun RiderMethodInfo.toMethodInfo(): MethodInfo {
        val methodInfo = MethodInfo(
            id = id,
            name = name,
            containingClass = containingClass,
            containingNamespace = containingNamespace,
            containingFileUri = normalizeFileUri(containingFileUri, project),
            offsetAtFileUri = offsetAtFileUri,
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
