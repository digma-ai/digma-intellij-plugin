package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class CodeLensMethodInsightsProvider7: JavaCodeVisionProvider() {
    companion object {
        const val ID = "DigmaGenericProvider7"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 7"

    override val groupId: String
        get() = ID

}