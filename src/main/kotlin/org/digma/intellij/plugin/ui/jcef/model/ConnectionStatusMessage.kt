package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty


data class ConnectionStatusMessage(val type: String, val action: String, val payload: IsDigmaRunningPayload)
data class IsDigmaRunningPayload(
    @get:JsonProperty("isDigmaRunning")
    @param:JsonProperty("isDigmaRunning")
    val isDigmaRunning: Boolean,
)