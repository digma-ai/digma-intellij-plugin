package org.digma.intellij.plugin.model.discovery

import org.digma.intellij.plugin.model.ElementUnderCaretType

data class MethodUnderCaret(
    override val id: String,
    val name:String,
    val className: String,
    override val fileUri: String
): ElementUnderCaret {

    override val type: ElementUnderCaretType = ElementUnderCaretType.Method

    override fun toString(): String {
        val toString = "${id}, " +
                       "${name}, " +
                       "${className}, " +
                       "${fileUri}"
        return toString;
    }
}
