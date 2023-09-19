package org.digma.intellij.plugin.ui.recentactivity.model

import org.digma.intellij.plugin.analytics.ConnectionTestResult

data class ConnectionTestResultMessage(val type: String, val action: String, val payload: ConnectionTestResult)