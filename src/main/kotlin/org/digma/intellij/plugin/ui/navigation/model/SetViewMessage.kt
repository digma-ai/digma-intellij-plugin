package org.digma.intellij.plugin.ui.navigation.model

import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetViewMessage(val action: String, val payload: SetViewMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
}

data class SetViewMessagePayload(val views: List<View>, val isTriggeredByJcef: Boolean)

