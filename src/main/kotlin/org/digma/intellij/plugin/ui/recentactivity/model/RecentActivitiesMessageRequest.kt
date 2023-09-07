package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import java.beans.ConstructorProperties


data class RecentActivitiesMessageRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String,
        val action: String,
        val payload: RecentActivitiesMessagePayload,
)


data class RecentActivitiesMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environments: List<String>, val entries: List<RecentActivityResponseEntry>)