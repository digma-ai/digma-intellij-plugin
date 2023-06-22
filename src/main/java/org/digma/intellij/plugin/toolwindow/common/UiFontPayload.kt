package org.digma.intellij.plugin.toolwindow.common

import com.fasterxml.jackson.annotation.JsonCreator


data class UiFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val mainFont: String)