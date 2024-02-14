package org.digma.intellij.plugin.ui.navigation.model

import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils


data class AutoFixResultMessage(val payload: InstrumentationResultPayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "NAVIGATION/SET_AUTOFIX_MISSING_DEPENDENCY_RESULT"
}

data class AddAnnotationResultMessage(val payload: InstrumentationResultPayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "NAVIGATION/SET_ADD_ANNOTATION_RESULT"
}


data class InstrumentationResultPayload(val result: InstrumentationResult, val error: String? = null)

@Suppress("EnumEntryName")
enum class InstrumentationResult { success, failure }
