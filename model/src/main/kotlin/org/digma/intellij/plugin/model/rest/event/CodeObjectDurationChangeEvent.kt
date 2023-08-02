package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import java.beans.ConstructorProperties
import java.sql.Timestamp


@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeObjectDurationChangeEvent
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "eventTime",
    "eventRecognitionTime",
    "spanCodeObjectId",
    "span",
    "changedDurationPercentile",
)
constructor(
    override val codeObjectId: String,
    override val eventTime: Timestamp,
    override val eventRecognitionTime: Timestamp,
    val spanCodeObjectId: String,
    val span: SpanInfo,
    val changedDurationPercentile: SpanDurationsPercentile,
    ) : CodeObjectEvent {

    override val type: CodeObjectEventType = CodeObjectEventType.DurationChangeEvent
}

