package org.digma.intellij.plugin.docker


enum class DigmaInstallationType {
    localEngine, dockerCompose, dockerDesktop
}


data class DigmaInstallationStatus(
    val connection: ConnectionStatus,
    val runningDigmaInstances: List<DigmaInstallationType>,
) {

    companion object {
        val NOT_RUNNING = DigmaInstallationStatus(ConnectionStatus(null, false), listOf())
    }

}

enum class ConnectionType { local, remote }

data class ConnectionStatus(val type: ConnectionType?, val status: Boolean)