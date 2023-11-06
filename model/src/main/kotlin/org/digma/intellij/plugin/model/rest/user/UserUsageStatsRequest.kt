package org.digma.intellij.plugin.model.rest.user

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUsageStatsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("accountId")
constructor(
    val accountId: String,
) {

}
