package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider4: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider4"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 4"

//    override val groupId: String
//        get() = ID

}