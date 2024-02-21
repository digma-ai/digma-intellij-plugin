package org.digma.intellij.plugin.ui.navigation.model

import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.navigation.View

data class SetViewMessage(val payload: SetViewMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = "NAVIGATION/SET_VIEWS"
}

data class SetViewMessagePayload(val views: List<View>, val isTriggeredByJcef: Boolean)

