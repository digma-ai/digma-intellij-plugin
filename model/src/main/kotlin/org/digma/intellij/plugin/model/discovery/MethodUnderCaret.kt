package org.digma.intellij.plugin.model.discovery

import org.digma.intellij.plugin.model.ElementUnderCaretType

data class MethodUnderCaret(
    override val id: String,
    val name:String,
    val className: String,
    val namespace: String,
    override val fileUri: String,
    override val isSupportedFile: Boolean = true,
): ElementUnderCaret {

    constructor(
        id: String,
        name: String,
        className: String,
        namespace: String,
        fileUri: String
    ) : this(id, name, className, namespace, fileUri, true) {

    }


    companion object {
        @JvmStatic
        val EMPTY = MethodUnderCaret("", "", "", "","", false)
    }

    override val type: ElementUnderCaretType = ElementUnderCaretType.Method

    override fun toString(): String {
        return "$id, " +
                "$name, " +
                "$className, " +
                "$namespace, " +
                "$fileUri, " +
                "$isSupportedFile";
    }
}
