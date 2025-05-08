package org.digma.intellij.plugin.model.rest.environment

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
import java.util.Date

enum class EnvType { Private, Public }

@JsonIgnoreProperties(ignoreUnknown = true)
data class Env
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("id", "name", "type", "lastActive")
constructor(
    val id: String,
    val name: String,
    val type: EnvType,
    val lastActive: Date?
)