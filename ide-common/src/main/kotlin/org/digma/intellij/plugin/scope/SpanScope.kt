package org.digma.intellij.plugin.scope

data class SpanScope(val spanCodeObjectId: String, var displayName: String? = null, val serviceName: String? = null) : AbstractScope() {

    constructor(spanCodeObjectId: String) : this(spanCodeObjectId, null, null)
}
