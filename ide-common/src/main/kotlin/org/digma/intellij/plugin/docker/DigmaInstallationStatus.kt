package org.digma.intellij.plugin.docker

import com.fasterxml.jackson.annotation.JsonProperty


enum class DigmaInstallationType {
    localEngine, dockerCompose, dockerDesktop, remote
}


data class DigmaInstallationStatus(
    @get:JsonProperty("isRunning")
    @param:JsonProperty("isRunning")
    val isRunning: Boolean,
    val type: DigmaInstallationType?,
)
