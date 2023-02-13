package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class ScaleFactorCodeLensProvider : JavaCodeVisionProvider() {

    companion object {
        const val ID = "DigmaScaleFactor"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Scale Factor"

    override val groupId: String
        get() = ID

}