package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MarkInsightsAsReadRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("insightIds")
constructor(val insightIds: List<String>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MarkAllInsightsAsReadRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("environment", "scope")
constructor(
    val environment: String,
    val scope: MarkInsightsAsReadScope?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MarkInsightsAsReadScope
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("spanCodeObject", "methodCodeObjectId", "serviceName", "role")
constructor(
    val spanCodeObject: String?,
    val methodCodeObjectId: String?,
    val serviceName: String?,
    val role: String?)