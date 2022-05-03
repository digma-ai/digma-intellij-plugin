package org.digma.intellij.plugin.model

data class CodeLens(val codeObjectId: String,
                    val codeObjectType: CodeObjectType,
                    val lensText: String,
                    var lensTooltipText: String,
                    var lensMoreText: String,
                    var anchor: String) {

    constructor(codeObjectId: String,codeObjectType: CodeObjectType, lensText: String) : this(codeObjectId,codeObjectType,lensText,"","","")
}