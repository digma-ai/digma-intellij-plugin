package org.digma.intellij.plugin.model.rest.notifications

import com.fasterxml.jackson.annotation.JsonCreator

data class GetUnreadNotificationsCountRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environment: String, val userId: String, val startDate: String)
