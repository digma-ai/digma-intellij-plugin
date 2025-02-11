package org.digma.intellij.plugin.model.rest.insights.issues

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonRawValue
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GetIssuesRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "environment",
    "scopedSpanCodeObjectId",
    "displayName",
    "showDismissed",
    "filters",
    "sortBy",
    "sortOrder",
    "insightTypes",
    "services",
    "criticalityFilter",
    "page"
)
constructor(
    var environment: String?,
    val scopedSpanCodeObjectId: String?,
    val displayName: String?,
    val showDismissed: Boolean,
    @JsonRawValue val filters: String,
    val sortBy: String,
    val sortOrder: String,
    @JsonRawValue val insightTypes: String?,
    @JsonRawValue val services: String?,
    @JsonRawValue val criticalityFilter: String?,
    val page: Number
)