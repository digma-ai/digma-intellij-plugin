package org.digma.intellij.plugin.model.rest.usage

import com.fasterxml.jackson.annotation.JsonCreator

data class UsageStatusRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    val codeObjectIds: List<String>,
    val filterByInsightProviders: List<String>,
) {

    constructor(
        codeObjectIds: List<String>,
    ) : this(codeObjectIds, emptyList())

}