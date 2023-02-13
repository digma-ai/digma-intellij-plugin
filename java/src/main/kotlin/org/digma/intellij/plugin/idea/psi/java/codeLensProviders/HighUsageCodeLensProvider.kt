package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class HighUsageCodeLensProvider : JavaCodeVisionProvider() {

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