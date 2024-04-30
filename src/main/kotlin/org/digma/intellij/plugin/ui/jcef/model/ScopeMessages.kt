package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants


data class SetScopeMessage(val payload: SetScopeMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_SCOPE
}

data class SetScopeMessagePayload(
    val span: SpanScope?,
    val code: CodeLocation,
    val hasErrors: Boolean,
    val analyticsInsightsCount: Number,
    val issuesInsightsCount: Number,
    val unreadInsightsCount: Number
)

