package org.digma.intellij.plugin.python

import com.intellij.lang.Language
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.stubs.PyClassNameIndex
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.discovery.FileDiscoveryProvider
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.NoOpInstrumentationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.python.discovery.PythonFileDiscoveryProvider
import org.digma.intellij.plugin.python.discovery.findFunctionByMethodId
import org.digma.intellij.plugin.python.navigation.PythonSpanNavigationProvider
import java.util.function.Supplier
import kotlin.coroutines.coroutineContext

@Suppress("LightServiceMigrationCode", "UnstableApiUsage")
class PythonLanguageService(private val project: Project) : LanguageService, DisposableAdaptor {

    private val logger: Logger = thisLogger()

    override fun getLanguage(): Language {
        return PythonLanguage.INSTANCE
    }

    override fun getFileType(): FileType {
        return PythonFileType.INSTANCE
    }

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return ReadActions.ensureReadAction(Supplier {
            psiFile is PyFile &&
                    PsiUtils.isValidPsiFile(psiFile) &&
                    PythonLanguage.INSTANCE == psiFile.viewProvider.baseLanguage
        })
    }

    override suspend fun detectMethodUnderCaret(
        virtualFile: VirtualFile,
        editor: Editor,
        caretOffset: Int
    ): MethodUnderCaret {

        Log.trace(logger, project, "detectMethodUnderCaret called {}", virtualFile)

        if (!isSupportedFile(virtualFile)) {
            return MethodUnderCaret.empty(virtualFile.url)
        }

        if (!isValidVirtualFile(virtualFile)) {
            return MethodUnderCaret.EMPTY
        }

        return smartReadAction(project) {
            return@smartReadAction detectMethodUnderCaretImpl(virtualFile, caretOffset)
        }
    }

    @RequiresReadLock(generateAssertion = false)
    private fun detectMethodUnderCaretImpl(
        virtualFile: VirtualFile,
        caretOffset: Int
    ): MethodUnderCaret {

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)?.takeIf { it.isValid && virtualFile.isValid }
            ?: return MethodUnderCaret.empty(virtualFile.url)

        val underCaret: PsiElement = psiFile.findElementAt(caretOffset).takeIf { psiFile.isValid && virtualFile.isValid }
            ?: return MethodUnderCaret.empty(virtualFile.url)

        val pyFunction = underCaret.takeIf { psiFile.isValid && virtualFile.isValid }?.let {
            PsiTreeUtil.getParentOfType(underCaret, PyFunction::class.java)
        } ?: return MethodUnderCaret.empty(virtualFile.url)

        val methodId = createPythonMethodCodeObjectId(project, pyFunction)
        val name = pyFunction.name ?: ""
        val containingClassName = pyFunction.containingClass?.name ?: ""
        return MethodUnderCaret(methodId, name, containingClassName, "", virtualFile.url, caretOffset)

    }

    override suspend fun buildDocumentInfo(virtualFile: VirtualFile): DocumentInfo? {

        Log.trace(logger, project, "buildDocumentInfo called {}", virtualFile)

        //don't call this method on EDT or in read access, read access is acquired when needed
        EDT.assertNonDispatchThread()
        ReadActions.assertNotInReadAccess()

        Log.trace(logger, project, "got buildDocumentInfo request for {}", virtualFile)

        if (!isValidVirtualFile(virtualFile)) {
            Log.trace(logger, project, "buildDocumentInfo: virtualFile is not valid for {}", virtualFile)
            return null
        }
        val psiFile = smartReadAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile == null) {
                Log.trace(logger, project, "buildDocumentInfo: could not find psiFile for {}", virtualFile)
                null
            } else if (psiFile !is PyFile) {
                Log.trace(logger, project, "buildDocumentInfo: psiFile is not PyFile {}", virtualFile)
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
        coroutineContext.ensureActive()
        return psiFile?.let {
            suspendableRetry {
                coroutineContext.ensureActive()
                org.digma.intellij.plugin.python.discovery.buildDocumentInfo(project, it, virtualFile.url, getLanguage())
            }
        }
    }

    override suspend fun getLanguageForMethodCodeObjectId(methodId: String): Language? {
        val methodName = methodId.substringAfter("\$_$")

        val function = readAction {
            PyFunctionNameIndex.find(methodName, project, GlobalSearchScope.projectScope(project))
                .firstOrNull { createPythonMethodCodeObjectId(project, it) == methodId }
        }

        return function?.let {
            PythonLanguage.INSTANCE
        }
    }

    override suspend fun getLanguageForClass(className: String): Language? {
        val classNameToFind = className.substringAfterLast(".")
        val clazz = readAction {
            PyClassNameIndex.find(classNameToFind, project, GlobalSearchScope.projectScope(project))
                .firstOrNull { it.qualifiedName == className }
        }

        return clazz?.let {
            PythonLanguage.INSTANCE
        }
    }

    override suspend fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {
        return PythonSpanNavigationProvider.getInstance(project).getMethodIdBySpanId(spanCodeObjectId)
    }

    override suspend fun navigateToMethod(methodId: String) {
        val methodName = getPythonMethodNameFromMethodId(methodId)
        val function = readAction {
            PyFunctionNameIndex.find(methodName, project, GlobalSearchScope.projectScope(project))
                .firstOrNull { createPythonMethodCodeObjectId(project, it) == methodId }
        }

        function?.takeIf { it.canNavigate() }.let {
            it?.navigate(true)
        }
    }

    override suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String> {

        val workspaceUrls = mutableMapOf<String, String>()

        methodCodeObjectIds.filter { isPythonMethodId(it) }.forEach { methodId ->
            val url = smartReadAction(project) {
                val function = findFunctionByMethodId(project, methodId) { GlobalSearchScope.projectScope(project) }
                function?.containingFile?.virtualFile?.url
            }
            url?.let {
                workspaceUrls[methodId] = it
            }
        }

        return workspaceUrls
    }

    override suspend fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {

        val workspaceUrls = mutableMapOf<String, Pair<String, Int>>()

        methodCodeObjectIds.filter { isPythonMethodId(it) }.forEach { methodId ->
            val pair = smartReadAction(project) {
                val function = findFunctionByMethodId(project, methodId) { GlobalSearchScope.projectScope(project) }
                function?.let { Pair(it.containingFile.virtualFile.url, it.textOffset) }
            }
            pair?.let {
                workspaceUrls[methodId] = it
            }
        }

        return workspaceUrls
    }

    override suspend fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        return PythonSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds)
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointLocation> {
        return setOf()
    }

    override fun isCodeVisionSupported(): Boolean = true

    override suspend fun findMethodPsiElementByMethodId(methodId: String): PsiElement? {
        return smartReadAction(project) {
            findFunctionByMethodId(project, methodId) { GlobalSearchScope.projectScope(project) }
        }
    }

    override suspend fun findClassPsiElementByMethodId(methodId: String): PsiElement? {
        return smartReadAction(project) {
            val function = findFunctionByMethodId(project, methodId) { GlobalSearchScope.projectScope(project) }
            function?.containingClass
        }
    }

    override suspend fun findClassPsiElementByClassName(className: String): PsiElement? {
        return smartReadAction(project) {
            PyClassNameIndex.findByQualifiedName(className, project, GlobalSearchScope.projectScope(project))
                .firstOrNull { it.isValid }
        }
    }

    override fun getInstrumentationProvider(): InstrumentationProvider {
        return NoOpInstrumentationProvider()
    }

    override fun findMethodsByCodeObjectIds(
        psiFile: PsiFile,
        methodIds: List<String>
    ): Map<String, PsiElement> {

        if (methodIds.isEmpty() || !PsiUtils.isValidPsiFile(psiFile)) {
            return emptyMap()
        }

        val functionNames = methodIds.associateBy { it.substringAfter("\$_$") }

        return ReadActions.ensureReadAction(Supplier {
            PsiTreeUtil.collectElementsOfType(psiFile, PyFunction::class.java)
                .filter { it.name != null && it.name in functionNames.keys }
                .associateBy { functionNames[it.name as String] as String }
        }) ?: emptyMap()

    }

    override fun getDiscoveryProvider(): FileDiscoveryProvider {
        return PythonFileDiscoveryProvider()
    }
}