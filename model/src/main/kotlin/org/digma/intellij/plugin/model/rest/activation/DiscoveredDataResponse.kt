package org.digma.intellij.plugin.model.rest.activation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiscoveredDataResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "assetFound",
    "insightFound",
    "issueFound",
    "importantIssueFound",
    "recentActivityFound"
)
constructor(
    val assetFound: Boolean,
    val insightFound: Boolean,
    val issueFound: Boolean,
    val importantIssueFound: Boolean,
    val recentActivityFound: Boolean,
)