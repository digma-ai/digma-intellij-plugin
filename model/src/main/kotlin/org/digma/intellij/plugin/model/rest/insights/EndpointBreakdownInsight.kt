package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointBreakdownInsight
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
    "isRecalculateEnabled",
    "shortDisplayInfo",
    "spanInfo",
    "route",
    "serviceName",
    "hasAsyncSpans",
    "components",
    "p50Components",
    "p95Components",
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
    override val isRecalculateEnabled: Boolean,
    override val shortDisplayInfo: ShortDisplayInfo?,
    override val spanInfo: SpanInfo,
    override var route: String,
    override var serviceName: String,

    // hasAsyncSpans. default is false.
    //  when value false should refer to fraction (percents)
    //  when value is true should refer to duration
    val hasAsyncSpans: Boolean,
    val components: List<EndpointBreakdownComponent>, // deprecated. instead, use p50Components
    val p50Components: List<EndpointBreakdownComponent>,
    val p95Components: List<EndpointBreakdownComponent>,
) : EndpointInsight {
    override val type: InsightType = InsightType.EndpointBreakdown
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointBreakdownComponent
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "fraction", "duration")
constructor(
    val type: String,
    val fraction: Double,
    val duration: Duration,
)
