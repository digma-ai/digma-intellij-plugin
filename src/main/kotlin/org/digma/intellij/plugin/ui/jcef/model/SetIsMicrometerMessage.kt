package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils

data class SetIsMicrometerMessage(
    val payload: IsMicrometerPayload,
) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_IS_MICROMETER_PROJECT
}

data class IsMicrometerPayload(
    @get:JsonProperty("isMicrometerProject")
    @param:JsonProperty("isMicrometerProject")
    val isMicrometerProject: Boolean,
)