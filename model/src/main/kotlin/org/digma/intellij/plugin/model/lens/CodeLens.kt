package org.digma.intellij.plugin.model.lens


data class CodeLens(
    val codeObjectId: String,
    val lensTitle: String,
    val importance: Int,
) {
    var lensDescription: String = ""
    var lensMoreText: String = ""
    var anchor: String = ""

    // generated code. hashCode and equals on top of lensTitle, in order to avoid duplicates
    override fun hashCode(): Int {
        return lensTitle.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeLens) return false

        return lensTitle == other.lensTitle
    }


}