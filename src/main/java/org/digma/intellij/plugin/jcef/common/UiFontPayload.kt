package org.digma.intellij.plugin.jcef.common

import com.fasterxml.jackson.annotation.JsonCreator


data class UiFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val mainFont: String)