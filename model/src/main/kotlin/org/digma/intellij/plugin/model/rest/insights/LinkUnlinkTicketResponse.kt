package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkUnlinkTicketResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("success", "message", "ticketLink")
constructor(val success : Boolean, val message : String?, var ticketLink: String?)

