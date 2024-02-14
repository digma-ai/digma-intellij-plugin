package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.model.code.CodeDetails
import org.digma.intellij.plugin.scope.SpanScope


data class SetScopeMessage(val payload: SetScopeMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_SCOPE
}

data class SetScopeMessagePayload(
    val span: SpanScope?,
    val code: CodeLocation,
)

data class CodeLocation(
    @get:JsonProperty("isAlreadyAtCode")
    @param:JsonProperty("isAlreadyAtCode")
    val isAlreadyAtCode: Boolean,
    val codeDetailsList: List<CodeDetails> = listOf(),
    val relatedCodeDetailsList: List<CodeDetails> = listOf(),
)

