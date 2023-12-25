package org.digma.intellij.plugin.model.discovery

data class TextRange(val start: Int, val end: Int) {

    fun contains(offset: Int): Boolean {
        return offset in start..end
    }

}