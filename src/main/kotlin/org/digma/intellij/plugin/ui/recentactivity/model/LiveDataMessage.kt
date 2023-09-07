package org.digma.intellij.plugin.ui.recentactivity.model

import org.digma.intellij.plugin.model.rest.livedata.DurationData
import org.digma.intellij.plugin.model.rest.livedata.LiveDataRecord

data class LiveDataMessage(val type: String, val action: String, val payload: LiveDataPayload)

data class LiveDataPayload(val liveDataRecords: List<LiveDataRecord>, val durationData: DurationData)