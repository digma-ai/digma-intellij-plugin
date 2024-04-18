package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.model.rest.environment.Env
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetEnvironmentMessage(val payload: SetEnvironmentMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_ENVIRONMENT
}

data class SetEnvironmentMessagePayload(val environment: Env)
