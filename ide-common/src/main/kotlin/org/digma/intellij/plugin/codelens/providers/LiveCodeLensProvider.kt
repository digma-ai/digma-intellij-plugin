package org.digma.intellij.plugin.codelens.providers

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class LiveCodeLensProvider : DigmaCodeVisionProviderBase() {

    companion object {
        const val ID = "DigmaLive"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Live Method Hints"

//    override val groupId: String
//        get() = ID

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)

}