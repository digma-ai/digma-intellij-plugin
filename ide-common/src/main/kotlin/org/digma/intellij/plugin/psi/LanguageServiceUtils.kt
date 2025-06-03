package org.digma.intellij.plugin.psi

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.document.findMethodInfo
import org.digma.intellij.plugin.document.getDominantLanguage
import org.digma.intellij.plugin.document.getLanguageByMethodCodeObjectId
import org.digma.intellij.plugin.model.discovery.MethodInfo

//needs to be a fast method. we could just check the file extension, but it's not as reliable for the long term as checking the language.
//if this method becomes a bottleneck, we can change it to check the file extension strings.
//check if the file is of any language supported by the plugin.
fun isSupportedLanguageFile(project: Project, virtualFile: VirtualFile): Boolean {
    val fileType = virtualFile.fileType
    if (fileType !is LanguageFileType) return false

    val supportedFileTypes = LanguageServiceProvider.getInstance(project).getFileTypes()
    return supportedFileTypes.any { it.name == fileType.name }

//            val language = fileType.language
//            val targetLanguages = LanguageServiceProvider.getInstance(project).getLanguages()
//            return targetLanguages.any { language.isKindOf(it) }
}

fun isSupportedLanguageFile(project: Project, psiFile: PsiFile): Boolean {
    return LanguageServiceProvider.getInstance(project).getLanguages().any { psiFile.language == it }
}

suspend fun findLanguageServiceByPredicate(project: Project, predicate: suspend (LanguageService) -> Boolean): LanguageService? {
    for (service in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
        if (predicate(service)) {
            return service
        }
    }
    return null
}

suspend fun findLanguageByPredicate(project: Project, predicate: suspend (LanguageService) -> Boolean): Language? {
    return findLanguageServiceByPredicate(project) { predicate(it) }?.getLanguage()
}

suspend fun findLanguageByMethodCodeObjectId(project: Project, methodId: String): Language? {
    return findLanguageByPredicate(project) { service: LanguageService -> service.getLanguageForMethodCodeObjectId(methodId) != null }
}

suspend fun findLanguageServiceByClassName(project: Project, className: String): LanguageService? {
    return findLanguageServiceByPredicate(project) { service: LanguageService -> service.getLanguageForClass(className) != null }
}

fun findLanguageServiceByFile(project: Project, virtualFile: VirtualFile): LanguageService? {
    return LanguageServiceProvider.getInstance(project).getLanguageService(virtualFile)
}

suspend fun findLanguageByClassName(project: Project, className: String): Language? {
    return findLanguageByPredicate(project) { service: LanguageService -> service.getLanguageForClass(className) != null }
}

//we don't know the language, so we try all language services and return the first non-empty result
//can also find the language by findLanguageByMethodCodeObjectId, but it will be a longer operation because it requires PSI queries
suspend fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(project: Project, methodCodeObjectIds: List<String>): Map<String, String> {
    for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
        val workspaceUris = languageService.findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds)
        if (workspaceUris.isNotEmpty()) {
            return workspaceUris
        }
    }
    return emptyMap()
}

fun getEndpointInfos(project: Project, endpointCodeObjectId: String): Set<EndpointLocation> {
    for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
        val endpointLocations = languageService.lookForDiscoveredEndpoints(endpointCodeObjectId)
        if (endpointLocations.isNotEmpty()) {
            return endpointLocations
        }
    }
    return emptySet()
}

suspend fun findMethodPsiElementByMethodId(project: Project, methodCodeObjectId: String): PsiElement? {
    for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
        val psiElement = languageService.findMethodPsiElementByMethodId(methodCodeObjectId)
        if (psiElement != null) {
            return psiElement
        }
    }
    return null
}

suspend fun findClassPsiElementByMethodId(project: Project, methodCodeObjectId: String): PsiElement? {
    for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
        val psiElement = languageService.findClassPsiElementByMethodId(methodCodeObjectId)
        if (psiElement != null) {
            return psiElement
        }
    }
    return null
}

suspend fun findClassPsiElementByClassName(project: Project, className: String): PsiElement? {
    for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
        val psiElement = languageService.findClassPsiElementByClassName(className)
        if (psiElement != null) {
            return psiElement
        }
    }
    return null
}

suspend fun findLanguageServiceByMethodCodeObjectId(project: Project, methodCodeObjectId: String?): LanguageService? {
    //first try to find a methodInfo, it will exist in documentInfoService if the document is opened in the editor.
    //it's the easiest way because documentInfoService has the information of which language this MethodInfo is.
    //if the document is not opened, MethodInfo will be null.
    //if getErrorDetails is called from error insight, then the document is opened for sure and MethodInfo will be found.

    if (methodCodeObjectId == null) {
        return null
    }

    val methodInfo: MethodInfo? = findMethodInfo(project, methodCodeObjectId)

    val language = if (methodInfo == null) {
        findLanguageByMethodCodeObjectId(project, methodCodeObjectId) ?: getDominantLanguage(project)
    } else {
        getLanguageByMethodCodeObjectId(project, methodInfo.id)
    }

    return language?.let { LanguageServiceProvider.getInstance(project).getLanguageService(it) }
}
