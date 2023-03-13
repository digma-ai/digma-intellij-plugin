package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider5: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider5"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 5"

    override val groupId: String
        get() = ID

}