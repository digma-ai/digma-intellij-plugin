package org.digma.intellij.plugin.model.lens


data class CodeLens(
    val codeMethod: String, //method this code lens should appear on
    val scopeCodeObjectId: String,
    val lensTitle: String,
    val importance: Int,
) {
    var lensDescription: String = ""
    var lensMoreText: String = ""
    var anchor: String = ""

    // generated code. hashCode and equals on top of codeObjectId and lensTitle, in order to avoid duplicates
    override fun hashCode(): Int {
        var result = codeMethod.hashCode()
        result = 31 * result + lensTitle.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeLens) return false

        if (codeMethod != other.codeMethod) return false
        return lensTitle == other.lensTitle
    }

}