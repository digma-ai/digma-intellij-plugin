package org.digma.intellij.plugin.ui.model

import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo

const val NOT_SUPPORTED_OBJECT_MSG = "Non supported object"

interface Scope{
    fun getScope():String
    fun getScopeTooltip():String
}

class EmptyScope(val text:String = ""): Scope{
    override fun getScope(): String {
        return text
    }

    override fun getScopeTooltip(): String {
        return getScope()
    }

}

class MethodScope(private val methodInfo: MethodInfo) : Scope {
    override fun getScope(): String {
        return "${methodInfo.containingClass}.${methodInfo.name}"
    }

    override fun getScopeTooltip(): String {
        return "${methodInfo.containingClass}.${methodInfo.nameWithParams()}"
    }
}

class DocumentScope(private val documentInfo: DocumentInfo) : Scope {
    override fun getScope(): String {
        return documentInfo.path.substringAfterLast('/')
    }

    override fun getScopeTooltip(): String {
        return getScope()
    }
}