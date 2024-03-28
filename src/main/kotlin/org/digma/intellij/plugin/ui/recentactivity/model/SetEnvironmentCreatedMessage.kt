package org.digma.intellij.plugin.ui.recentactivity.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetEnvironmentCreatedMessage(val payload: String) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "RECENT_ACTIVITY/ENVIRONMENT_CREATED"
}