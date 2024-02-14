package org.digma.intellij.plugin.scope

data class SpanScope(val spanCodeObjectId: String, val displayName: String, val serviceName: String? = null) : AbstractScope() {

}
