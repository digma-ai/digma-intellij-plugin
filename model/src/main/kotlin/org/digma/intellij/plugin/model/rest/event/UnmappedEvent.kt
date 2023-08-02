package org.digma.intellij.plugin.model.rest.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties
import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnmappedEvent
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
        "codeObjectId",
        "eventTime",
        "eventRecognitionTime"
)
constructor(
        override val codeObjectId: String,
        override val eventTime: Timestamp,
        override val eventRecognitionTime: Timestamp,
        @JsonProperty("type")
        val theType: String,
) : CodeObjectEvent {

    override val type: CodeObjectEventType = CodeObjectEventType.Unmapped
}
