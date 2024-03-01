package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider7: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider7"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 7"

//    override val groupId: String
//        get() = ID

}