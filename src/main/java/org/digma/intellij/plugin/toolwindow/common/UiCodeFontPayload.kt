package org.digma.intellij.plugin.toolwindow.common

import com.fasterxml.jackson.annotation.JsonCreator


data class UiCodeFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val codeFont: String)