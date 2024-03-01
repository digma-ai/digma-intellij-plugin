package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider3: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider3"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 3"

//    override val groupId: String
//        get() = ID

}