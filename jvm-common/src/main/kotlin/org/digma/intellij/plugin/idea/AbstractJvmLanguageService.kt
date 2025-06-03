package org.digma.intellij.plugin.idea

import com.intellij.lang.Language
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.document.DocumentInfoStorage
import org.digma.intellij.plugin.idea.discovery.AbstractCodeObjectDiscovery
import org.digma.intellij.plugin.idea.discovery.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.discovery.findMethodInClass
import org.digma.intellij.plugin.idea.discovery.getClassSimpleName
import org.digma.intellij.plugin.idea.navigation.JvmEndpointNavigationProvider
import org.digma.intellij.plugin.idea.navigation.JvmSpanNavigationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.discovery.TextRange
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType
import kotlin.coroutines.coroutineContext

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractJvmLanguageService(protected val project: Project, protected val codeObjectDiscovery: AbstractCodeObjectDiscovery) :
    LanguageService, JvmLanguageService {

    protected val logger: Logger = thisLogger()

    companion object {
        val fileNamesToExclude = mutableSetOf("package-info.java", "MavenWrapperDownloader.java")
    }


    init {
        Log.trace(logger, project, "Initializing language service {}", javaClass)
    }

    override fun dispose() {

    }

    //It's a different search for each jvm language.
    abstract fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass?

    abstract fun findParentMethod(psiElement: PsiElement): UMethod?


    override fun isSupportedFile(virtualFile: VirtualFile): Boolean {
        return super.isSupportedFile(virtualFile) && !fileNamesToExclude.contains(virtualFile.name)
    }

    /**
     * Builds a DocumentInfo.
     * This method doesn't handle exceptions and may throw any kind of exception
     */
    override suspend fun buildDocumentInfo(virtualFile: VirtualFile): DocumentInfo? {

        Log.trace(logger, project, "buildDocumentInfo called {}", virtualFile)
        /*
            Important notice:
            building document info may be a long operation for large files.
            Holding read access for the whole operation may cause short freezes in the UI if the user is typing.
            Holding cancelable read access for the whole operation may cause many cancellations and eventually may
            fail to build the document info.
            So the strategy here is to hold read access for short periods, only for code that really needs read access,
            that way the chance for freezes or too many cancellations is minimized.
         */

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
                codeObjectDiscovery.buildDocumentInfo(project, it, virtualFile.url, getLanguage())
            }
        }
    }

    override fun isCodeVisionSupported(): Boolean {
        return true
    }


    override suspend fun getLanguageForMethodCodeObjectId(methodId: String): Language? {

        //try to parse the methodId as if it is java and try to find the language
        if (methodId.indexOf("\$_$") <= 0) {
            Log.trace(logger, project, "method id in getLanguageForMethodCodeObjectId does not contain \$_$ {}", methodId)
            return null
        }

        var className = methodId.substring(0, methodId.indexOf("\$_$"))
        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.')

        return readAction {
            val uClass = findClassByClassName(className, GlobalSearchScope.allScope(project))
            uClass?.let {
                it.sourcePsi?.language
            }
        }
    }

    override suspend fun getLanguageForClass(className: String): Language? {

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        val classNameToFind = className.replace('$', '.')

        return readAction {
            val uClass = findClassByClassName(classNameToFind, GlobalSearchScope.allScope(project))
            uClass?.let {
                it.sourcePsi?.language
            }
        }
    }


    override suspend fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        return JvmSpanNavigationProvider.Companion.getInstance(project).getUrisForSpanIds(spanIds)
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointLocation> {
        return JvmEndpointNavigationProvider.Companion.getInstance(project).getEndpointInfos(endpointId)
    }


    override suspend fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        val workspaceUrls: MutableMap<String, Pair<String, Int>> = HashMap()
        methodCodeObjectIds.forEach { methodId: String ->
            val psiMethod = findMethodPsiElementByMethodId(methodId)
            psiMethod?.let {
                readAction {
                    it.containingFile?.virtualFile?.let { virtualFile ->
                        workspaceUrls[methodId] = Pair(virtualFile.url, it.textOffset)
                    }
                }
            }
        }
        return workspaceUrls
    }


    override suspend fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {
        return JvmSpanNavigationProvider.Companion.getInstance(project).getMethodIdBySpanId(spanCodeObjectId)
    }


    override suspend fun findUMethodByMethodCodeObjectId(methodCodeObjectId: String?): UMethod? {
        return methodCodeObjectId?.takeIf { it.contains("\$_$") }?.let { methodId ->
            val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)
            readAction {
                val uClass = findClassByClassName(methodAndClass.first, GlobalSearchScope.projectScope(project))
                uClass?.let {
                    findMethodInClass(it, methodId)
                }
            }
        }
    }


    override suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String> {

        val workspaceUrls: MutableMap<String, String> = HashMap()

        methodCodeObjectIds.filter { s: String -> s.contains("\$_$") }.forEach { methodId ->
            var className = methodId.substring(0, methodId.indexOf("\$_$"))
            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            className = className.replace('$', '.')
            readAction {
                val uClass = findClassByClassName(className, GlobalSearchScope.projectScope(project))
                uClass?.let {
                    val psiFile = it.getContainingUFile()?.sourcePsi
                    psiFile?.let {
                        val url = PsiUtils.psiFileToUri(psiFile)
                        workspaceUrls[methodId] = url
                    }
                }
            }
        }

        return workspaceUrls
    }


    override suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret {

        Log.trace(logger, project, "detectMethodUnderCaret called {}", virtualFile)

        if (isSupportedFile(virtualFile).not()) {
            return MethodUnderCaret.Companion.empty(virtualFile.url)
        }

        if (!isValidVirtualFile(virtualFile)) {
            return MethodUnderCaret.EMPTY
        }

        return smartReadAction(project) {
            return@smartReadAction detectMethodUnderCaretImpl(virtualFile, caretOffset)
        }
    }


    @RequiresReadLock
    private fun detectMethodUnderCaretImpl(virtualFile: VirtualFile, caretOffset: Int): MethodUnderCaret {

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return MethodUnderCaret.empty(virtualFile.url)
        val packageName = psiFile.toUElementOfType<UFile>()?.packageName ?: ""
        val underCaret: PsiElement = psiFile.findElementAt(caretOffset) ?: return MethodUnderCaret.empty(virtualFile.url)
        val uMethod = findParentMethod(underCaret)
        val className: String = uMethod?.getParentOfType<UClass>()?.let {
            getClassSimpleName(it)
        } ?: ""

        if (uMethod != null) {

            val methodId = createMethodCodeObjectId(uMethod)
            val endpointTextRange = findEndpointTextRange(virtualFile, caretOffset, methodId)

            return MethodUnderCaret(
                methodId,
                uMethod.name,
                className,
                packageName,
                virtualFile.url,
                caretOffset,
                endpointTextRange
            )
        }
        return MethodUnderCaret.Companion.empty(virtualFile.url)
    }


    private fun findEndpointTextRange(virtualFile: VirtualFile, caretOffset: Int, methodId: String): TextRange? {
        val documentInfo = DocumentInfoStorage.Companion.getInstance(project).getDocumentInfo(virtualFile)
        if (documentInfo != null) {
            val methodInfo = documentInfo.methods[methodId]
            if (methodInfo != null) {
                val endpointInfo = methodInfo.endpoints.firstOrNull { endpointInfo: EndpointInfo ->
                    endpointInfo.textRange.contains(caretOffset)
                }

                if (endpointInfo != null) {
                    return endpointInfo.textRange
                }
            }
        }
        return null
    }


    override suspend fun navigateToMethod(methodId: String) {

        Log.trace(logger, project, "navigateToMethod called {}", methodId)

        if (methodId.indexOf("\$_$") <= 0) {
            Log.trace(logger, project, "method id in navigateToMethod does not contain \$_$, can not navigate {}", methodId)
            return
        }

        var className = methodId.substring(0, methodId.indexOf("\$_$"))
        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.')
        val method = readAction {
            val cls = findClassByClassName(className, GlobalSearchScope.allScope(project))
            cls?.let {
                val method = findMethodInClass(it, methodId)
                if (method?.sourcePsi is Navigatable && (method.sourcePsi as Navigatable).canNavigate()) {
                    Log.trace(logger, project, "can navigate to method {}", method)
                    method
                } else {
                    Log.trace(logger, project, "can not navigate to method {}", method)
                    null
                }
            }
        }
        method?.let {
            withContext(Dispatchers.EDT) {
                Log.trace(logger, project, "navigating to method {}", method)
                (it.sourcePsi as Navigatable).navigate(true)
            }
        }
    }


    override suspend fun findMethodPsiElementByMethodId(methodId: String): PsiElement? {

        Log.trace(logger, project, "findMethodPsiElementByMethodId called {}", methodId)

        if (methodId.indexOf("\$_$") <= 0) {
            Log.trace(logger, project, "method id in findMethodPsiElementByMethodId does not contain \$_$, can not find psi element {}", methodId)
            return null
        }

        val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)
        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        val className = methodAndClass.first.replace('$', '.')

        return readAction {
            val cls = findClassByClassName(className, GlobalSearchScope.allScope(project))
            cls?.let {
                val method = findMethodInClass(it, methodId)
                method?.sourcePsi
            }
        }
    }

    override suspend fun findClassPsiElementByMethodId(methodId: String): PsiElement? {

        Log.trace(logger, project, "findClassPsiElementByMethodId called {}", methodId)

        if (methodId.indexOf("\$_$") <= 0) {
            Log.trace(logger, project, "method id in getClassPsiElementByMethodId does not contain \$_$, can not navigate {}", methodId)
            return null
        }

        val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)
        return findClassPsiElementByClassName(methodAndClass.first)
    }


    override suspend fun findClassPsiElementByClassName(className: String): PsiElement? {

        Log.trace(logger, project, "findClassPsiElementByClassName called {}", className)

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        val classNameToFind = className.replace('$', '.')
        return readAction {
            val uClass = findClassByClassName(classNameToFind, GlobalSearchScope.allScope(project))
            uClass?.sourcePsi
        }
    }


}