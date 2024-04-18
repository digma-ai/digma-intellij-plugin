package org.digma.intellij.plugin.model.rest.environment

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties

enum class EnvType { Private, Public }

data class Env
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("id", "name", "type")
constructor(
    val id: String,
    val name: String,
    val type: EnvType,
)