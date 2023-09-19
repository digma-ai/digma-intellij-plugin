package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.docker.DigmaInstallationStatus


data class DigmaEngineStatusMessage(val type: String, val action: String, val payload: DigmaInstallationStatus)
