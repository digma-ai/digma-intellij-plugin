package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator

data class SpanHistogramQuery
@JsonCreator
constructor(
    val environment: String,
    val spanName: String,
    val instrumentationLibrary: String,
    val codeObjectId: String,
    val theme: String?,
    val backgroundColor: String?,
)
