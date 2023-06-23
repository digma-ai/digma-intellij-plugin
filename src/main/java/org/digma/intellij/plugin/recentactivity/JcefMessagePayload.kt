package org.digma.intellij.plugin.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry


data class JcefMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environments: List<String>, val entries: List<RecentActivityResponseEntry>)

data class JcefConnectionCheckMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val result: String)


enum class ConnectionCheckResult(val value: String) {
    SUCCESS("success"),
    FAILURE("failure")
}