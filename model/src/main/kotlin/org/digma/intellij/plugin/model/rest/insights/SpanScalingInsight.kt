package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.*

data class SpanScalingInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "codeObjectId",
        "environment",
        "scope",
        "importance",
        "decorators",
        "actualStartTime",
        "customStartTime",
        "prefixedCodeObjectId",
        "spanName",
        "turningPointConcurrency",
        "maxConcurrency",
        "minDuration",
        "maxDuration",
)
constructor(
        override val codeObjectId: String,
        override val environment: String,
        override val scope: String,
        override val importance: Int,
        override val decorators: List<CodeObjectDecorator>?,
        override val actualStartTime: Date?,
        override val customStartTime: Date?,
        override val prefixedCodeObjectId: String?,
        val spanName: String,
        val turningPointConcurrency: Int,
        val maxConcurrency: Int,
        val minDuration: Duration,
        val maxDuration: Duration,
) : CodeObjectInsight {

    override val type: InsightType = InsightType.SpanScaling
}