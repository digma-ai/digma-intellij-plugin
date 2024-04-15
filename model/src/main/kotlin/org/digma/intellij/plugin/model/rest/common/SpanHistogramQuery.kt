package org.digma.intellij.plugin.model.rest.common

import com.fasterxml.jackson.annotation.JsonCreator

data class SpanHistogramQuery
@JsonCreator
constructor(
    val environment: String,
    val spanCodeObjectId: String,
    val theme: String?,
    val backgroundColor: String?,
)
