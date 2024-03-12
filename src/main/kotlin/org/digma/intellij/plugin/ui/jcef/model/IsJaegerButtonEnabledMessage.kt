package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants


data class IsJaegerButtonEnabledMessage(val payload: IsJaegerButtonEnabledMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_IS_JAEGER_ENABLED
}

data class IsJaegerButtonEnabledMessagePayload(
    @get:JsonProperty("isJaegerEnabled")
    @param:JsonProperty("isJaegerEnabled")
    val isJaegerEnabled: Boolean,
)

