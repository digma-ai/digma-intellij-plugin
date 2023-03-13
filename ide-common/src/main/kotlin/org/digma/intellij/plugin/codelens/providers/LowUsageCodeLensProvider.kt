package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class LowUsageCodeLensProvider : DigmaCodeVisionProviderBase() {

    companion object {
        const val ID = "DigmaLowUsage"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma low usage"

    override val groupId: String
        get() = ID

}