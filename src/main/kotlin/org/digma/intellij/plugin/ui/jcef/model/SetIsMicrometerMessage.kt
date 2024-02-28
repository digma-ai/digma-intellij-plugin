package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetIsMicrometerMessage(
    val payload: IsMicrometerPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_IS_MICROMETER_PROJECT
}

data class IsMicrometerPayload(
    @get:JsonProperty("isMicrometerProject")
    @param:JsonProperty("isMicrometerProject")
    val isMicrometerProject: Boolean,
)