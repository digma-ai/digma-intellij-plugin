package org.digma.intellij.plugin.model.nav

data class MiniScope(
    val type: ScopeType,
    val id: String,
)

enum class ScopeType {
    Method,
    Span,
}

