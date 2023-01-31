package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class InsightsOfMethodsResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("environment", "methodsWithInsights")
constructor(val environment: String, val methodsWithInsights: List<MethodWithInsights>)