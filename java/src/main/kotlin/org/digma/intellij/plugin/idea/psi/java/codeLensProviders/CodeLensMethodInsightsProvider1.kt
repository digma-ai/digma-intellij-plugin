package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class CodeLensMethodInsightsProvider1: JavaCodeVisionProvider() {
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