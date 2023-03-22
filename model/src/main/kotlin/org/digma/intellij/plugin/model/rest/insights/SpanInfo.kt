package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

// SpanInfo attached to SpanInsight
@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "instrumentationLibrary",
        "name",
        "spanCodeObjectId",
        "displayName",
        "methodCodeObjectId",
        "kind",
)
constructor(
        val instrumentationLibrary: String,
        val name: String,
        val spanCodeObjectId: String?, //TODO: should be not null, somehow the backend returns nulls on endpoints
        val displayName: String,
        val methodCodeObjectId: String?,
        val kind: String?,
)
