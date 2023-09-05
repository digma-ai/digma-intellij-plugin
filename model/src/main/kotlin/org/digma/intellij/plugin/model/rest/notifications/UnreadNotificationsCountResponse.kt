package org.digma.intellij.plugin.model.rest.notifications

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnreadNotificationsCountResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "unreadCount"
)
constructor(val unreadCount: Long)
