package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider9: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider9"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 9"

//    override val groupId: String
//        get() = ID

}