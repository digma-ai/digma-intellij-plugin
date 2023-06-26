package org.digma.intellij.plugin.ui.model

enum class UIInsightsStatus {
    Startup, //used only to show startup message and should never be used, its the initial value
    Default, //default means the UI will be updated according to existence of insights/errors list.
    NoInsights, // no insights
    InsightPending, // backend is aware of the code objects, but still no insights, soon there will be
    NoSpanData, // backend is not aware of code objects
    NoObservability, // method has no insights and has no related code object ids (spans and/or endpoints)
    Loading
}