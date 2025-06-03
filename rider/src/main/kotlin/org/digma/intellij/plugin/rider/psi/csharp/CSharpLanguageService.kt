package org.digma.intellij.plugin.rider.psi.csharp

import com.intellij.lang.Language
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileType
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.document.DocumentInfoStorage
import org.digma.intellij.plugin.document.getSelectedTextEditorForFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.NoOpInstrumentationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.rider.protocol.LanguageServiceHost
import kotlin.time.Duration.Companion.seconds

@Suppress("LightServiceMigrationCode")
class CSharpLanguageService(project: Project) : LifetimedProjectComponent(project), LanguageService {

    private val logger = Logger.getInstance(this::class.java)


    init {
        SolutionLifecycleHost.getInstance(project).isBackendLoaded.whenTrue(componentLifetime) {

            /*
            Idea and Rider may reopen files on startup.
            In Idea, we wait for smart mode on startup before building DocumentInfo for open files.
            In Rider the indexes are not ready when building the DocumentInfo for open files.
            Here we wait for the solution loaded and simulate fileOpened again for every file in DocumentInfoStorage
            This code runs on EDT and should not block EDT for a long time.
             */

            Log.trace(
                logger, "Rider solution loaded, warm startup: {}, initializing {}",
                SolutionStartupService.getInstance(project).isWarmStartup(), SolutionStartupService.getInstance(project).isInitializing()
            )

            FileEditorManager.getInstance(project).openFiles.forEach { file ->
                componentLifetime.coroutineScope.launch {
                    delay(1.seconds) //just wait a second for the solution to fully load, it seems necessary
                    Log.trace(logger, "Rider solution loaded, building DocumentInfo for {}", file)
                    val documentInfo = buildDocumentInfo(file)
                    if (documentInfo != null) {
                        Log.trace(logger, "Rider solution loaded, done building DocumentInfo for {}", file)
                        DocumentInfoStorage.getInstance(project).updateDocumentInfoForRider(file, documentInfo)
                    } else {
                        Log.log(logger::warn, "Rider solution loaded,Could not build DocumentInfo for {}", file)
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

    override suspend fun getLanguageForMethodCodeObjectId(methodId: String): Language? {

        if (methodId.indexOf("\$_$") <= 0) {
            Log.trace(logger, "method id in getLanguageForMethodCodeObjectId does not contain \$_$ {}", methodId)
            return null
        }

        return if (LanguageServiceHost.getInstance(project).isCSharpMethod(methodId)) {
            return CSharpLanguage
        } else {
            null
        }
    }


    override suspend fun getLanguageForClass(className: String): Language? {
        //todo: implement
//        if (LanguageServiceHost.getInstance(project).isCSharpClass(className)) {
//            return CSharpLanguageUtil.getCSharpLanguageInstanceWithReflection();
//        }
        return null
    }


    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return CSharpLanguage == psiFile.language
    }


    override suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret {
        return LanguageServiceHost.getInstance(project).detectMethodUnderCaret(virtualFile, editor, caretOffset)
    }


    override suspend fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {
        return try {
            LanguageServiceHost.getInstance(project).getMethodIdBySpanId(spanCodeObjectId)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "Error in detectMethodBySpan {}", e)
            ErrorReporter.getInstance().reportError(project, javaClass.getSimpleName() + ".detectMethodBySpan", e)
            null
        }
    }


    override suspend fun navigateToMethod(methodId: String) {

        Log.trace(logger, "got navigate to method request {}", methodId)
        if (methodId.indexOf("\$_$") <= 0) {
            Log.trace(logger, "method id in navigateToMethod does not contain \$_$, can not navigate {}", methodId)
            return
        }

        LanguageServiceHost.getInstance(project).navigateToMethod(methodId)
    }


    override suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String> {
        return LanguageServiceHost.getInstance(project).findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds)
    }

    override suspend fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        return LanguageServiceHost.getInstance(project).findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds)
    }


    override suspend fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        return LanguageServiceHost.getInstance(project).findWorkspaceUrisForSpanIds(spanIds)
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointLocation> {
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

        Log.trace(logger, project, "got buildDocumentInfo request for {}", virtualFile)

        if (!isValidVirtualFile(virtualFile)) {
            Log.trace(logger, project, "buildDocumentInfo: virtualFile is not valid for {}", virtualFile)
            return null
        }

        //we don't need the psi file here, discovery is done in Rider backend. this is just a validation check
        // to make sure that it's a csharp file that can still be found
        val psiFile = smartReadAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile == null) {
                Log.trace(logger, project, "buildDocumentInfo: could not find psiFile for {}", virtualFile)
                null
            } else if (!PsiUtils.isValidPsiFile(psiFile)) {
                Log.trace(logger, project, "buildDocumentInfo: psiFile is not valid for {}", virtualFile)
                null
            } else if (!isSupportedFile(psiFile)) {
                Log.trace(logger, project, "buildDocumentInfo: psiFile is not supported for {}", virtualFile)
                null
            } else if (!isProjectValid(project)) {
                Log.trace(logger, project, "buildDocumentInfo: project is not valid for {}", virtualFile)
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
                // the DocumentInfo needs to update. consider removing it or refactor Rider backend to do something else.
                val editor: Editor? = withContext(Dispatchers.EDT) {
                    getSelectedTextEditorForFile(project, virtualFile)
                }

                LanguageServiceHost.getInstance(project).getDocumentInfo(virtualFile, editor, getLanguage())
            }
        }
    }


    override fun isCodeVisionSupported(): Boolean {
        return false
    }


    override suspend fun findMethodPsiElementByMethodId(methodId: String): PsiElement? {
        //not relevant for Rider
        return null
    }

    override suspend fun findClassPsiElementByMethodId(methodId: String): PsiElement? {
        //this method is used for running tests, not supported in Rider
        return null
    }

    override suspend fun findClassPsiElementByClassName(className: String): PsiElement? {
        //this method is used for running tests, not supported in Rider
        return null
    }

    override fun getInstrumentationProvider(): InstrumentationProvider {
        return NoOpInstrumentationProvider()
    }

    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: List<String>): Map<String, PsiElement> {
        //Never called for csharp language,
        //this method is used by CodeLensService, which is only relevant for java/kotlin/python.
        //Rider does code lens differently in Rider backend, so this method is never called in Rider.
        throw UnsupportedOperationException("should not be called for CSharp")
    }

    override fun dispose() {

    }
}

