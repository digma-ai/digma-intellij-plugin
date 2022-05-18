package org.digma.intellij.plugin.ui.common

fun String.trimLastChar(): String {
    if (this.length < 1) {
        return this
    }
    return this.substring(0, this.length - 1)
}
