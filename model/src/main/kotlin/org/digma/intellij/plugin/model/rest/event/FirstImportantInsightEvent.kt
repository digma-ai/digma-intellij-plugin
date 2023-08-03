package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import java.beans.ConstructorProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class FirstImportantInsightEvent @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "codeObjectId",
    "eventTime",
    "eventRecognitionTime",
    "insight",
)
constructor(
    override val codeObjectId: String,
    override val eventTime: Timestamp,
    override val eventRecognitionTime: Timestamp,
    val insight: CodeObjectInsight,
) : CodeObjectEvent {

    override val type: CodeObjectEventType = CodeObjectEventType.FirstImportantInsight
}
