package org.digma.intellij.plugin.ui.model

import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo


interface Scope{
    fun getScope():String
}

class EmptyScope(val text:String): Scope{
    override fun getScope(): String {
        return text
    }

}

class MethodScope(val methodInfo: MethodInfo): Scope{
    override fun getScope(): String {
        return "${methodInfo.containingClass}.${methodInfo.name}"
    }

}

class DocumentScope(val documentInfo: DocumentInfo): Scope{
    override fun getScope(): String {
        return "${documentInfo.path.substringAfterLast('/')}"
    }

}