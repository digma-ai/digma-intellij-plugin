package org.digma.intellij.plugin.python

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.psi.LanguageService

class PythonLanguageService: LanguageService, DisposableAdaptor {
    override fun getLanguage(): Language {
        TODO("Not yet implemented")
    }

    override fun getFileType(): FileType {
        TODO("Not yet implemented")
    }

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun detectMethodUnderCaret(
        virtualFile: VirtualFile,
        editor: Editor,
        caretOffset: Int
    ): org.digma.intellij.plugin.model.discovery.MethodUnderCaret {
        TODO("Not yet implemented")
    }

    override suspend fun buildDocumentInfo(virtualFile: VirtualFile): org.digma.intellij.plugin.model.discovery.DocumentInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getLanguageForMethodCodeObjectId(methodId: String): Language? {
        TODO("Not yet implemented")
    }

    override suspend fun getLanguageForClass(className: String): Language? {
        TODO("Not yet implemented")
    }

    override suspend fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun navigateToMethod(methodId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String> {
        TODO("Not yet implemented")
    }

    override suspend fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        TODO("Not yet implemented")
    }

    override suspend fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        TODO("Not yet implemented")
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointLocation> {
        TODO("Not yet implemented")
    }

    override fun isCodeVisionSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun findMethodPsiElementByMethodId(methodId: String): PsiElement? {
        TODO("Not yet implemented")
    }

    override suspend fun findClassPsiElementByMethodId(methodId: String): PsiElement? {
        TODO("Not yet implemented")
    }

    override suspend fun findClassPsiElementByClassName(className: String): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun getInstrumentationProvider(): InstrumentationProvider {
        TODO("Not yet implemented")
    }

    override fun findMethodsByCodeObjectIds(
        psiFile: PsiFile,
        methodIds: List<String>
    ): Map<String, PsiElement> {
        TODO("Not yet implemented")
    }
}