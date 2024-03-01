package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider6: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider6"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 6"

//    override val groupId: String
//        get() = ID

}