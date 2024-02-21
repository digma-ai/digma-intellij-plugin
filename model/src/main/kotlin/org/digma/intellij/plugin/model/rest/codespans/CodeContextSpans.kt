package org.digma.intellij.plugin.model.rest.codespans

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeContextSpans
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "assets",
    "errorData"
)
constructor(val assets: List<Asset>, val errorData: ErrorData? = null)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Asset
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId",
    "displayName"
)
constructor(val spanCodeObjectId: String, val displayName: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorData
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "count"
)
constructor(val codeObjectId: String, val count: Int)