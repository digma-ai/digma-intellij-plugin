package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils


data class IsJaegerButtonEnabledMessage(val payload: IsJaegerButtonEnabledMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_IS_JAEGER_ENABLED
}

data class IsJaegerButtonEnabledMessagePayload(
    @get:JsonProperty("isJaegerEnabled")
    @param:JsonProperty("isJaegerEnabled")
    val isJaegerEnabled: Boolean,
)

