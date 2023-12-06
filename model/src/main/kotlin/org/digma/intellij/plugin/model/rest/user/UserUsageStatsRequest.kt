package org.digma.intellij.plugin.model.rest.user

import com.fasterxml.jackson.annotation.JsonCreator


data class UserUsageStatsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
//  val accountId: String?,
    val dummy: String?,
) {
    constructor() : this(null)
}
