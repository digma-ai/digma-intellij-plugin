package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class HighUsageCodeLensProvider : DigmaCodeVisionProviderBase() {

    companion object {
        const val ID = "DigmaHighUsage"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma High Usage"

    override val groupId: String
        get() = ID


}