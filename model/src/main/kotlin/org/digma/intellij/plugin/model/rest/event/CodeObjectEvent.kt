package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.sql.Timestamp


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
)
interface CodeObjectEvent{
    val type: CodeObjectEventType
    val codeObjectId: String
    val eventTime: Timestamp
    val eventRecognitionTime: Timestamp
}