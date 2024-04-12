package org.digma.intellij.plugin.ui.recentactivity.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetDigmathonProgressData(val payload: DigmathonProgressDataPayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "RECENT_ACTIVITY/SET_DIGMATHON_PROGRESS_DATA"
}

data class DigmathonProgressDataPayload(
    val insights: Collection<String>
)