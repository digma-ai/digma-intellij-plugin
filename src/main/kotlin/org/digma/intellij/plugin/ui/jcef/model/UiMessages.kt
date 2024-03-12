package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UICodeFontRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: UiCodeFontPayload,
)

data class UiCodeFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val codeFont: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UIFontRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: UiFontPayload,
)

data class UiFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val mainFont: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UIThemeRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: UiThemePayload,
)

data class UiThemePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val theme: String)