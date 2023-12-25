package org.digma.intellij.plugin.model.nav

data class MiniScope(
    val type: ScopeType,
    val scopeObject: Any,
)

enum class ScopeType {
    Method,
    Span,
    Endpoint
}

