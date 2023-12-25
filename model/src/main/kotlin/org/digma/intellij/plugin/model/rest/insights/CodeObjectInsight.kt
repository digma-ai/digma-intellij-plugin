package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.digma.intellij.plugin.model.InsightType
import java.util.Date


/**
 * Mapping for insights types.
 * this mapping supports unmapped types with UnmappedInsight class. JsonIgnoreProperties is necessary only for
 * unmapped types and makes the mapping less robust. the property 'type' is visible only to be mapped to UnmappedInsight
 * so that it can be showed in the GUI but is not mapped in any other subtype, instead there is an enum type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true,
    defaultImpl = UnmappedInsight::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = HotspotInsight::class, name = "HotSpot"),
    JsonSubTypes.Type(value = ErrorInsight::class, name = "Errors"),
    JsonSubTypes.Type(value = SpanUsagesInsight::class, name = "SpanUsages"),
    JsonSubTypes.Type(value = SlowestSpansInsight::class, name = "SlowestSpans"),
    JsonSubTypes.Type(value = LowUsageInsight::class, name = "LowUsage"),
    JsonSubTypes.Type(value = NormalUsageInsight::class, name = "NormalUsage"),
    JsonSubTypes.Type(value = HighUsageInsight::class, name = "HighUsage"),
    JsonSubTypes.Type(value = EPNPlusSpansInsight::class, name = "EndpointSpaNPlusOne"),
    JsonSubTypes.Type(value = EndpointSessionInViewInsight::class, name = "EndpointSessionInView"),
    JsonSubTypes.Type(value = EndpointChattyApiInsight::class, name = "EndpointChattyApi"),

    JsonSubTypes.Type(value = EndpointDurationSlowdownInsight::class, name = "EndpointDurationSlowdown"),
    JsonSubTypes.Type(value = EndpointBreakdownInsight::class, name = "EndpointBreakdown"),
    JsonSubTypes.Type(value = EndpointHighNumberOfQueriesInsight::class, name = "EndpointHighNumberOfQueries"),
    JsonSubTypes.Type(value = SpanNPlusOneInsight::class, name = "SpaNPlusOne"),
    JsonSubTypes.Type(value = SlowEndpointInsight::class, name = "SlowEndpoint"),
    JsonSubTypes.Type(value = SpanScalingInsight::class, name = "SpanScaling"),
    JsonSubTypes.Type(value = SpanDurationsInsight::class, name = "SpanDurations"),
    JsonSubTypes.Type(value = SpanSlowEndpointsInsight::class, name = "SpanEndpointBottleneck"),
    JsonSubTypes.Type(value = SpanDurationBreakdownInsight::class, name = "SpanDurationBreakdown"),
)
interface CodeObjectInsight {
    val type: InsightType
    val codeObjectId: String
    val environment: String
    val scope: String
    val importance: Int
    val decorators: List<CodeObjectDecorator>?
    val actualStartTime: Date?
    val customStartTime: Date?
    val prefixedCodeObjectId: String?
    val isRecalculateEnabled: Boolean
    val shortDisplayInfo: ShortDisplayInfo?
    val severity: Double
    val impact: Double
    val criticality: Double
    val firstCommitId: String?
    val lastCommitId: String?
    val deactivatedCommitId: String?

    fun hasDecorators(): Boolean {
        return !decorators.isNullOrEmpty()
    }

    fun isTypeMapped(): Boolean {
        return type != null && type != InsightType.Unmapped
    }
}