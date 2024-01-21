package org.digma.intellij.plugin.model.rest.testing

import com.fasterxml.jackson.annotation.JsonCreator

data class LatestTestsOfSpanRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    val spanCodeObjectIds: Set<String>,
    val methodCodeObjectId: String?,
    val endpointCodeObjectId: String?,
    val environments: Set<String>, // can be empty, then will not filter by it
    val pageNumber: Int,
    val pageSize: Int,
)
