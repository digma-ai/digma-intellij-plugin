package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenInDefaultBrowserRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: OpenInDefaultBrowserRequestPayload,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenInDefaultBrowserRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("url")
constructor(
    val url: String,
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenInInternalBrowserRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("action", "payload")
constructor(
    val action: String,
    val payload: OpenInInternalBrowserRequestPayload,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenInInternalBrowserRequestPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("url", "title")
constructor(
    val url: String,
    val title: String,
)