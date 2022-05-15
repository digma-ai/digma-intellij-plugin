package org.digma.intellij.plugin.model.discovery

import org.digma.intellij.plugin.model.ElementUnderCaretType

interface ElementUnderCaret {
    val id: String
    val fileUri: String
    val type: ElementUnderCaretType

    fun idWithType():String
}