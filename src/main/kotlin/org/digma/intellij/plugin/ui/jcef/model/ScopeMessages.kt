package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.scope.SpanScope


data class SetScopeMessage(val payload: SetScopeMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_SCOPE
}

data class SetScopeMessagePayload(
    val span: SpanScope?,
    val code: CodeLocation,
)

