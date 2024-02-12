package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils


data class IsObservabilityEnabledMessage(val payload: IsObservabilityEnabledPayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_IS_OBSERVABILITY_ENABLED
}

data class IsObservabilityEnabledPayload(
    @get:JsonProperty("isObservabilityEnabled")
    @param:JsonProperty("isObservabilityEnabled")
    val isObservabilityEnabled: Boolean,
)

