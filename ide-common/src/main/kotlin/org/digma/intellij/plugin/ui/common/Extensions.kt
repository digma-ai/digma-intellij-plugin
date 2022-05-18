package org.digma.intellij.plugin.ui.common

sealed class Extensions {

    companion object {

        fun String.trimLastChar(): String {
            if (this.length < 1) {
                return this
            }
            return this.substring(0, this.length - 1)
        }

    }
}