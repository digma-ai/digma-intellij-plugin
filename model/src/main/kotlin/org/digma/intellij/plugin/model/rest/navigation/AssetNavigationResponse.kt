package org.digma.intellij.plugin.model.rest.navigation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssetNavigationResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeLocation",
    "relatedCodeLocations"
)
constructor(val codeLocation: AssetCodeLocation, val relatedCodeLocations: List<AssetRelatedCodeLocation>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssetCodeLocation
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId",
    "methodCodeObjectId",
    "displayName",
    "endpointAssetCodeLocation"
)
constructor(
    val spanCodeObjectId: String,
    val methodCodeObjectId: String? = null,
    val displayName: String,
    val endpointAssetCodeLocation: EndpointAssetCodeLocation? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssetRelatedCodeLocation
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeLocation",
    "distance",
    "flowIndex"
)
constructor(val spanCodeLocation: AssetCodeLocation, val distance: Int, val flowIndex: Int)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointAssetCodeLocation
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "endpointCodeObjectId"
)
constructor(val endpointCodeObjectId: String)