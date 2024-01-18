package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlowSpanInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("spanInfo", "probabilityOfBeingBottleneck", "avgDurationWhenBeingBottleneck","criticality","ticketLink")
constructor(val spanInfo: SpanInfo,
            val probabilityOfBeingBottleneck: Double,
            val avgDurationWhenBeingBottleneck: Duration,
            val criticality: Double,
            val ticketLink: String?)
