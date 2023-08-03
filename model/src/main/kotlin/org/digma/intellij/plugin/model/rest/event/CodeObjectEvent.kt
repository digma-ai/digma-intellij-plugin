package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.ZonedDateTime


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true,
    defaultImpl = UnmappedEvent::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CodeObjectDurationChangeEvent::class, name = "DurationChangeEvent"),
    JsonSubTypes.Type(value = FirstImportantInsightEvent::class, name = "FirstImportantInsight"),
)
interface CodeObjectEvent {
    val type: CodeObjectEventType
    val environment: String
    val codeObjectId: String?
    val eventTime: ZonedDateTime
    val eventRecognitionTime: ZonedDateTime
}