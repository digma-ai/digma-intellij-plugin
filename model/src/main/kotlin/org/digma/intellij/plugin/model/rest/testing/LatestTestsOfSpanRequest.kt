package org.digma.intellij.plugin.model.rest.testing

import com.fasterxml.jackson.annotation.JsonCreator

data class LatestTestsOfSpanRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    val spanCodeObjectIds: List<String>,
    val environments: List<String>, // can be empty, then will not filter by it
    val pageNumber: Int,
    val pageSize: Int,
)
