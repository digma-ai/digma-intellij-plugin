package org.digma.intellij.plugin.model.discovery

import java.util.Objects

data class MethodUnderCaret(
    override val id: String,
    val name: String,
    val className: String,
    val namespace: String,
    override val fileUri: String,
    override val caretOffset: Int,
    //endpointTextRange is optimization. see CurrentContextUpdater.updateCurrentContext
    // when the caret moves in the scope of the same method CurrentContextUpdater will not update the context.
    // but for some frameworks we do want to update the context when moving between lambdas while still in the same
    // method. for example ktor endpoints.
    val endpointTextRange: TextRange? = null,
    override val isSupportedFile: Boolean = true,
) : ElementUnderCaret {


    constructor(
        id: String,
        name: String,
        className: String,
        namespace: String,
        fileUri: String,
        caretOffset: Int,
    ) : this(id, name, className, namespace, fileUri, caretOffset, null, true)


    companion object {
        @JvmStatic
        val EMPTY = MethodUnderCaret("", "", "", "", "", 0, null, false)
    }

    override val type: ElementUnderCaretType = ElementUnderCaretType.Method


    //we don't want caretOffset to be included in equals. its needed for some frameworks, for example ktor support
    // But it should not impact equals , see CurrentContextUpdater.updateCurrentContext, maybe the caret offset changed
    // but we don't want to update the context.
    override fun equals(other: Any?): Boolean {
        if (other !is MethodUnderCaret) return false

        return id == other.id &&
                name == other.name &&
                className == other.className &&
                namespace == other.namespace &&
                fileUri == other.fileUri &&
                endpointTextRange == other.endpointTextRange &&
                isSupportedFile == other.isSupportedFile

    }

    override fun hashCode(): Int {
        return Objects.hash(id, name, className, namespace, fileUri, endpointTextRange, isSupportedFile)
    }


    override fun toString(): String {
        return "$id, " +
                "$name, " +
                "$className, " +
                "$namespace, " +
                "$fileUri, " +
                "$caretOffset, " +
                "$endpointTextRange, " +
                "$isSupportedFile"
    }
}
