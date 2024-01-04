package org.digma.intellij.plugin.insights.model.outgoing

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


data class CommitInfo(val commit: String, val url: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SetCommitInfoData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("commitInfos")
constructor(
    val commitInfos: Map<String, CommitInfo>,
)

data class SetCommitInfoMessage(val type: String = "digma", val action: String = "INSIGHTS/SET_COMMIT_INFO", val payload: SetCommitInfoData)
