package org.digma.intellij.plugin.ui.common

fun String.trimLastChar(): String {
    if (this.isEmpty()) {
        return this
    }
    return this.substring(0, this.length - 1)
}
