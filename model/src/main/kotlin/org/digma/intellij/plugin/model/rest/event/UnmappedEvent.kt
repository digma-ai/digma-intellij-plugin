package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties
import java.time.ZonedDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnmappedEvent
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "codeObjectId",
        "environment",
        "eventTime",
        "eventRecognitionTime"
)
constructor(
        override val codeObjectId: String?,
        override val environment: String,
        override val eventTime: ZonedDateTime,
        override val eventRecognitionTime: ZonedDateTime,
        @JsonProperty("type")
        val theType: String,
) : CodeObjectEvent {

    override val type: CodeObjectEventType = CodeObjectEventType.Unmapped
}
