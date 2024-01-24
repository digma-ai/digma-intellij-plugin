package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.common.EnvironmentEntity
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils

data class SetEnvironmentsMessage(val payload: SetEnvironmentsMessagePayload) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_ENVIRONMENTS
}

data class SetEnvironmentsMessagePayload(val environments: List<EnvironmentEntity>)
