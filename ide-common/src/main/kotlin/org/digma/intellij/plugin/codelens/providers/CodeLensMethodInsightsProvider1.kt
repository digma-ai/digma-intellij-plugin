package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class CodeLensMethodInsightsProvider1: DigmaCodeVisionProviderBase() {
    companion object {
        const val ID = "DigmaGenericProvider1"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 1"

    override val groupId: String
        get() = ID
}