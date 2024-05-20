package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants


data class ViewedInsight(val type: String, val foundAt: String)

data class SetDigmathonProgressData(val payload: DigmathonProgressDataPayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "RECENT_ACTIVITY/SET_DIGMATHON_PROGRESS_DATA"
}

data class DigmathonProgressDataPayload(
    val insights: Collection<ViewedInsight>,
    val lastUpdatedByUserAt: String?
)





data class SetDigmathonState(
    val payload: DigmathonStatePayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_DIGMATHON_MODE
}

data class DigmathonStatePayload(
    @get:JsonProperty("isDigmathonModeEnabled")
    @param:JsonProperty("isDigmathonModeEnabled")
    val isDigmathonModeEnabled: Boolean,
)

data class SetDigmathonProductKey(
    val payload: DigmathonProductKeyPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_DIGMATHON_PRODUCT_KEY
}

data class DigmathonProductKeyPayload(
    val productKey: String?,
)

data class SetUserFinishedDigmathon(
    val payload: UserFinishedDigmathonPayload,
) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_IS_DIGMATHON_GAME_FINISHED
}

data class UserFinishedDigmathonPayload(
    @get:JsonProperty("isDigmathonGameFinished")
    @param:JsonProperty("isDigmathonGameFinished")
    val isDigmathonGameFinished: Boolean,
)