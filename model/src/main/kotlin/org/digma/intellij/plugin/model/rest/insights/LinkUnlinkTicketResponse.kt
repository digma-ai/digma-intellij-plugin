package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkUnlinkTicketResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("environment", "success", "message", "codeObjectId", "insightType", "ticketLink")
constructor(val environment: String, val success : Boolean, val message : String?, val codeObjectId: String, val insightType: String, var ticketLink: String?)

