package org.digma.intellij.plugin.python

import org.digma.intellij.plugin.model.discovery.MethodInfo

class PythonMethodInfoDisplayNameProvider: MethodInfo.DisplayNameProvider {
    override fun provideDisplayName(methodInfo: MethodInfo): String {
        val methodName = methodInfo.id.substringAfter("\$_$")
        val filePath = methodInfo.id.substringBefore("\$_$")

        if (methodName == filePath) {
            return methodName
        }

        val lastPart = filePath.substringAfterLast("/")

        return lastPart.plus(".").plus(methodName)
    }
}