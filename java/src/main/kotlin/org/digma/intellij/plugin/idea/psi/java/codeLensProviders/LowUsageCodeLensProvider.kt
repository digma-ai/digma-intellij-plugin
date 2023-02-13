package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class LowUsageCodeLensProvider : JavaCodeVisionProvider() {

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