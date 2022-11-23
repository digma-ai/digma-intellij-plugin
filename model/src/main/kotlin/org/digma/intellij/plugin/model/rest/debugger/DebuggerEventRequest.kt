package org.digma.intellij.plugin.model.rest.debugger

import com.fasterxml.jackson.annotation.JsonCreator

data class DebuggerEventRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val event: String, val machineName: String,val timestamp: String)
