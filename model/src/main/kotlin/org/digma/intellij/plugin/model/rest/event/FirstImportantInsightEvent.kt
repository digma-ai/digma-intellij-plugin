package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import java.beans.ConstructorProperties
import java.time.ZonedDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class FirstImportantInsightEvent @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "environment",
    "eventTime",
    "eventRecognitionTime",
    "insight",
)
constructor(
    override val codeObjectId: String?,
    override val environment: String,
    override val eventTime: ZonedDateTime,
    override val eventRecognitionTime: ZonedDateTime,
    val insight: CodeObjectInsight,
) : CodeObjectEvent {

    override val type: CodeObjectEventType = CodeObjectEventType.FirstImportantInsight
}
