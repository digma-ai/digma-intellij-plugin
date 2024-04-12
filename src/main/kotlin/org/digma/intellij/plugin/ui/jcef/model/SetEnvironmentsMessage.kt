package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.model.rest.environment.Env
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetEnvironmentsMessage(val payload: SetEnvironmentsMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_ENVIRONMENTS
}

data class SetEnvironmentsMessagePayload(val environments: List<Env>)
