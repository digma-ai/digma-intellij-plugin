package org.digma.intellij.plugin.toolwindow

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry


data class JcefMessagePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environments: MutableList<String>, val entries: MutableList<RecentActivityResponseEntry>)