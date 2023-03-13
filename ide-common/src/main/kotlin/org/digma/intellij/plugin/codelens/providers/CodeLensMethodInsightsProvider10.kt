package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider10: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider10"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 10"

    override val groupId: String
        get() = ID

}