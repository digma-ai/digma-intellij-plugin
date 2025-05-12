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
){

    //ignore lastActive in hashCode and equals because it will cause many events to be fired.
    //we rely on equals to decide if the environments list changed or if the current environment changed

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Env) return false

        return id == other.id && name == other.name && type == other.type
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}