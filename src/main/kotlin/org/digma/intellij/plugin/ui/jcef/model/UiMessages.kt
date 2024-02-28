package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

data class UICodeFontRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: UiCodeFontPayload,
)

data class UiCodeFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val codeFont: String)

data class UIFontRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: UiFontPayload,
)

data class UiFontPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val mainFont: String)

data class UIThemeRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "action", "payload")
constructor(
        val type: String?,
        val action: String,
        val payload: UiThemePayload,
)

data class UiThemePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val theme: String)