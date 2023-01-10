package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator

data class RecentActivityRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(
//        val accountId: String?, // we don't use this field for now
//        val maxEntriesPerEnvironment: Int?, // we don't use this field for now
        val environments: List<String>
)