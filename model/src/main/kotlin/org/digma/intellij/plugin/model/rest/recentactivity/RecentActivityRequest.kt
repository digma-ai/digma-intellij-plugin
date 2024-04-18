package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator

data class RecentActivityRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
        val environments: List<String>
)