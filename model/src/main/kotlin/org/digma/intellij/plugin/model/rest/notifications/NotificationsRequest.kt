package org.digma.intellij.plugin.model.rest.notifications

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class NotificationsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
    val environment: String,
    val userId: String,
    val startDate: String,
    val pageNumber: Int,
    val pageSize: Int,
    @get:JsonProperty("isRead")
    @param:JsonProperty("isRead")
    val isRead: Boolean,
)
