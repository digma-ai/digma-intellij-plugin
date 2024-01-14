package org.digma.intellij.plugin.ui.tests.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessagePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEnvironment
import java.beans.ConstructorProperties

data class SetEnvironmentsMessage@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
    val type: String,
    val action: String,
    val payload: SetEnvironmentsMessagePayload,
)

data class SetEnvironmentsMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("environments")
constructor(val environments: List<EnvironmentEntity>)

data class EnvironmentEntity
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("name", "originalName")
constructor(
    val name: String,
    val originalName: String,
)
