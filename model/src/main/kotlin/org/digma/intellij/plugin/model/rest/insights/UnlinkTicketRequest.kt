package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnlinkTicketRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("environment", "insightId")
constructor(val environment: String, val insightId: String)
