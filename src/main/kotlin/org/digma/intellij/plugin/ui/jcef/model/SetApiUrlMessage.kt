package org.digma.intellij.plugin.ui.jcef.model

data class SetApiUrlMessage(
    val type: String,
    val action: String,
    val payload: ApiUrlPayload,
)

data class ApiUrlPayload(val url: String)