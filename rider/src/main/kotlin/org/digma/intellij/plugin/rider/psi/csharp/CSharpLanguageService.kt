package org.digma.intellij.plugin.rider.psi.csharp

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.SolutionLifecycleHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.executeCatchingWithResult
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.document.BuildDocumentInfoProcessContext
import org.digma.intellij.plugin.editor.CaretContextService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.NoOpInstrumentationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiFileCachedValueWithUri
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.rider.protocol.CodeLensHost
import org.digma.intellij.plugin.rider.protocol.LanguageServiceHost
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageUtil.cSharpLanguageInstanceWithReflection
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

@Suppress("LightServiceMigrationCode")
class CSharpLanguageService(project: Project, private val cs: CoroutineScope) : LifetimedProjectComponent(project), LanguageService {

    private val logger = Logger.getInstance(this::class.java)


    override fun ensureStartupOnEDT(project: Project) {
        Log.log(
            logger::debug,
            "ensureStartupOnEDT called, backend loaded: {}",
            SolutionLifecycleHost.Companion.getInstance(project).isBackendLoaded.value
        )
        //make sure LanguageServiceHost is initialized on EDT, for example project.solution.languageServiceModel must be
        // called on EDT
        LanguageServiceHost.getInstance(project)
        CodeLensHost.getInstance(project)
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
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false
        }
        return CSharpLanguageUtil.isCSharpLanguage(psiFile.language)
    }


    override fun detectMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor?, caretOffset: Int): MethodUnderCaret {
        //detectMethodUnderCaret should run very fast and return a result,
        // this operation may become invalid very soon if the user clicks somewhere else.
        // no retry because it needs to complete very fast
        //it may be called from EDT or background, runInReadAccessWithResult will acquire read access if necessary.

        return Backgroundable.executeOnPooledThread(Callable {

            executeCatchingWithResult({
                runInReadAccessWithResult {
                    runBlocking {
                        LanguageServiceHost.getInstance(project).detectMethodUnderCaret(psiFile, selectedEditor, caretOffset)
                    }
                }
            }, { throwable ->
                ErrorReporter.getInstance().reportError(project, javaClass.getSimpleName() + ".detectMethodUnderCaret", throwable)
                MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), caretOffset, null, false)
            })

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


    override fun buildDocumentInfo(psiFileCachedValue: PsiFileCachedValueWithUri, context: BuildDocumentInfoProcessContext): DocumentInfo {
        return buildDocumentInfo(psiFileCachedValue, null, context)
    }


    override fun buildDocumentInfo(
        psiFileCachedValue: PsiFileCachedValueWithUri,
        newEditor: FileEditor?,
        context: BuildDocumentInfoProcessContext
    ): DocumentInfo {
        val psiFile = psiFileCachedValue.value
        if (psiFile == null || !PsiUtils.isValidPsiFile(psiFile)) {
            return DocumentInfo(psiFileCachedValue.uri, mutableMapOf())
        }

        Log.log(logger::debug, "got buildDocumentInfo request for {}", psiFile)
        //must be CSharpFile
        if (CSharpLanguageUtil.isCSharpFile(psiFile)) {
            var documentInfo = Backgroundable.executeOnPooledThread(Callable {
                runBlocking {
                    LanguageServiceHost.getInstance(project).getDocumentInfo(psiFile, newEditor)
                }
            }).get()

            if (documentInfo == null) {
                Log.log(logger::warn, "DocumentInfo not found for {}, returning empty DocumentInfo", psiFileCachedValue.value)
                documentInfo = DocumentInfo(psiFileCachedValue.uri, mutableMapOf())
            }
            return documentInfo
        } else {
            Log.log(logger::debug, "psi file is not CSharpFile, returning empty DocumentInfo for {}", psiFileCachedValue.value)
            return DocumentInfo(psiFileCachedValue.uri, mutableMapOf())
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


    override fun refreshMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor?, offset: Int) {
        val methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedEditor, offset)
        CaretContextService.getInstance(project).contextChanged(methodUnderCaret)
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
        TODO("Not yet implemented")
    }


}

