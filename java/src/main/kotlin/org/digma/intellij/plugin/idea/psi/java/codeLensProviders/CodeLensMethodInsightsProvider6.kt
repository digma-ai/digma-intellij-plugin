package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class CodeLensMethodInsightsProvider6: JavaCodeVisionProvider() {
    companion object {
        const val ID = "DigmaGenericProvider6"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Generic Provider 6"

    override val groupId: String
        get() = ID

}