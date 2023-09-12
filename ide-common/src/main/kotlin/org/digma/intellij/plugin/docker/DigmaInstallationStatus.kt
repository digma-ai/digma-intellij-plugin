package org.digma.intellij.plugin.docker

import com.fasterxml.jackson.annotation.JsonProperty


enum class DigmaInstallationType {
    LocalEngine, DockerCompose, DockerDesktop, Remote, Unknown
}


data class DigmaInstallationStatus(
    @get:JsonProperty("isRunning")
    @param:JsonProperty("isRunning")
    val isRunning: Boolean,
    val type: DigmaInstallationType,
)
