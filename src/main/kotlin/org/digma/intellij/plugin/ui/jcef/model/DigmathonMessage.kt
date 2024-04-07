package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetDigmathonState(
    val payload: DigmathonStatePayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_DIGMATHON_MODE
}

data class DigmathonStatePayload(
    @get:JsonProperty("isDigmathonModeEnabled")
    @param:JsonProperty("isDigmathonModeEnabled")
    val isDigmathonModeEnabled: Boolean,
)

data class SetDigmathonProductKey(
    val payload: DigmathonProductKeyPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_DIGMATHON_PRODUCT_KEY
}

data class DigmathonProductKeyPayload(
    val productKey: String?,
)