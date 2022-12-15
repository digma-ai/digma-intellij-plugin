package org.digma.intellij.plugin.model.lens


data class CodeLens(
        val codeObjectId: String,
        val lensTitle: String,
        var lensDescription: String,
        var lensMoreText: String,
        val importance: Int,
        var anchor: String
) {

    constructor(codeObjectId: String,
                lensTitle: String,
                importance: Int
    ) : this(
            codeObjectId,
            lensTitle,
            "",
            "",
            importance,
            ""
    )

}