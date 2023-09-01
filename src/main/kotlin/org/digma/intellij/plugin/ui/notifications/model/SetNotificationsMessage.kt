package org.digma.intellij.plugin.ui.notifications.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode


class SetNotificationsMessage
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val type: String, val action: String, val payload: JsonNode)
