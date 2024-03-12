package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.model.rest.common.SpanDurationsPercentile
import java.beans.ConstructorProperties
import java.time.ZonedDateTime


@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeObjectDurationChangeEvent
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "environment",
    "eventTime",
    "eventRecognitionTime",
    "spanCodeObjectId",
    "span",
    "changedDurationPercentile",
)
constructor(
    override val codeObjectId: String?,
    override val environment: String,
    override val eventTime: ZonedDateTime,
    override val eventRecognitionTime: ZonedDateTime,
    val spanCodeObjectId: String,
    val span: SpanInfo,
    val changedDurationPercentile: SpanDurationsPercentile,
    ) : CodeObjectEvent {

    override val type: CodeObjectEventType = CodeObjectEventType.DurationChangeEvent
}

