package org.digma.intellij.plugin.ui.model.insights

enum class InsightGroupType(val sortIndex: Int) {
    HttpEndpoint(10), // grouped by route name
    Span(20),         // grouped by span name
}