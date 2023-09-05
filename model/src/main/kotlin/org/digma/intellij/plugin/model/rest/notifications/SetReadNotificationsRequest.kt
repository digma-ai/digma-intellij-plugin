package org.digma.intellij.plugin.model.rest.notifications

import com.fasterxml.jackson.annotation.JsonCreator

data class SetReadNotificationsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environment: String, val userId: String, val upToDateTime: String)
