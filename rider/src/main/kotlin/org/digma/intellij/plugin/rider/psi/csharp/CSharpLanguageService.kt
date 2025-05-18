package org.digma.intellij.plugin.rider.psi.csharp

import com.intellij.lang.Language
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.rd.util.reactive.whenTrue
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpFileType
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import com.jetbrains.rider.projectView.SolutionStartupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.document.DocumentInfoStorage
import org.digma.intellij.plugin.document.getSelectedTextEditorForFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.NoOpInstrumentationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.rider.protocol.LanguageServiceHost
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageUtil.cSharpLanguageInstanceWithReflection
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.time.Duration.Companion.seconds

@Suppress("LightServiceMigrationCode")
class CSharpLanguageService(project: Project, cs: CoroutineScope) : LifetimedProjectComponent(project), LanguageService {

    private val logger = Logger.getInstance(this::class.java)


    init {
        SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(componentLifetime) {

            /*
            Idea and Rider may reopen files on startup.
            In Idea, we wait for smart mode on startup before building DocumentInfo for open files.
            In Rider the indexes are not ready when building the DocumentInfo for open files.
            Here we wait for the solution loaded and simulate fileOpened again for every file in DocumentInfoStorage
            This code runs on EDT and should not block EDT for long time.
             */

            Log.log(
                logger::trace, "Rider solution loaded, warm startup: {}, initializing {}",
                SolutionStartupService.getInstance(project).isWarmStartup(), SolutionStartupService.getInstance(project).isInitializing()
            )

            FileEditorManager.getInstance(project).openFiles.forEach { file ->
                cs.launch {
                    delay(1.seconds)
                    Log.log(logger::trace, "Rider solution loaded, building DocumentInfo for {}", file)
                    val documentInfo = buildDocumentInfo(file)
                    if (documentInfo != null) {
                        Log.log(logger::trace, "Rider solution loaded, done building DocumentInfo for {}", file)
                        DocumentInfoStorage.getInstance(project).updateDocumentInfoForRider(file, documentInfo)
                    } else {
                        Log.log(logger::warn,"Rider solution loaded,Could not build DocumentInfo for {}",file)
                    }
                }
            }
        }
    }


    override fun getLanguage(): Language {
        return CSharpLanguage
    }

    override fun getFileType(): FileType {
        return CSharpFileType
    }

    override fun runWhenSmart(task: Runnable) {
        val r = Runnable {
            if (DumbService.isDumb(project)) {
                DumbService.getInstance(project).runWhenSmart(task)
            } else {
                task.run()
            }
        }

        LanguageServiceHost.getInstance(project).runIfSolutionLoaded(r)
    }


    override fun getLanguageForMethodCodeObjectId(methodId: String): Language? {

        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in getLanguageForMethodCodeObjectId does not contain \$_$ {}", methodId)
            return null
        }

