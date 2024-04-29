package org.digma.intellij.plugin.model.rest.highlights

data class HighlightsRequest(
    val scopedSpanCodeObjectId: String,
    val environments: List<String>
)