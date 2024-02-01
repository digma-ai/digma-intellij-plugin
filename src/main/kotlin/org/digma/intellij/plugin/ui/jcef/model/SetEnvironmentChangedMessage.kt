package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils

data class SetEnvironmentMessage(val payload: SetEnvironmentMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_ENVIRONMENT
}

data class SetEnvironmentMessagePayload(val environment: String)
