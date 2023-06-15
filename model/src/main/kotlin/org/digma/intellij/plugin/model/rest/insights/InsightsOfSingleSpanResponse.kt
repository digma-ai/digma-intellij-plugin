package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class InsightsOfSingleSpanResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("spanCodeObjectId", "spanInfo", "insights")
constructor(val spanCodeObjectId: String, val spanInfo: SpanInfo?, val insights: List<CodeObjectInsight>)