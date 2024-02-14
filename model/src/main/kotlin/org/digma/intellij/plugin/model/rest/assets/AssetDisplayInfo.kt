package org.digma.intellij.plugin.model.rest.assets

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssetDisplayInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "displayName",
    "methodCodeObjectId"
)
constructor(val displayName: String, val methodCodeObjectId: String?)