package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class MethodWithInsightStatus
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("methodWithIds", "insightStatus")
constructor(
        val methodWithIds: MethodWithCodeObjects,
        val insightStatus: InsightStatus,
)
