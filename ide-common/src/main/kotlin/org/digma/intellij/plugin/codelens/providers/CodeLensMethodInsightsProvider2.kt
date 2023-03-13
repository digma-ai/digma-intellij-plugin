package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider2: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider2"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 2"

    override val groupId: String
        get() = ID

}