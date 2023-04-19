package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AffectedEndpointInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "route",
    "serviceName",
    "name",
    "instrumentationLibrary",
    "spanCodeObjectId",
    "displayName",
    "methodCodeObjectId",
    "kind",
    "codeObjectId",
    "sampleTraceId")
constructor(
    val route: String,
    val serviceName: String,
    override val name: String,
    override val instrumentationLibrary: String,
    override val spanCodeObjectId: String?,
    override val displayName: String,
    override val methodCodeObjectId: String?,
    override val kind: String?,
    val codeObjectId: String?,
    val sampleTraceId: String
) : SpanInfo (
    instrumentationLibrary,
    name,
    spanCodeObjectId,
    displayName,
    methodCodeObjectId,
    kind)