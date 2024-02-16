package org.digma.intellij.plugin.scope

import org.digma.intellij.plugin.model.rest.assets.Role

data class SpanScope(
    val spanCodeObjectId: String,
    var displayName: String? = null,
    val serviceName: String? = null,
    var role: Role?,
    var methodId: String? = null,
) :
    AbstractScope() {

    constructor(spanCodeObjectId: String) : this(spanCodeObjectId, null, null, null)
}
