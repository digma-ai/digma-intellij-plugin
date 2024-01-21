package org.digma.intellij.plugin.insights.model.outgoing

import org.digma.intellij.plugin.model.rest.insights.LinkUnlinkTicketResponse

data class SetLinkUnlinkResponseMessage(val type: String = "digma", val action: String = "INSIGHTS/SET_TICKET_LINK", val payload: LinkUnlinkTicketResponse)