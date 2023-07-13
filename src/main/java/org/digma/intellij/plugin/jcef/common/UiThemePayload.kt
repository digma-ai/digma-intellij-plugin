package org.digma.intellij.plugin.jcef.common

import com.fasterxml.jackson.annotation.JsonCreator


data class UiThemePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val theme: String)