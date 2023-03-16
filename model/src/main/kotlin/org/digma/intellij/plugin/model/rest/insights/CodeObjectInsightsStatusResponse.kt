package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class CodeObjectInsightsStatusResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("environment", "codeObjectsWithInsightsStatus")
constructor(val environment: String, val codeObjectsWithInsightsStatus: List<MethodWithInsightStatus>)
