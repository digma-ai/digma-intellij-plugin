package org.digma.intellij.plugin.scope

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.model.rest.assets.Role
import java.beans.ConstructorProperties

data class SpanScope
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId",
    "displayName",
    "serviceName",
    "role",
    "methodId",
)
constructor (

    val spanCodeObjectId: String,
    var displayName: String? = null,
    val serviceName: String? = null,
    var role: Role?,
    var methodId: String? = null,
) :
    AbstractScope() {

    constructor(spanCodeObjectId: String) : this(spanCodeObjectId, null, null, null)
}
