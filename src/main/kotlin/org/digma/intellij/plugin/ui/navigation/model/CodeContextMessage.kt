package org.digma.intellij.plugin.ui.navigation.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants


data class CodeContextMessage(val payload: CodeContextMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "NAVIGATION/SET_CODE_CONTEXT"
}


data class CodeContextMessagePayload(
    val displayName: String?,
    val spans: CodeContextSpans,
    @get:JsonProperty("isInstrumented")
    @param:JsonProperty("isInstrumented")
    val isInstrumented: Boolean? = null,
    val methodId: String? = null,
    @get:JsonProperty("hasMissingDependency")
    @param:JsonProperty("hasMissingDependency")
    val hasMissingDependency: Boolean? = null,
    @get:JsonProperty("canInstrumentMethod")
    @param:JsonProperty("canInstrumentMethod")
    val canInstrumentMethod: Boolean? = null,
)

