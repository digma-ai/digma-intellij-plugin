package org.digma.intellij.plugin.ui.recentactivity.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class OpenRegistrationDialogMessage(val payload: OpenRegistrationDialogPayload = OpenRegistrationDialogPayload()) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "RECENT_ACTIVITY/OPEN_REGISTRATION_DIALOG"
}

class OpenRegistrationDialogPayload