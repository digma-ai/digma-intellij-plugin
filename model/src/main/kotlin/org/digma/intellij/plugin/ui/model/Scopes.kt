package org.digma.intellij.plugin.ui.model

import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import org.jetbrains.annotations.Nullable

const val NOT_SUPPORTED_OBJECT_MSG = "Non supported object"

interface Scope {
    fun getScope(): String
    fun getScopeTooltip(): String

    @Nullable
    fun getFileUri(): String?
}

class EmptyScope(val text: String = "") : Scope {
    override fun getScope(): String {
        return text
    }

    override fun getScopeTooltip(): String {
        return getScope()
    }

    override fun getFileUri(): String? {
        return null
    }
}

class MethodScope(private val methodInfo: MethodInfo) : Scope {
    override fun getScope(): String {
        if (methodInfo.containingClass.isBlank()) {
            return methodInfo.name
        }
        return "${methodInfo.containingClass.substringAfterLast('.')}.${methodInfo.name}"
    }

    override fun getScopeTooltip(): String {
        if (methodInfo.containingClass.isBlank()) {
            return methodInfo.name
        }
        return "${methodInfo.containingClass}.${methodInfo.nameWithParams()}"
    }

    override fun getFileUri(): String? {
        if (methodInfo.containingFileUri.isBlank()) {
            return null
        }
        return methodInfo.containingFileUri
    }

    fun getMethodInfo(): MethodInfo {
        return methodInfo
    }
}


class CodeLessSpanScope(private val codeLessSpan: CodeLessSpan, private val spanInfo: SpanInfo?) : Scope {
    override fun getScope(): String {
        return spanInfo?.displayName ?: codeLessSpan.spanId.substringAfterLast("\$_$")
    }

    override fun getScopeTooltip(): String {
        return getScope()
    }

    override fun getFileUri(): String? {
        return null
    }

    fun getSpan(): CodeLessSpan {
        return codeLessSpan
    }
}


class DocumentScope(private val documentInfo: DocumentInfo) : Scope {
    override fun getScope(): String {
        return documentInfo.fileUri.substringAfterLast('/')
    }

    override fun getScopeTooltip(): String {
        return getScope()
    }

    override fun getFileUri(): String? {
        if (documentInfo.fileUri.isBlank()) {
            return null
        }
        return documentInfo.fileUri
    }

    fun getDocumentInfo(): DocumentInfo {
        return documentInfo
    }

}


class ErrorDetailsScope(private val name: String) : Scope {
    override fun getScope(): String {
        return name
    }

    override fun getScopeTooltip(): String {
        return name
    }

    override fun getFileUri(): String? {
        return null
    }

}
