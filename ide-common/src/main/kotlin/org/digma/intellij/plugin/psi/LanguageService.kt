package org.digma.intellij.plugin.psi

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.discovery.FileDiscoveryProvider
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret

interface LanguageService : Disposable {

    fun getLanguage(): Language
    fun getFileType(): FileType

    fun isSupportedFile(virtualFile: VirtualFile): Boolean {
        val languageFileType = virtualFile.fileType as? LanguageFileType ?: return false
        return languageFileType.language.isKindOf(getLanguage())
    }

    fun isSupportedFile(psiFile: PsiFile): Boolean

    //some language services need the editor. for example, CSharpLanguageService needs to take
    // getProjectModelId from the selected editor, which is the preferred way to find the IPsiSourceFile in resharper.
    // it's not possible to invoke getProjectModelId in regular intellij code because it's an extension method available only in Rider.
    suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret

    suspend fun buildDocumentInfo(virtualFile: VirtualFile): DocumentInfo?

    /**
     * This method should be a last resort to find language as it is slow and not reliable.
     * Try to find the language by method code object id.
     * Each language service should implement it differently and may return null.
     * Each language service can only check if this method's language is the language it supports.
     * So actually, the language service can only return its supported language or null.
     */
    suspend fun getLanguageForMethodCodeObjectId(methodId: String): Language?

    suspend fun getLanguageForClass(className: String): Language?

    suspend fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String?

    suspend fun navigateToMethod(methodId: String)

    suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String>

    suspend fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>>

    suspend fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>>

    fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointLocation>

    fun isCodeVisionSupported(): Boolean

    suspend fun findMethodPsiElementByMethodId(methodId: String): PsiElement?

    suspend fun findClassPsiElementByMethodId(methodId: String): PsiElement?

    suspend fun findClassPsiElementByClassName(className: String): PsiElement?

    fun getInstrumentationProvider(): InstrumentationProvider

    //todo: this method is called from CodeLensService.getCodeLens , its not trivial to run it in coroutine
    fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: List<String>): Map<String, PsiElement>

    fun getDiscoveryProvider(): FileDiscoveryProvider {
        //currently relevant for jvm language services and python language service.
        //should never be called for other languages.
        throw UnsupportedOperationException("getDiscoveryProvider is not implemented for ${this.javaClass.name}")
    }

}