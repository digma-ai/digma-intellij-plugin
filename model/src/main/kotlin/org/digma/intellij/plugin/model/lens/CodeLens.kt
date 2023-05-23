package org.digma.intellij.plugin.model.lens


data class CodeLens(
    val codeObjectId: String,
    val lensTitle: String,
    val importance: Int,
) {
    var lensDescription: String = ""
    var lensMoreText: String = ""
    var anchor: String = ""

    // generated code. hashCode and equals on top of codeObjectId and lensTitle, in order to avoid duplicates
    override fun hashCode(): Int {
        var result = codeObjectId.hashCode()
        result = 31 * result + lensTitle.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeLens) return false

        if (codeObjectId != other.codeObjectId) return false
        return lensTitle == other.lensTitle
    }

}