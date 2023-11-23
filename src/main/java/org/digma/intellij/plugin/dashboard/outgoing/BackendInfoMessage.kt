package org.digma.intellij.plugin.dashboard.outgoing

import org.digma.intellij.plugin.model.rest.AboutResult

data class BackendInfoMessage(val type: String, val action: String, val payload: AboutResult)

