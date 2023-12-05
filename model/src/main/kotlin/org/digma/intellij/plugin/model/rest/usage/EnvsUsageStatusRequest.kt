package org.digma.intellij.plugin.model.rest.usage

import com.fasterxml.jackson.annotation.JsonCreator

data class EnvsUsageStatusRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    val codeObjectIds: List<String> = emptyList(),
    val filterByInsightProviders: List<String> = emptyList(),
) {

    constructor(
        codeObjectIds: List<String>,
    ) : this(codeObjectIds, emptyList())

}