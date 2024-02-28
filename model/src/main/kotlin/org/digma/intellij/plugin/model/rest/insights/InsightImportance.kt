package org.digma.intellij.plugin.model.rest.insights

enum class InsightImportance(val priority: Int) {
    HCF(0),
    ShowStopper(1),
    Critical(2),
    HighlyImportant(3),
    Important(4),
    Interesting(5),
    Info(6),
    NotInteresting(7),
    Clutter(8),
    Spam(9)
}