        if (LanguageServiceHost.getInstance(project).isCSharpMethod(methodId)) {
            return cSharpLanguageInstanceWithReflection
        }
        return null
    }


    override fun getLanguageForClass(className: String): Language? {
        //todo: implement
//        if (LanguageServiceHost.getInstance(project).isCSharpClass(className)) {
//            return CSharpLanguageUtil.getCSharpLanguageInstanceWithReflection();
//        }
        return null
    }


    override fun isSupportedFile(project: Project, newFile: VirtualFile): Boolean {
        if (!isValidVirtualFile(newFile)) {
            return false
        }

        val psiFile = runInReadAccessWithResult { PsiManager.getInstance(project).findFile(newFile) }
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false
        }
        return psiFile?.let {
            isSupportedFile(it)
        } ?: false
    }

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        //todo: remove this check and move it to callers so that this method tests only if a psi file is supported by this language service
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false
        }
        return CSharpLanguageUtil.isCSharpLanguage(psiFile.language)
    }


    override suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret {

        return try {
            LanguageServiceHost.getInstance(project).detectMethodUnderCaret(virtualFile, editor, caretOffset)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, javaClass.getSimpleName() + ".detectMethodUnderCaret", e)
            MethodUnderCaret.empty(virtualFile.url)
        }
    }


    override fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {
        return Backgroundable.executeOnPooledThread(Callable {
            runBlocking {
                try {
                    LanguageServiceHost.getInstance(project).getMethodIdBySpanId(spanCodeObjectId)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError(project, javaClass.getSimpleName() + ".detectMethodBySpan", e)
                    null
                }
            }
        }).get()
    }


    override fun navigateToMethod(methodId: String) {

        Log.log(logger::debug, "got navigate to method request {}", methodId)
        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in navigateToMethod does not contain \$_$, can not navigate {}", methodId)
            return
        }

        LanguageServiceHost.getInstance(project).navigateToMethod(methodId)
    }


    override fun isServiceFor(language: Language): Boolean {
        return CSharpLanguageUtil.isCSharpLanguage(language)
    }

    override fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(codeObjectIds: List<String>): Map<String, String> {
        return Backgroundable.executeOnPooledThread(Callable {
            runBlocking {
                LanguageServiceHost.getInstance(project).findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(codeObjectIds)
            }
        }).get()
    }

    override fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        return Backgroundable.executeOnPooledThread(Callable {
            runBlocking {
                LanguageServiceHost.getInstance(project).findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds)
            }
        }).get()
    }


    override fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        val stopWatch = StopWatch.createStarted()
        try {
            return Backgroundable.executeOnPooledThread(Callable {
                runBlocking {
                    LanguageServiceHost.getInstance(project).findWorkspaceUrisForSpanIds(spanIds)
                }
            }).get()

        } finally {
            stopWatch.stop()
            Log.log(logger::debug, "findWorkspaceUrisForSpanIds time took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS))
        }
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointInfo> {
        return emptySet()
    }

    /**
     * Builds a DocumentInfo.
     * This method doesn't handle exceptions and may throw any kind of exception
     */
    override suspend fun buildDocumentInfo(virtualFile: VirtualFile): DocumentInfo? {

        //don't call this method on EDT or in read access, read access is taken when needed
        EDT.assertNonDispatchThread()
        ReadActions.assertNotInReadAccess()

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "got buildDocumentInfo request for {}", virtualFile)
        }

        //we don't need the psi file here, discovery is done in Rider backend. this is just a validation check
        // to make sure that it's a csharp file that can still be found
        val psiFile = smartReadAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile == null) {
                Log.log(logger::trace, "buildDocumentInfo: could not find psiFile for {}", virtualFile)
                null
            } else if (!PsiUtils.isValidPsiFile(psiFile)) {
                Log.log(logger::trace, "buildDocumentInfo: psiFile is not valid for {}", virtualFile)
                null
            } else if (!isSupportedFile(psiFile)) {
                Log.log(logger::trace, "buildDocumentInfo: psiFile is not supported for {}", virtualFile)
                null
            } else if (!isProjectValid(project)) {
                Log.log(logger::trace, "buildDocumentInfo: project is not valid for {}", virtualFile)
                null
            } else {
                psiFile
            }
        }

        return psiFile?.let {
            suspendableRetry {

                //In Rider the preferred way to find a psi source file in the backend is to use projectModelId which can be found
                //  in the frontend editor. If there is no editor, the psi will be searched by the file url
                //todo: this is a costly operation because it must run on EDT. it happens on every file open and every time
                // the DocumentInfo needs to update. consider removing it or refctore Rider backend to do something else.
                val editor: Editor? = withContext(Dispatchers.EDT) {
                    getSelectedTextEditorForFile(project, virtualFile)
                }

                LanguageServiceHost.getInstance(project).getDocumentInfo(virtualFile, editor,getLanguage())
            }
        }
    }


    override fun isRelevant(file: VirtualFile): Boolean {
        if (file.isDirectory) {
            return false
        }

        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false
        }

        return psiFile?.let {
            isRelevant(it)
        } ?: false


    }


    override fun isRelevant(psiFile: PsiFile): Boolean {
        return ReadActions.ensureReadAction(Supplier {
            PsiUtils.isValidPsiFile(psiFile) &&
                    psiFile.isWritable &&
                    isSupportedFile(psiFile)
        })
    }

    override fun isCodeVisionSupported(): Boolean {
        return false
    }


    override fun getPsiElementForMethod(methodId: String): PsiElement? {
        //todo: implement
        return null
    }

    override fun getPsiElementForClassByMethodId(methodId: String): PsiElement? {
        return null
    }

    override fun getPsiElementForClassByName(className: String): PsiElement? {
        return null
    }

    override fun getInstrumentationProvider(): InstrumentationProvider {
        return NoOpInstrumentationProvider()
    }

    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: MutableList<String?>): MutableMap<String?, PsiElement?> {
        //Never called for csharp language,
        //this method is used by CodeLensService, which is only relevant for java/kotlin/python.
        //Rider does code lens differently
        throw UnsupportedOperationException("should not be called for CSharp")
    }

    override fun dispose() {

    }


}

