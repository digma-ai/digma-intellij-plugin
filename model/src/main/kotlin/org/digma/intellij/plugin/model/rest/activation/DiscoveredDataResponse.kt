package org.digma.intellij.plugin.model.rest.activation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiscoveredDataResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "assetFound",
    "issueFound",
    "recentActivityFound"
)
constructor(
    val assetFound: Boolean,
    val issueFound: Boolean,
    val recentActivityFound: Boolean,
)