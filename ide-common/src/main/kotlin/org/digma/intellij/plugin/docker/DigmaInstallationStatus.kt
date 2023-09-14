package org.digma.intellij.plugin.docker


enum class DigmaInstallationType {
    localEngine, dockerCompose, dockerDesktop
}


data class DigmaInstallationStatus(
    val connection: ConnectionStatus,
    val runningDigmaInstances: List<DigmaInstallationType>,
)

enum class ConnectionType { local, remote }

data class ConnectionStatus(val type: ConnectionType?, val status: Boolean)