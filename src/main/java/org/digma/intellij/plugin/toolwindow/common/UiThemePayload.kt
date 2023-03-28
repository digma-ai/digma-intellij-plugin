package org.digma.intellij.plugin.toolwindow.common

import com.fasterxml.jackson.annotation.JsonCreator


data class UiThemePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val theme: String)