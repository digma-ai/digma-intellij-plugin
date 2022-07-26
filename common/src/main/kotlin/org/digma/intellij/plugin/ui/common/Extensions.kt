package org.digma.intellij.plugin.ui.common

fun String.trimLastChar(): String {
    if (this.isEmpty()) {
        return this
    }
    return this.dropLast(1)
}
