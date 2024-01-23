package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndpointChattyApiInsight
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
    "shortDisplayInfo",
    "spanInfo",
    "route",
    "serviceName",
    "spans",
    "severity",
    "impact",
    "criticality",
    "firstCommitId",
    "lastCommitId",
    "deactivatedCommitId",
    "reopenCount",
    "ticketLink",
    "firstDetected"
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
    //override val isRecalculateEnabled: Boolean,
    override val shortDisplayInfo: ShortDisplayInfo?,
    override val spanInfo: SpanInfo,
    override var route: String,
    override var serviceName: String,
    val spans: List<ChattyApiSpanInfo>,
    override val severity: Double,
    override val impact: Double,
    override val criticality: Double,
    override val firstCommitId: String?,
    override val lastCommitId: String?,
    override val deactivatedCommitId: String?,
    override val reopenCount: Int,
    override val ticketLink: String?,
    override val firstDetected: Date?,
) : EndpointInsight {

    override val type: InsightType = InsightType.EndpointChattyApi
    override val isRecalculateEnabled: Boolean = true // should remove the setter = true later ...support backward compatibility

}