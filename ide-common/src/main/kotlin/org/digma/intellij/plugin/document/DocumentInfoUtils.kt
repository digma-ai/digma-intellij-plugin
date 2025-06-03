package org.digma.intellij.plugin.document

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.model.discovery.MethodInfo


fun findMethodInfo(project: Project, methodId: String): MethodInfo? {
    return DocumentInfoStorage.getInstance(project).allDocumentInfos()
        .firstOrNull { it.methods.containsKey(methodId) }?.methods?.get(methodId) //older kotlin compilers complain about methods[methodId]
}


fun findMethodInfo(project: Project, file: VirtualFile, methodId: String): MethodInfo? {
    return DocumentInfoStorage.getInstance(project).getDocumentInfo(file)?.methods?.get(methodId)
}

fun getLanguageByMethodCodeObjectId(project: Project, methodId: String): Language? {
    val id = DocumentInfoStorage.getInstance(project).allDocumentInfos()
        .firstOrNull { it.methods.containsKey(methodId) }?.languageId
    return id?.let {
        Language.findLanguageByID(it)
    }
}

fun getDominantLanguage(project: Project): Language? =
    DocumentInfoStorage.getInstance(project).allDocumentInfos()
        .map { it.languageId }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }?.key?.let { Language.findLanguageByID(it) }