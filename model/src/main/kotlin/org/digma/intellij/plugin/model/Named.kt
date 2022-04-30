package org.digma.intellij.plugin.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Named(@JsonProperty("name") val name: String)
