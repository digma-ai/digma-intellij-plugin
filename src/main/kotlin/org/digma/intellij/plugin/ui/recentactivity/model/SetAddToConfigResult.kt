package org.digma.intellij.plugin.ui.recentactivity.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class AddToConfigData(val environment: String, val result: AdditionToConfigResult );

data class SetAddToConfigResult(val payload: AddToConfigData) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "RECENT_ACTIVITY/ADD_ENVIRONMENT_TO_RUN_CONFIG_RESULT"
}