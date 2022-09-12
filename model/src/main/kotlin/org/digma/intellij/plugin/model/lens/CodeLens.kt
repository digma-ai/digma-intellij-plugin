package org.digma.intellij.plugin.model.lens

import org.digma.intellij.plugin.model.CodeObjectType

data class CodeLens(
    val codeObjectId: String,
    val codeObjectType: CodeObjectType,
    val type: CodeLensType,
    val lensText: String,
    var lensTooltipText: String,
    var lensMoreText: String,
    var anchor: String
) {

    constructor(codeObjectId: String, codeObjectType: CodeObjectType, type: CodeLensType, lensText: String) : this(
        codeObjectId,
        codeObjectType,
        type,
        lensText,
        "",
        "",
        ""
    )


    enum class CodeLensType {
        ErrorHotspot,
        LowUsage,
        HighUsage
    }
}