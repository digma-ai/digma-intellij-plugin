package org.digma.intellij.plugin.model.rest.livedata

import com.fasterxml.jackson.annotation.JsonCreator

data class DurationLiveDataRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environment: String, val codeObjectId: String)
