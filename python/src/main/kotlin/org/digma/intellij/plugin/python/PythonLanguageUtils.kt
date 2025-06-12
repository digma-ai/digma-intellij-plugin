@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.python

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.python.psi.PyFunction
import org.digma.intellij.plugin.python.discovery.SITE_PACKAGES_DIR_NAME

@RequiresReadLock(generateAssertion = false)
fun createPythonMethodCodeObjectId(project: Project, pyFunction: PyFunction): String {
    val name = pyFunction.name ?: pyFunction.qualifiedName ?: ""
    val filePath = buildFilePath(project, pyFunction)
//    return "${filePath.substringBefore(".py")}\$_$$name"
    return "$filePath\$_$$name"
}

@RequiresReadLock(generateAssertion = false)
private fun buildFilePath(project: Project, pyFunction: PyFunction): String {
    return getRelativePath(project, pyFunction.containingFile)
}

@RequiresReadLock(generateAssertion = false)
fun getRelativePath(project: Project, pyFile: PsiFile): String {
    val path = pyFile.virtualFile.toNioPath()
    val pathElements = path.map { it.toString() }
    if (pathElements.contains(SITE_PACKAGES_DIR_NAME)) {
        return pyFile.virtualFile.toNioPath().subpath(pathElements.indexOf(SITE_PACKAGES_DIR_NAME), pathElements.size).toString()
    }
    if (pathElements.contains(project.name)) {
        return pyFile.virtualFile.toNioPath().subpath(pathElements.indexOf(project.name), pathElements.size).toString()
    }
    return pyFile.virtualFile.toNioPath().toString()
}


fun createSpanId(instLibrary: String, spanName: String): String {
    return "$instLibrary\$_$$spanName"
}

fun getPythonMethodNameFromMethodId(methodId: String): String {
    return methodId.substringAfter("\$_$")
}

fun isPythonMethodId(methodId: String): Boolean {
    return methodId.contains("\$_$")
}