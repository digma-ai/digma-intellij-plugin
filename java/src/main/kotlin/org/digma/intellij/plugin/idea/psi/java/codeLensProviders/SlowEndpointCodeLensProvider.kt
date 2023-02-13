package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class SlowEndpointCodeLensProvider : JavaCodeVisionProvider() {

    companion object {
        const val ID = "DigmaSlowEndpoint"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Slow Endpoint"

    override val groupId: String
        get() = ID

}