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
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.environment.Env

interface LanguageService : Disposable {

    fun getLanguage(): Language
    fun getFileType(): FileType

    fun isSupportedFile(virtualFile: VirtualFile): Boolean {
        val languageFileType = virtualFile.fileType as? LanguageFileType ?: return false
        return languageFileType.language.isKindOf(getLanguage())
    }

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
    fun getLanguageForMethodCodeObjectId(methodId: String): Language?

    fun getLanguageForClass(className: String): Language?

    fun isSupportedFile(project: Project, newFile: VirtualFile): Boolean

    fun isSupportedFile(psiFile: PsiFile): Boolean

    fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {
        return null
    }

    /**
     * This method is called from the function list preview tab panel and is meant to navigate
     * to a method of the current opened file. It will not navigate to any method in the project.
     *
     * @param methodId the method id to navigate to
     */
    fun navigateToMethod(methodId: String)

    fun isServiceFor(language: Language): Boolean

    fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String>

    fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>>

    fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>>

    fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointInfo>

    /**
     * let language services do something on environmentChanged. for example, to update the current method context.
     */
    fun environmentChanged(newEnv: Env) {
        //nothing to do, implement for specific languages if necessary
    }


    fun isRelevant(file: VirtualFile): Boolean

    fun isRelevant(psiFile: PsiFile): Boolean

    fun isCodeVisionSupported(): Boolean

    fun canInstrumentMethod(methodId: String): MethodObservabilityInfo {
        return MethodObservabilityInfo(
            methodId,
            hasMissingDependency = false,
            canInstrumentMethod = false,
            annotationClassFqn = null,
            hasAnnotation = false
        )
    }

    fun instrumentMethod(methodObservabilityInfo: MethodObservabilityInfo): Boolean {
        return false
    }

    fun addDependencyToOtelLib(methodId: String) {
        //only relevant for jvm languages
        //todo: maybe throw non supported operation ?
    }

    fun getPsiElementForMethod(methodId: String): PsiElement?

    fun getPsiElementForClassByMethodId(methodId: String): PsiElement?

    fun getPsiElementForClassByName(className: String): PsiElement?

    fun getInstrumentationProvider(): InstrumentationProvider

    fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: List<String>): Map<String, PsiElement>

}