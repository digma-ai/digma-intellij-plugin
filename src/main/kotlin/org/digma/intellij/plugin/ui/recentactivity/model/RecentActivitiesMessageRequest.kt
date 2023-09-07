package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
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
@ConstructorProperties("environments", "entries")
constructor(val environments: List<RecentActivityEnvironment>, val entries: List<RecentActivityResponseEntry>)


data class RecentActivityEnvironment
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("name", "isPending")
constructor(
    val name: String,
    @get:JsonProperty("isPending")
    @param:JsonProperty("isPending")
    val isPending: Boolean,
)