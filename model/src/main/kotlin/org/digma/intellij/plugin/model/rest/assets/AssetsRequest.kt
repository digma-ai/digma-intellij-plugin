package org.digma.intellij.plugin.model.rest.assets

import com.fasterxml.jackson.annotation.JsonCreator

data class AssetsRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
constructor(val environment: String)
