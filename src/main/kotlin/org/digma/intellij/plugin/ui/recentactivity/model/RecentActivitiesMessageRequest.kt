package org.digma.intellij.plugin.ui.recentactivity.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivitiesMessageRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
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
@ConstructorProperties("name", "originalName", "isPending", "additionToConfigResult", "type", "serverApiUrl", "token", "isOrgDigmaSetupFinished")
constructor(
    val name: String,
    val originalName: String,
    @get:JsonProperty("isPending")
    @param:JsonProperty("isPending")
    val isPending: Boolean,
    val additionToConfigResult: AdditionToConfigResult? = null,
    var type: EnvironmentType? = null,
    val serverApiUrl: String? = null,
    val token: String? = null,
    @get:JsonProperty("isOrgDigmaSetupFinished")
    @param:JsonProperty("isOrgDigmaSetupFinished")
    var isOrgDigmaSetupFinished: Boolean = false,
)


data class PendingEnvironment
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("name", "type", "additionToConfigResult", "serverApiUrl", "token", "isOrgDigmaSetupFinished")
constructor(
    val name: String,
    var type: EnvironmentType? = null,
    var additionToConfigResult: AdditionToConfigResult? = null,
    var serverApiUrl: String? = null,
    var token: String? = null,
    @get:JsonProperty("isOrgDigmaSetupFinished")
    @param:JsonProperty("isOrgDigmaSetupFinished")
    var isOrgDigmaSetupFinished: Boolean = false,
)

enum class AdditionToConfigResult { success, failure }
enum class EnvironmentType { local, shared }

