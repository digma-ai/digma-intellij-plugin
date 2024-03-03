package org.digma.intellij.plugin.model.lens

import java.util.Objects


data class CodeLens(
    val id: String,
    val codeMethod: String, //method this code lens should appear on
    val scopeCodeObjectId: String,
    val lensTitle: String,
    val importance: Int,
) {
    var lensDescription: String = ""
    var lensMoreText: String = ""

    // generated code. hashCode and equals on top of codeObjectId and lensTitle, in order to avoid duplicates
    override fun hashCode(): Int {
        return Objects.hash(codeMethod, id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeLens) return false

        if (codeMethod != other.codeMethod) return false
        return id == other.id
    }

}